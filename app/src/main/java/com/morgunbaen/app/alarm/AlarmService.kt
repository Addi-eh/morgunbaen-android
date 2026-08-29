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
import com.morgunbaen.app.data.EpisodeRepository
import com.morgunbaen.app.data.Prefs

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
     * loks varahljod sem spilar tar til slokkt er.
     */
    private var stage = Stage.PRAYER

    private enum class Stage { PRAYER, NEWS, FALLBACK }
    private val handler = Handler(Looper.getMainLooper())
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

        // Halda ordgjafanum vakandi medan spilad er.
        acquireWakeLock()

        // Forgrunnstilkynning MED fullum skja - tetta laetur vekjarann
        // birtast ofan a laestum skja, eins og venjuleg vekjaraklukka.
        startForeground(NOTIFICATION_ID, buildNotification())

        // Full-screen intent er RETTA leidin til ad birta vekjarann - en fra
        // Android 14 getur kerfid neitad honum. Tha spilar hljodid an tess ad
        // nokkur skjar birtist og notandinn hefur enga leid til ad slokkva
        // nema drepa appid.
        //
        // Tess vegna reynum vid lika ad opna skjainn beint. Tetta er
        // bakgrunnsraesing sem Android getur hafnad, svo hun er varin -
        // en tegar hun tekst bjargar hun deginum.
        launchAlarmScreenDirectly()

        // Tha sem er ad spila - Spotify, hladvarp - er thaggad medan
        // vekjarinn hringir. An tessa blandast hljodin saman.
        requestAudioFocus()

        // Oryggisnet: ef enginn slekkur - siminn gleymdist heima, notandinn
        // er ekki vid - tha ma tjonustan ekki spila endalaust.
        handler.postDelayed({
            Log.i(TAG, "Tímamörk náð - stöðva vekjara sjálfkrafa")
            stopAlarm()
        }, AUTO_STOP_MINUTES * 60 * 1000L)

        val repository = EpisodeRepository(this)
        val source = repository.playbackSource()

        val mediaUri: Uri? = when (source) {
            is EpisodeRepository.PlaybackSource.LocalFile -> Uri.fromFile(source.file)
            is EpisodeRepository.PlaybackSource.Stream -> Uri.parse(source.url)
            null -> {
                // Engin baen a diski. Rás 1 er næst útvarpsupprunanum —
                // ef netið er til staðar heyrist útsendingin sjálf.
                // Streymið endar ekki, svo 15 mínútna tímamörkin slökkva.
                // Brjóti streymið (ekkert net) fer onPlayerError í fréttir
                // eða kirkjuklukku.
                Log.w(TAG, "Engin bæn á diski - streymi Rásar 1")
                updateNotification(getString(R.string.ras1_fallback))
                Uri.parse(com.morgunbaen.app.data.RuvClient.RAS1_LIVE_URL)
            }
        }

        if (mediaUri != null) {
            playAudio(mediaUri)
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
        handler.removeCallbacksAndMessages(null)
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
                // Varahljodid er thegar i lykkju - ekkert ad gera.
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

    /** Ef baenin klikkar eda klarast - kirkjuklukka i lykkju. */
    private fun playFallbackTone() {
        val bell = bundledBellUri()
        val existing = player
        if (existing == null) {
            playAudio(bell)
            player?.repeatMode = Player.REPEAT_MODE_ONE
            return
        }
        existing.setMediaItem(MediaItem.fromUri(bell))
        existing.repeatMode = Player.REPEAT_MODE_ONE
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

    private fun buildNotification(contentText: String? = null): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS },
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
        handler.removeCallbacksAndMessages(null)
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
        handler.removeCallbacksAndMessages(null)
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
