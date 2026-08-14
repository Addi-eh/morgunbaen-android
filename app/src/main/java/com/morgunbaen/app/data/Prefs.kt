package com.morgunbaen.app.data

import android.content.Context

/**
 * Allar stillingar appsins a einum stad.
 * Notar SharedPreferences - einfalt og nogu gott fyrir svona litid app.
 */
class Prefs(context: Context) {

    // deviceStorage svo stillingarnar seu laesilegar fyrir upplasningu simans.
    private val sp = context.deviceStorage
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    /** Hvenaer vekjarinn hringdi sidast i alvoru. Notad til ad greina bilanir. */
    var lastAlarmFiredMillis: Long
        get() = sp.getLong(KEY_LAST_FIRED, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_FIRED, value).apply()

    /**
     * Hvenaer notandinn var sidast latinn vita af vekjara sem klikkadi.
     * Kemur i veg fyrir ad sama vidvorunin se endurtekin i sifellu.
     */
    var missedAlarmAcknowledged: Long
        get() = sp.getLong(KEY_MISSED_ACK, 0L)
        set(value) = sp.edit().putLong(KEY_MISSED_ACK, value).apply()

    /**
     * Spila frettir strax a eftir baeninni.
     *
     * Frettirnar eru naesti dagskrarlidur a eftir Morgunbaeninni, svo tetta
     * speglar utsendinguna sjalfa.
     */
    var newsEnabled: Boolean
        get() = sp.getBoolean(KEY_NEWS_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_NEWS_ENABLED, value).apply()

    /** Slod a nidurhaladan frettatima, ef hann er til. */
    var newsFilePath: String?
        get() = sp.getString(KEY_NEWS_PATH, null)
        set(value) = sp.edit().putString(KEY_NEWS_PATH, value).apply()

    var newsTitle: String?
        get() = sp.getString(KEY_NEWS_TITLE, null)
        set(value) = sp.edit().putString(KEY_NEWS_TITLE, value).apply()

    /** Hvenaer frettatiminn sem vid eigum var fluttur. */
    var newsFirstrun: String?
        get() = sp.getString(KEY_NEWS_FIRSTRUN, null)
        set(value) = sp.edit().putString(KEY_NEWS_FIRSTRUN, value).apply()

    var newsEpisodeId: String?
        get() = sp.getString(KEY_NEWS_ID, null)
        set(value) = sp.edit().putString(KEY_NEWS_ID, value).apply()

    /**
     * Annar vekjaratimi um helgar.
     *
     * Morgunbaenin er adeins flutt a virkum dogum, svo um helgar spilar
     * appid sidustu baen vikunnar. Margir vilja sofa lengur ta - en samt
     * ekki sleppa henni alveg.
     */
    var weekendTimeEnabled: Boolean
        get() = sp.getBoolean(KEY_WEEKEND_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_WEEKEND_ENABLED, value).apply()

    var weekendHour: Int
        get() = sp.getInt(KEY_WEEKEND_HOUR, 9)
        set(value) = sp.edit().putInt(KEY_WEEKEND_HOUR, value).apply()

    var weekendMinute: Int
        get() = sp.getInt(KEY_WEEKEND_MINUTE, 0)
        set(value) = sp.edit().putInt(KEY_WEEKEND_MINUTE, value).apply()

    /**
     * Vaxandi hljodstyrkur: byrjar lagt og haekkar rolega upp i fullan styrk.
     *
     * SJALFGEFID AF. Baenin er talad mal, ekki tonn - fyrstu setningarnar
     * hverfa ef styrkurinn er enn ad haekka tegar tær eru fluttar.
     * Ta vaknar folk vid baen sem tad heyrdi ekki byrjunina a.
     */
    var fadeInEnabled: Boolean
        get() = sp.getBoolean(KEY_FADE_IN, false)
        set(value) = sp.edit().putBoolean(KEY_FADE_IN, value).apply()

