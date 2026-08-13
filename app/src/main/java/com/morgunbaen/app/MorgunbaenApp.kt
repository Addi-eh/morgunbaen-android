package com.morgunbaen.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.UserManager
import com.morgunbaen.app.alarm.AlarmScheduler
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.data.deviceStorage
import com.morgunbaen.app.work.CatchUpScheduler
import com.morgunbaen.app.work.SyncWorker

/**
 * Raesipunktur appsins. Setur upp tilkynningarasir og
 * gengur ur skugga um ad bakgrunnssoknin se skrad.
 */
class MorgunbaenApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Appid er directBootAware, svo onCreate getur keyrt ADUR en notandinn
        // hefur opnad simann eftir endurraesingu. Allt her verdur ad tola tad.
        migrateLegacyPrefs()
        createNotificationChannels()
        AlarmScheduler.schedule(this)

        // Soknarglugginn notar AlarmManager og virkar tvi i Direct Boot.
        CatchUpScheduler.schedule(this)

        // WorkManager notar gagnagrunn i credential-geymslu og hrynur ef hann
        // er raestur i Direct Boot. Bakgrunnssoknin bidur - hun liggur ekkert a.
        val userManager = getSystemService(UserManager::class.java)
        if (userManager.isUserUnlocked) {
            SyncWorker.schedule(this)
        }
    }

    /**
     * Faerir stillingar fra eldri utgafum yfir i device-protected geymslu.
     * An tessa myndi notandi sem uppfaerir appid missa vekjarann sinn.
     * Ohaett ad kalla oft - Android gerir ekkert ef tar er ekkert ad faera.
     */
    private fun migrateLegacyPrefs() {
        try {
            deviceStorage.moveSharedPreferencesFrom(this, Prefs.PREFS_NAME)
        } catch (e: Exception) {
            // Ekkert ad faera, eda vid erum i Direct Boot og gamla svaedid
            // er ekki laesilegt. Hvorugt er banvaent.
        }
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
