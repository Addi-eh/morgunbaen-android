package com.morgunbaen.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
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
        return START_NOT_STICKY
    }

    private fun startAlarm() {
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

        val repository = EpisodeRepository(this)
        val source = repository.playbackSource()

        val mediaUri: Uri = when (source) {
            is EpisodeRepository.PlaybackSource.LocalFile -> Uri.fromFile(source.file)
            is EpisodeRepository.PlaybackSource.Stream -> Uri.parse(source.url)
            null -> {
                // Engin baen til stadar - notandinn ma samt EKKI sofa yfir sig.
                Log.w(TAG, "Engin bæn til - nota varahljóð símans")
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        }

        playAudio(mediaUri)
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
                            // Baenin er buin - vid stoppum EKKI sjalfkrafa,
                            // tvi tá gaeti notandinn sofnad aftur.
                            // Spilum varahljod tar til slokkt er a vekjaranum.
                            playFallbackTone()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.w(TAG, "Spilun mistókst - skipti í varahljóð", error)
                        playFallbackTone()
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

    /** Ef baenin klikkar eda klarast - spilum venjulegt vekjarahljod i lykkju. */
    private fun playFallbackTone() {
        val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        player?.apply {
            setMediaItem(MediaItem.fromUri(fallback))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            play()
        }
    }

    private fun raiseAlarmVolume() {
        try {
            val audioManager = getSystemService(AudioManager::class.java)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            // Aldrei laekka tad sem notandinn valdi - bara haekka ef tad er of lagt.
            val minimum = (max * 0.6).toInt()
            if (current < minimum) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, minimum, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Náði ekki að stilla hljóðstyrk", e)
        }
    }

    private fun buildNotification(): Notification {
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

        val title = prefs.cachedTitle ?: getString(R.string.app_name)

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

    private fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)
        vibrator?.cancel()
        vibrator = null
        player?.release()
        player = null
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
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