    /** Hversu lengi hljodstyrkurinn er ad na fullum styrk, i sekundum. */
    var fadeInSeconds: Int
        get() = sp.getInt(KEY_FADE_SECONDS, 30)
        set(value) = sp.edit().putInt(KEY_FADE_SECONDS, value).apply()

    /**
     * Titringur.
     *
     * SJALFGEFID AF. Sudid i nattbordinu keppir vid rodd prestsins og
     * gerir hana erfidari ad heyra. Tetta er baenavekjari, ekki
     * verksmidjuflauta - hljodid eitt a ad duga.
     *
     * Se kveikt a honum bidur hann tar til hljodstyrkurinn hefur nad
     * fullum styrk, se fade-in lika virkt.
     */
    var vibrateEnabled: Boolean
        get() = sp.getBoolean(KEY_VIBRATE, false)
        set(value) = sp.edit().putBoolean(KEY_VIBRATE, value).apply()

    /** Hefur notandinn afgreitt Samsung-leidbeiningarnar? */
    var oemGuideDone: Boolean
        get() = sp.getBoolean(KEY_OEM_GUIDE, false)
        set(value) = sp.edit().putBoolean(KEY_OEM_GUIDE, value).apply()

    /** Hversu lengi blundur varir, i minutum. */
    var snoozeMinutes: Int
        get() = sp.getInt(KEY_SNOOZE, 9)
        set(value) = sp.edit().putInt(KEY_SNOOZE, value).apply()

    /**
     * Hvenaer virkur blundur a ad hringja, i millisekundum.
     * 0 = enginn blundur i bidi.
     *
     * Vistað sér svo blundurinn lifi af endurræsingu og stillingabreytingu
     * - hann má ekki deila PendingIntent með daglega vekjaranum.
     */
    var snoozeUntilMillis: Long
        get() = sp.getLong(KEY_SNOOZE_UNTIL, 0L)
        set(value) {
            // commit svo gildið nái á disk áður en ferlið getur dáið
            // (endurræsing, drepið app). apply() uppfærir minnið strax —
            // sami ferill les alltaf nýja gildið — en diskskrifin er ósamstillt.
            sp.edit().putLong(KEY_SNOOZE_UNTIL, value).commit()
        }

    /**
     * Næsti daglegi hringitími sem var skráður síðast.
     * Heilsuvöktunin ber hann saman við lastAlarmFiredMillis —
     * ekki previousTriggerTime, sem breytist ef notandinn hreyfir klukkuna.
     */
    var lastScheduledTriggerMillis: Long
        get() = sp.getLong(KEY_LAST_SCHEDULED, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_SCHEDULED, value).apply()

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
        private const val KEY_SNOOZE_UNTIL = "snooze_until"
        private const val KEY_LAST_FIRED = "last_alarm_fired"
        private const val KEY_LAST_SCHEDULED = "last_scheduled_trigger"
        private const val KEY_MISSED_ACK = "missed_alarm_ack"
        private const val KEY_OEM_GUIDE = "oem_guide_done"
        private const val KEY_FADE_IN = "fade_in_enabled"
        private const val KEY_FADE_SECONDS = "fade_in_seconds"
        private const val KEY_VIBRATE = "vibrate_enabled"
        private const val KEY_NEWS_ENABLED = "news_enabled"
        private const val KEY_NEWS_PATH = "news_file_path"
        private const val KEY_NEWS_TITLE = "news_title"
        private const val KEY_NEWS_FIRSTRUN = "news_firstrun"
        private const val KEY_NEWS_ID = "news_episode_id"
        private const val KEY_WEEKEND_ENABLED = "weekend_time_enabled"
        private const val KEY_WEEKEND_HOUR = "weekend_hour"
        private const val KEY_WEEKEND_MINUTE = "weekend_minute"

        const val PREFS_NAME = "morgunbaen"

        // Manudagur (2) til fostudags (6)
        private val DEFAULT_DAYS = setOf("2", "3", "4", "5", "6")
    }
}
