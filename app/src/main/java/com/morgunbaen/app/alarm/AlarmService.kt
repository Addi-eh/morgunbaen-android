package com.morgunbaen.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.morgunbaen.app.MorgunbaenApp
import com.morgunbaen.app.R
import com.morgunbaen.app.data.AlarmSoundStore
import com.morgunbaen.app.data.EpisodeRepository
import com.morgunbaen.app.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Spilar bænina þegar vekjarinn hringir.
 *
 * Keyrir sem forgrunnstjonusta - annars gaeti Android drepid spilunina
 * eftir nokkrar sekundur.
 */
class AlarmService : Service() {

    private var player: ExoPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * Hljodstyrkurinn eins og notandinn hafdi hann adur en vid snertum hann.
     * -1 tydir ad vid hofum ekki breytt neinu og megum tvi ekki skila neinu.
     */
    private var originalAlarmVolume = -1

    /**
     * Hvar i rodinni vid erum. Baenin fyrst, sidan frettir (ef valid),
     * loks varahljod sem spilar tar til slokkt er. WAKE_SOUND er
     * vekjarahljodid sem hringir a undan baeninni i "Vekjarahljod, svo baen".
     */
    private var stage = Stage.PRAYER

    private enum class Stage { WAKE_SOUND, PRAYER, NEWS, FALLBACK }

    /**
     * Notandinn er vaknadur og hlustar. Tha er tetta venjuleg spilun:
     * midlastyrkur, engin timamork, og hun HAETTIR tegar efnid klarast
     * i stad tess ad halda afram i varahljod.
     */
    private var listening = false
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Vekjarinn hringir og notandinn hefur ekki brugdist vid. Adeins ta
     * a ad reyna ad koma skjanum aftur upp - ekki i hlustun eda spurningu.
     */
    private var ringing = false

