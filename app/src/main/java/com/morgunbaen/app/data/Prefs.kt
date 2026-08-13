package com.morgunbaen.app.data

import android.content.Context

/**
 * Allar stillingar appsins a einum stad.
 * Notar SharedPreferences - einfalt og nogu gott fyrir svona litid app.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("morgunbaen", Context.MODE_PRIVATE)

    /** Er vekjarinn virkur? */
    var alarmEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Klukkutimi vekjarans (0-23). */
    var alarmHour: Int
        get() = sp.getInt(KEY_HOUR, 7)
        set(value) = sp.edit().putInt(KEY_HOUR, value).apply()

    /** Minuta vekjarans (0-59). */
    var alarmMinute: Int
        get() = sp.getInt(KEY_MINUTE, 0)
        set(value) = sp.edit().putInt(KEY_MINUTE, value).apply()

    /**
     * A hvada dogum vekjarinn hringir.
     * Notar Calendar.SUNDAY = 1 ... Calendar.SATURDAY = 7.
     * Sjalfgefid: virkir dagar, tvi Morgunbaenin er bara flutt tha.
     */
    var alarmDays: Set<Int>
        get() = sp.getStringSet(KEY_DAYS, DEFAULT_DAYS)!!.map { it.toInt() }.toSet()
        set(value) = sp.edit()
            .putStringSet(KEY_DAYS, value.map { it.toString() }.toSet())
            .apply()

    /** Slod a nidurhaladu hljodskrana i geymslu appsins, ef hun er til. */
    var cachedFilePath: String?
        get() = sp.getString(KEY_FILE_PATH, null)
        set(value) = sp.edit().putString(KEY_FILE_PATH, value).apply()

    /** Bein slod hja RUV - notud ef ekki tokst ad hlada nidur (HLS-streymi). */
    var cachedStreamUrl: String?
        get() = sp.getString(KEY_STREAM_URL, null)
        set(value) = sp.edit().putString(KEY_STREAM_URL, value).apply()

    /** Titill thattarins sem er tilbuinn - oftast nafn prestsins. */
    var cachedTitle: String?
        get() = sp.getString(KEY_TITLE, null)
        set(value) = sp.edit().putString(KEY_TITLE, value).apply()

    /** Hvenaer thatturinn sem vid eigum var fluttur. */
    var cachedFirstrun: String?
        get() = sp.getString(KEY_FIRSTRUN, null)
        set(value) = sp.edit().putString(KEY_FIRSTRUN, value).apply()

    /** Audkenni thattarins sem vid eigum - notad til ad sleppa endurtekinni sokn. */
    var cachedEpisodeId: String?
        get() = sp.getString(KEY_EPISODE_ID, null)
        set(value) = sp.edit().putString(KEY_EPISODE_ID, value).apply()

    /** Timastimpill sidustu velheppnudu sokn, i millisekundum. */
    var lastSyncMillis: Long
        get() = sp.getLong(KEY_LAST_SYNC, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_SYNC, value).apply()

    /** Hversu lengi blundur varir, i minutum. */
    var snoozeMinutes: Int
        get() = sp.getInt(KEY_SNOOZE, 9)
        set(value) = sp.edit().putInt(KEY_SNOOZE, value).apply()

    companion object {
        private const val KEY_ENABLED = "alarm_enabled"
        private const val KEY_HOUR = "alarm_hour"
        private const val KEY_MINUTE = "alarm_minute"
        private const val KEY_DAYS = "alarm_days"
        private const val KEY_FILE_PATH = "cached_file_path"
        private const val KEY_STREAM_URL = "cached_stream_url"
        private const val KEY_TITLE = "cached_title"
        private const val KEY_FIRSTRUN = "cached_firstrun"
        private const val KEY_EPISODE_ID = "cached_episode_id"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_SNOOZE = "snooze_minutes"

        // Manudagur (2) til fostudags (6)
        private val DEFAULT_DAYS = setOf("2", "3", "4", "5", "6")
    }
}
