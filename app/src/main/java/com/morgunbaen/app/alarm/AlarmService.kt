package com.morgunbaen.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
    }

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

        val title = prefs.cachedTitle ?: getString(R.string.app_name)

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
            .addAction(R.drawable.ic_alarm, getString(R.string.dismiss), dismissIntent)
            .build()
    }

    private fun snooze() {
        AlarmScheduler.scheduleSnooze(this, prefs.snoozeMinutes)
        stopAlarm()
    }

    private fun stopAlarm() {
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
        player?.release()
        player = null
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 42

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