    /**
     * Eigin Handler fyrir endurtilraunir skjasins, svo taer taemist ekki med
     * fade-in-skrefunum og timamorkunum - og oll stodvun hreinsi taer
     * a einum stad, sja cancelScreenRetries().
     */
    private val screenHandler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAlarm()
            ACTION_DISMISS -> stopAlarm()
            ACTION_SNOOZE -> snooze()
            ACTION_AWAKE -> awake(intent.getBooleanExtra(EXTRA_FROM_SCREEN, false))
            ACTION_LISTEN -> startListening()
            else -> stopAlarm()
        }
        // Aðeins ræsingin á að koma aftur ef Android drepur þjónustuna.
        // Slökkva/blundur mega ekki endurræsa vekjarann.
        return if (intent?.action == ACTION_START) START_REDELIVER_INTENT
        else START_NOT_STICKY
    }

    private fun startAlarm() {
        // Óhætt að kalla tvisvar: drepið eintak, eða tvöfaldur ACTION_START.
        resetPlayback()

        // START_REDELIVER_INTENT getur skilað ACTION_START löngu eftir að
        // vekjarinn átti að hringja. Án þessa færi bænin í gang kl. 08:40
        // ef þjónustan var drepin kl. 07:03.
        val firedAt = prefs.lastAlarmFiredMillis
        if (firedAt > 0L &&
            System.currentTimeMillis() - firedAt > STALE_START_LIMIT_MS
        ) {
            Log.w(TAG, "ACTION_START of seint - hætti")
            startForeground(NOTIFICATION_ID, buildNotification())
            stopAlarm()
            return
        }

        stage = Stage.PRAYER
        ringing = true
        // Skjarinn er ekki kominn upp enn. Stodvud gildi fra i gaer mega
        // ekki segja ad hann se tad - ta vaeri endurtilraununum sleppt.
        screenVisible.value = false

        // Halda ordgjafanum vakandi medan spilad er.
        acquireWakeLock()

        // Forgrunnstilkynning MED fullum skja - tetta laetur vekjarann
        // birtast ofan a laestum skja, eins og venjuleg vekjaraklukka.
        startForeground(NOTIFICATION_ID, buildNotification())

        // Full-screen intent er EINA leidin sem kemur skjanum upp a laestum
        // sima. Beina raesingin her fyrir nedan fekk BAL_BLOCK (result 102)
        // i ollum tremur logcat-profunum a Galaxy A54 (2026-09-21): stada
        // sem forgrunnstjonusta veitir ekkert leyfi til bakgrunnsraesingar.
        // Hun er latin standa af tvi hun kostar ekkert og VIRKAR tegar
        // appid er sjalft opid - ta hefur tad synilegan glugga.
        launchAlarmScreenDirectly()

        // Annad vekjaraapp a somu sekundu getur lagst ofan a okkar skja, og
        // ta kemur hann aldrei aftur af sjalfu ser. Sja scheduleScreenRetries().
        scheduleScreenRetries()

        // Tha sem er ad spila - Spotify, hladvarp - er thaggad medan
        // vekjarinn hringir. An tessa blandast hljodin saman.
        requestAudioFocus()

        // Oryggisnet: ef enginn slekkur - siminn gleymdist heima, notandinn
        // er ekki vid - tha ma tjonustan ekki spila endalaust.
        handler.postDelayed({
            Log.i(TAG, "Tímamörk náð - stöðva vekjara sjálfkrafa")
            stopAlarm()
        }, AUTO_STOP_MINUTES * 60 * 1000L)

        // Vekjarahljod fyrst - baenin kemur tegar notandinn slekkur,
        // sja awake(). Lykkja: hljodid a ad hringja tar til hann vaknar.
        if (prefs.wakeWithSound) {
            stage = Stage.WAKE_SOUND
            updateNotification(alarmSoundLabel())
            playAudio(alarmSoundUri())
            player?.repeatMode = Player.REPEAT_MODE_ONE
            return
        }

        val repository = EpisodeRepository(this)
        val source = repository.playbackSource()

        val mediaUri: Uri? = when (source) {
            is EpisodeRepository.PlaybackSource.LocalFile -> Uri.fromFile(source.file)
            is EpisodeRepository.PlaybackSource.Stream -> Uri.parse(source.url)
            null -> fallbackUri()
        }

        if (mediaUri != null) {
            playAudio(mediaUri)
            if (source == null && !prefs.fallbackRas1) {
                player?.repeatMode = Player.REPEAT_MODE_ONE
            }
        } else {
            Log.e(TAG, "Ekkert hljóð til að spila — skjárinn og titringur verða að duga")
            startVibrationIfEnabled()
        }
    }

    /**
     * Stöðvar fyrri spilun án þess að slökkva á þjónustunni.
     * startAlarm getur komið tvisvar - nýr ACTION_START, eða Android
     * endurræsti þjónustuna - og má ekki leka ExoPlayer eða tvöfalda tímamörk.
     */
    private fun resetPlayback() {
        listening = false
        listeningState.value = false
        handler.removeCallbacksAndMessages(null)
        cancelScreenRetries()
        vibrator?.cancel()
        vibrator = null
        player?.release()
        player = null
    }

    /**
     * Opnar vekjaraskjainn beint, til vidbotar vid full-screen intent.
     *
     * Android takmarkar bakgrunnsraesingu a skjaum, svo tetta getur brugdist -
     * en tad kostar ekkert ad reyna og tvofoldun a leidum er einmitt tad sem
     * vekjari tarf.
     */
    private fun launchAlarmScreenDirectly() {
        try {
            startActivity(
                Intent(this, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að opna vekjaraskjá beint", e)
        }
    }

    /**
     * Skjarinn gaeti verid hulinn: annad vekjaraapp a somu sekundu
     * raesir sinn eigin skja, og sa sem raesir sig SIDAST lendir efst.
     * Logcat syndi ad full-screen intent okkar kemur 0,65-1,54 sek eftir
     * ad vekjarinn hringir, svo tad er ekki i okkar hondum hvor verdur
     * sidastur. Tapist kapphlaupid kemur skjarinn ekki aftur - hann er
     * i eigin verkefni og excludeFromRecents, svo tegar hinn vekjarinn er
     * afgreiddur fellur siminn a heimaskjainn.
     *
     * Endurtilraun er NY tilkynning med full-screen intent. Hvorki
     * startActivity (BAL_BLOCK) ne updateNotification() (uppfaersla a
     * sama audkenni raesir skjainn ekki aftur - hun gerist a hverjum
     * morgni an tess) koma skjanum upp.
     *
     * Hver tilraun gerir ekkert ef skjarinn sest. Fimm tilraunir, engin
     * lykkja: tvo forrit sem baedi endurheimta skja i sifellu vaeru flokt,
     * ekki vekjari.
     */
    private fun scheduleScreenRetries() {
        // Ekki cancelScreenRetries(): hun setur ringing = false, og ta
        // haettu allar tilraunirnar an tess ad gera nokkud.
        screenHandler.removeCallbacksAndMessages(null)
        SCREEN_RETRY_SECONDS.forEach { seconds ->
            screenHandler.postDelayed({ retryScreen(seconds) }, seconds * 1000L)
        }
    }

    private fun retryScreen(seconds: Long) {
        if (!ringing) return
        if (screenVisible.value) {
            // Kominn upp - aukatilkynningin a ekki lengur erindi i skuffuna.
            cancelRetryNotification()
            return
        }
        Log.i(TAG, "Vekjaraskjárinn sést ekki eftir ${seconds}s — birti full-screen intent aftur")
        try {
            val manager = getSystemService(NotificationManager::class.java)
            // Aflyst og birt a ny, ekki uppfaerd: adeins NY tilkynning
            // raesir full-screen intent.
            manager.cancel(RETRY_NOTIFICATION_ID)
            manager.notify(RETRY_NOTIFICATION_ID, buildNotification(retry = true))
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að birta vekjaraskjá aftur", e)
        }
    }

    private fun cancelScreenRetries() {
        ringing = false
        screenHandler.removeCallbacksAndMessages(null)
        cancelRetryNotification()
    }

    private fun cancelRetryNotification() {
        try {
            getSystemService(NotificationManager::class.java).cancel(RETRY_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að fjarlægja aukatilkynningu", e)
        }
    }

    /**
     * Bidur um AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE.
     *
     * "Exclusive" tydir ad annad hljod eigi ad tagna alveg - ekki bara laekka.
     * Vekjari a ekki ad keppa vid hladvarp sem gleymdist i gangi.
     */
    private fun requestAudioFocus() {
        try {
            val audioManager = getSystemService(AudioManager::class.java)
            val attributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                // Vid gefum ALDREI eftir - vekjari sem tagnar vid tilkynningu
                // fra odru appi er onytur.
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { }
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki hljóðfókus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            audioFocusRequest?.let {
                getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að skila hljóðfókus", e)
        }
        audioFocusRequest = null
    }

    /** Getur appid birt vekjarann a laestum skja? */
    private fun canUseFullScreen(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    private fun playAudio(uri: Uri) {
        // ExoPlayer raedur baedi vid venjulegar hljodskrar og HLS-streymi.
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_ALARM)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        // Vekjarastyrkur - ekki midilsstyrkur. Tannig heyrist baenin
        // tott siminn se a hljodlausri stillingu.
        raiseAlarmVolume()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, false)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_OFF

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            // Vid stoppum ALDREI sjalfkrafa tegar efni klarast
                            // - tha gaeti notandinn sofnad aftur. Vid faerum
                            // okkur bara a naesta stig.
                            advanceToNextStage()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.w(TAG, "Spilun mistókst - held áfram", error)
                        advanceToNextStage()
                    }
                })

                prepare()
                play()
            }

        startVolumeRamp()
    }

    // ------------------------------------------------------------------
    //  Vaxandi hljodstyrkur
    // ------------------------------------------------------------------

    /**
     * Haekkar hljodstyrk spilarans rolega ur naestum tognun i fullan styrk.
     *
     * Tetta breytir EKKI kerfisstyrknum - adeins styrk tessarar spilunar.
     * Tannig raskast ekkert hja notandanum tott vekjarinn se stodvadur i
     * midri haekkun.
     *
     * Titringurinn bidur tar til haekkuninni er lokid. Titringur medan
     * hljodid er enn lagt eydileggur einmitt tad sem fade-in a ad skila.
     */
    private fun startVolumeRamp() {
        if (!prefs.fadeInEnabled) {
            player?.volume = 1f
            startVibrationIfEnabled()
            return
        }

        val seconds = prefs.fadeInSeconds.coerceIn(5, 300)
        val steps = seconds * STEPS_PER_SECOND
        player?.volume = START_VOLUME

        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                step++
                val progress = step.toFloat() / steps
                player?.volume = START_VOLUME + (1f - START_VOLUME) * progress

                if (step < steps) {
                    handler.postDelayed(this, STEP_INTERVAL_MS)
                } else {
                    // Fullur styrkur naadur - nu ma titringurinn byrja.
                    startVibrationIfEnabled()
                }
            }
        }
        handler.postDelayed(runnable, STEP_INTERVAL_MS)
    }

    // ------------------------------------------------------------------
    //  Titringur
    // ------------------------------------------------------------------

    private fun startVibrationIfEnabled() {
        if (!prefs.vibrateEnabled) return

        val vib = obtainVibrator() ?: return
        if (!vib.hasVibrator()) return

        vibrator = vib

        // Bid, titringur, hle - endurtekid fra fyrsta lid.
        val pattern = longArrayOf(0, 500, 1200)
        val effect = VibrationEffect.createWaveform(pattern, 0)

        // USAGE_ALARM svo titringurinn komist i gegnum "Ekki trufla".
        val attributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            vib.vibrate(effect, attributes)
        } catch (e: Exception) {
            Log.w(TAG, "Titringur mistókst", e)
        }
    }

    private fun obtainVibrator(): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    } catch (e: Exception) {
        null
    }

    // ------------------------------------------------------------------

    /**
     * Faerir spilunina a naesta stig.
     *
     * Baen -> frettir -> varahljod. Frettunum er sleppt ef notandinn hefur
     * ekki valid taer, eda ef frettatimi dagsins naadist ekki - gamlar
     * frettir eru verri en engar.
     */
    private fun advanceToNextStage() {
        when (stage) {
            Stage.WAKE_SOUND -> {
                // Lykkja endar aldrei - hingad kemst adeins villa, t.d.
                // skemmd skra. Kirkjuklukkan tekur vid; notandinn er ekki vaknadur.
                stage = Stage.FALLBACK
                playBellLoop()
            }

            Stage.PRAYER -> {
                val news = EpisodeRepository(this).newsPlaybackSource()
                if (news != null) {
                    stage = Stage.NEWS
                    Log.i(TAG, "Bænin búin - spila fréttir")
                    updateNotification(prefs.newsTitle ?: getString(R.string.news_label))
                    playNext(Uri.fromFile(news))
                } else {
                    stage = Stage.FALLBACK
                    playFallbackTone()
                }
            }

            Stage.NEWS -> {
                stage = Stage.FALLBACK
                playFallbackTone()
            }

            Stage.FALLBACK -> {
                // Rás 1 eða eigið hljóð klikkaði — kirkjuklukkan má ekki þegja.
                playBellLoop()
            }
        }
    }

    /** Skiptir um efni an tess ad byggja spilarann upp a nytt. */
    private fun playNext(uri: Uri) {
        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            play()
        }
    }

    /** Uppfaerir tilkynninguna svo hun syni hvad er ad spila hverju sinni. */
    private fun updateNotification(text: String) {
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(text))
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að uppfæra tilkynningu", e)
        }
    }

    /**
     * Eldri klukka Staðarfellskirkju, í APK-inu.
     * Kerfisvekjari getur vantað; þá þegði appið áður.
     */
    private fun bundledBellUri(): Uri =
        Uri.parse("android.resource://$packageName/${R.raw.stadarfell_eldri}")

    /**
     * Vekjarahljóðið sem notandinn valdi, annars kirkjuklukkan.
     * Skráin er afrituð í device-protected geymslu (AlarmSoundStore)
     * og því læsileg þótt síminn hafi endurræst sig og sé enn læstur.
     */
    private fun alarmSoundUri(): Uri =
        AlarmSoundStore(this).file()?.let { Uri.fromFile(it) } ?: bundledBellUri()

    /** Heitið í tilkynningunni — sama hljóð og alarmSoundUri spilar. */
    private fun alarmSoundLabel(): String =
        AlarmSoundStore(this).file()?.let { prefs.alarmSoundTitle }
            ?: getString(R.string.fallback_bell)

    private fun ras1Uri(): Uri =
        Uri.parse(com.morgunbaen.app.data.RuvClient.RAS1_LIVE_URL)

    /** Notandinn velur vekjarahljóð eða Rás 1. Streymi endar ekki. */
    private fun fallbackUri(): Uri {
        if (prefs.fallbackRas1) {
            Log.i(TAG, "Varaleið: Rás 1")
            updateNotification(getString(R.string.ras1_fallback))
            return ras1Uri()
        }
        Log.i(TAG, "Varaleið: vekjarahljóð")
        updateNotification(alarmSoundLabel())
        return alarmSoundUri()
    }

    /** Ef baenin klikkar eda klarast — valin varaleið. */
    private fun playFallbackTone() {
        playUri(fallbackUri(), loop = !prefs.fallbackRas1)
    }

    /**
     * Síðasta vörnin: ALLTAF innbyggða klukkan, aldrei hljóð notandans.
     * Klikki eigið hljóð (skemmd skrá, óstutt snið) má vekjarinn ekki þegja.
     */
    private fun playBellLoop() {
        updateNotification(getString(R.string.fallback_bell))
        playUri(bundledBellUri(), loop = true)
    }

    private fun playUri(uri: Uri, loop: Boolean) {
        val existing = player
        if (existing == null) {
            playAudio(uri)
            if (loop) player?.repeatMode = Player.REPEAT_MODE_ONE
            return
        }
        existing.setMediaItem(MediaItem.fromUri(uri))
        existing.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        existing.prepare()
        existing.play()
    }

    private fun raiseAlarmVolume() {
        try {
            val audioManager = getSystemService(AudioManager::class.java)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            // Aldrei laekka tad sem notandinn valdi - bara haekka ef tad er of lagt.
            val minimum = (max * 0.6).toInt()
            if (current < minimum) {
                // Muna hvad notandinn hafdi valid svo vid getum skilad tvi.
                // An tessa saeti siminn eftir a haerri styrk en eigandinn valdi
                // - og hann myndi aldrei atta sig a hvers vegna.
                originalAlarmVolume = current
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, minimum, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að stilla hljóðstyrk", e)
        }
    }

    /**
     * @param retry Endurtilraun ur scheduleScreenRetries(). Ta ma ekki
     *              hreinsa verkefnid (CLEAR_TASK) - skjarinn er til, bara
     *              hulinn, og a ad koma aftur eins og hann var. Eigid
     *              requestCode er naudsynlegt: PendingIntent greinir ekki a
     *              milli intent-fana, svo med sama koda (0) myndi
     *              FLAG_UPDATE_CURRENT skila upphaflegu utgafunni.
     */
    private fun buildNotification(contentText: String? = null, retry: Boolean = false): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            if (retry) RETRY_REQUEST_CODE else 0,
            Intent(this, AlarmActivity::class.java).apply {
                flags = if (retry) {
                    Intent.FLAG_ACTIVITY_NEW_TASK
                } else {
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // I "Vekjarahljod, svo baen" tydir Slokkva "eg er vaknadur" -
        // baenin a ad fylgja, lika tegar slokkt er ur tilkynningunni.
        val dismissIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).apply {
                action = if (prefs.wakeWithSound) ACTION_AWAKE else ACTION_DISMISS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AlarmService::class.java).apply { action = ACTION_SNOOZE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = contentText ?: prefs.cachedTitle ?: getString(R.string.app_name)

        if (!canUseFullScreen()) {
            Log.w(TAG, "Full-screen intent ekki leyft - tilkynningin ein sér verður að duga")
        }

        return NotificationCompat.Builder(this, MorgunbaenApp.CHANNEL_ALARM)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(title)
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            // Tetta er lykillinn ad tvi ad vekjarinn birtist a laestum skja:
            .setFullScreenIntent(fullScreenIntent, true)
            // Ef enginn skjar birtist eru tessir tveir takkar eina leidin til
            // ad slokkva. Teir MEGA tvi ekki vanta.
            .addAction(R.drawable.ic_alarm, getString(R.string.dismiss), dismissIntent)
            .addAction(R.drawable.ic_alarm, getString(R.string.snooze), snoozeIntent)
            // Ad ytt se a tilkynninguna sjalfa opnar lika vekjaraskjainn.
            .setContentIntent(fullScreenIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    // ------------------------------------------------------------------
    //  Baen eftir voknun
    // ------------------------------------------------------------------

    /**
     * Notandinn slokkti a vekjarahljodinu i "Vekjarahljod, svo baen".
     * Vekjarinn thagnar og skilar hljodstyrk og fokus; svo fer eftir
     * afterWake hvort baenin byrjar, skjarinn spyr, eda tilkynning bidur.
     *
     * fromScreen = false tydir ad slokkt var ur tilkynningunni. Tha er
     * enginn skjar til ad spyrja, svo "Spyrja mig" verdur ad "Seinna".
     */
    private fun awake(fromScreen: Boolean) {
        resetPlayback()
        restoreAlarmVolume()
        abandonAudioFocus()
        when (prefs.afterWake) {
            Prefs.AFTER_AUTO -> startListening()
            Prefs.AFTER_ASK -> {
                if (!fromScreen) postListenLaterNotification()
                stopAlarm()
            }
            else -> {
                postListenLaterNotification()
                stopAlarm()
            }
        }
    }

    /**
     * Spilar baen dagsins sem venjulegan midil - notandinn er vaknadur.
     *
     * USAGE_MEDIA: midlastyrkur, ExoPlayer ser sjalfur um hljodfokus (simtal
     * gerir hle) og um ad gera hle tegar heyrnartol eru tekin ur.
     * Engin timamork og ekkert varahljod: tegar efnid er buid er tvi lokid.
     */
    private fun startListening() {
        resetPlayback()
        // Vekjarinn helt vakandi i 15 min. ExoPlayer heldur sjalfur
        // vakandi medan hann spilar - okkar las ma fara.
        releaseWakeLock()
        cancelListenLaterNotification()
        listening = true
        listeningState.value = true
        stage = Stage.PRAYER

        val source = EpisodeRepository(this).playbackSource()
        val (uri, label) = when (source) {
            is EpisodeRepository.PlaybackSource.LocalFile ->
                Uri.fromFile(source.file) to (prefs.cachedTitle ?: getString(R.string.app_name))
            is EpisodeRepository.PlaybackSource.Stream ->
                Uri.parse(source.url) to (prefs.cachedTitle ?: getString(R.string.app_name))
            // Engin baen - Ras 1 i beinni er naest thvi sem RUV er ad senda.
            null -> ras1Uri() to getString(R.string.ras1_fallback)
        }
        startForeground(NOTIFICATION_ID, buildListenNotification(label))

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) advanceListening()
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.w(TAG, "Hlustun mistókst", error)
                        advanceListening()
                    }
                })
                prepare()
                play()
            }
    }

    /** Baen -> frettir (ef valdar og til) -> buid. Aldrei varahljod. */
    private fun advanceListening() {
        if (stage == Stage.PRAYER) {
            val news = EpisodeRepository(this).newsPlaybackSource()
            if (news != null) {
                stage = Stage.NEWS
                val label = prefs.newsTitle ?: getString(R.string.news_label)
                notifySafely(NOTIFICATION_ID, buildListenNotification(label))
                playNext(Uri.fromFile(news))
                return
            }
        }
        Log.i(TAG, "Hlustun lokið")
        stopAlarm()
    }

    private fun buildListenNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MorgunbaenApp.CHANNEL_PRAYER)
            .setContentTitle(getString(R.string.listen_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_alarm)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_alarm, getString(R.string.listen_stop), stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * "Seinna": tilkynning sem spilar baenina tegar notandanum hentar.
     * Hverfur eftir 12 klst - baen dagsins a ekki ad bida til morguns.
     */
    private fun postListenLaterNotification() {
        val listenIntent = PendingIntent.getForegroundService(
            this,
            3,
            Intent(this, AlarmService::class.java).apply { action = ACTION_LISTEN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, MorgunbaenApp.CHANNEL_PRAYER)
            .setContentTitle(getString(R.string.listen_later_title))
            .setContentText(prefs.cachedTitle ?: getString(R.string.listen_later_text))
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentIntent(listenIntent)
            .addAction(R.drawable.ic_alarm, getString(R.string.listen_now), listenIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(LISTEN_LATER_TIMEOUT_MS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        notifySafely(LISTEN_LATER_NOTIFICATION_ID, notification)
    }

    private fun cancelListenLaterNotification() {
        try {
            getSystemService(NotificationManager::class.java)
                .cancel(LISTEN_LATER_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að fjarlægja tilkynningu", e)
        }
    }

    private fun notifySafely(id: Int, notification: Notification) {
        try {
            getSystemService(NotificationManager::class.java).notify(id, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að birta tilkynningu", e)
        }
    }

    private fun snooze() {
        AlarmScheduler.scheduleSnooze(this, prefs.snoozeMinutes)
        stopAlarm()
    }

    /** Skilar hljodstyrknum eins og hann var - en adeins ef vid breyttum honum. */
    private fun restoreAlarmVolume() {
        if (originalAlarmVolume < 0) return
        try {
            getSystemService(AudioManager::class.java)
                .setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að skila hljóðstyrk", e)
        }
        originalAlarmVolume = -1
    }

    private fun stopAlarm() {
        listening = false
        listeningState.value = false
        handler.removeCallbacksAndMessages(null)
        cancelScreenRetries()
        restoreAlarmVolume()
        abandonAudioFocus()
        vibrator?.cancel()
        vibrator = null
        player?.release()
        player = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "morgunbaen:alarm"
        ).apply {
            // Sleppir sjalfkrafa eftir 15 min svo hann festist aldrei.
            acquire(15 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        listeningState.value = false
        handler.removeCallbacksAndMessages(null)
        cancelScreenRetries()
        restoreAlarmVolume()
        abandonAudioFocus()
        vibrator?.cancel()
        player?.release()
        player = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 42

        /** Upphafsstyrkur fade-in. Nogu hatt til ad heyrast, nogu lagt til ad vekja mjukt. */
        private const val START_VOLUME = 0.05f
        private const val STEPS_PER_SECOND = 4
        private const val STEP_INTERVAL_MS = 250L

        /**
         * Haemarkslengd spilunar. Vekjarinn stoppar EKKI tegar baenin klarast
         * - ta gaeti notandinn sofnad aftur - en hann ma heldur ekki spila
         * endalaust ef enginn er heima.
         */
        private const val AUTO_STOP_MINUTES = 15L

        /** Eftir þetta er endursend ACTION_START úrelt, ekki vakning. */
        private const val STALE_START_LIMIT_MS = 20 * 60 * 1000L

        const val ACTION_START = "com.morgunbaen.app.START_ALARM"
        const val ACTION_DISMISS = "com.morgunbaen.app.DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.morgunbaen.app.SNOOZE_ALARM"
        const val ACTION_AWAKE = "com.morgunbaen.app.AWAKE"
        const val ACTION_LISTEN = "com.morgunbaen.app.LISTEN"
        private const val EXTRA_FROM_SCREEN = "from_screen"

        private const val LISTEN_LATER_NOTIFICATION_ID = 43

        /** Aukatilkynningin sem reynir ad koma huldum vekjaraskja aftur upp. */
        private const val RETRY_NOTIFICATION_ID = 44
        private const val RETRY_REQUEST_CODE = 4

        /**
         * Hvenaer athugad er hvort skjarinn sjaist. Su fyrsta kemur a eftir
         * badum raesingunum i logcat-profunum (0,65-1,54 sek); hinar na
         * skjanum aftur eftir ad hinn vekjarinn hefur verid afgreiddur.
         */
        private val SCREEN_RETRY_SECONDS = listOf(2L, 5L, 10L, 20L, 40L)
        private const val LISTEN_LATER_TIMEOUT_MS = 12 * 60 * 60 * 1000L

        /**
         * Er baenin ad spila sem hlustun nuna? AlarmActivity lokar ser
         * tegar tetta fer ur true i false - hlustun lokid eda stodvud.
         * Sama ferli, svo einfalt StateFlow dugar.
         */
        val listeningState = MutableStateFlow(false)

        /**
         * Er vekjaraskjarinn synilegur? AlarmActivity setur tetta i onResume
         * og tekur tad nidur i onPause - EKKI i onCreate, tvi huldur skjar
         * er buinn til en osynilegur, og ta haettu endurtilraunirnar
         * einmitt tegar taer attu ad keyra.
         */
        val screenVisible = MutableStateFlow(false)

        /** Slokkt a vekjarahljodinu a skjanum - baenin fylgir samkvaemt afterWake. */
        fun awake(context: Context) {
            context.startService(
                Intent(context, AlarmService::class.java).apply {
                    action = ACTION_AWAKE
                    putExtra(EXTRA_FROM_SCREEN, true)
                }
            )
        }

        /** Hlusta a baenina - ur "Spyrja mig"-skjanum. */
        fun listen(context: Context) {
            context.startForegroundService(
                Intent(context, AlarmService::class.java).apply { action = ACTION_LISTEN }
            )
        }

        fun dismiss(context: Context) {
            context.startService(
                Intent(context, AlarmService::class.java).apply { action = ACTION_DISMISS }
            )
        }

        fun snooze(context: Context) {
            context.startService(
                Intent(context, AlarmService::class.java).apply { action = ACTION_SNOOZE }
            )
        }
    }
}
