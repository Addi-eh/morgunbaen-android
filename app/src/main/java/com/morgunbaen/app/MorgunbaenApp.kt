package com.morgunbaen.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import com.morgunbaen.app.alarm.AlarmScheduler
import com.morgunbaen.app.work.SyncWorker

/**
 * Raesipunktur appsins. Setur upp tilkynningarasir og
 * gengur ur skugga um ad bakgrunnssoknin se skrad.
 */
class MorgunbaenApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        SyncWorker.schedule(this)
        AlarmScheduler.schedule(this)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Vekjararásin verdur ad hafa haesta mikilvaegi, annars
        // birtist hun ekki a laestum skja.
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alarm_desc)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            // Tjonustan spilar hljodid sjalf - rasin ma tvi vera hljod.
            setSound(null, null)
        }

        manager.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "morgunbaen_alarm"
    }
}
