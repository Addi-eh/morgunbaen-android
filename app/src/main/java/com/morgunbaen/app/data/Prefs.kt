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
     *
     * Sjalfgefid virkir dagar - EKKI vegna dagskrarinnar. Morgunbaenin er
     * flutt kl. 06:55 ALLA daga, lika um helgar. Tetta er einfaldlega tad
     * sem flestir vilja: sofa lengur um helgar. Notandinn getur valid
     * hvada daga sem er og faer alltaf baen tess dags.
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
     * Eigin tími einstakra daga, í mínútum frá miðnætti. Dagur sem vantar
     * hér notar alarmHour:alarmMinute.
     *
     * Morgunbænin er flutt alla daga. Þetta er hrein tímastilling: sofa
     * lengur á laugardögum, fara fyrr á fætur á föstudögum — og fá samt
     * bæn ÞESS dags. Leysti af hólmi „Annar tími um helgar" (v0.96), og
     * rofinn sem kveikti á því var lagður niður í v0.972 — sjá
     * migrateRetiredPerDaySwitch().
     *
     * Vistað sem "1=540,7=540" — StringSet tapar engu en er óraðað og
     * erfiðara að lesa í adb.
     */
    var dayTimes: Map<Int, Int>
        get() = parseDayTimes(sp.getString(KEY_DAY_TIMES, null))
        // KEY_PER_DAY_ENABLED fylgir kortinu. Rofinn sem hann stýrði var
        // lagður niður í v0.972 — dagarnir eru nú stilltir beint í dálkunum
        // og tómt kort þýðir „sami tími alla daga". Lykillinn er skrifaður
        // áfram, jafnaður við kortið, svo hann geti ekki sagt ósatt.
        set(value) = sp.edit()
            .putString(KEY_DAY_TIMES, value.entries.joinToString(",") { "${it.key}=${it.value}" })
            .putBoolean(KEY_PER_DAY_ENABLED, value.isNotEmpty())
            .apply()

    /**
     * Kortið sem TriggerTimes á að nota. Áður var það tómt þegar rofinn var
     * af; nú er kortið sjálft sannleikurinn — sjá migrateRetiredPerDaySwitch,
     * sem hreinsaði falda tíma ÁÐUR en þeir gátu orðið virkir.
     */
    val activeDayTimes: Map<Int, Int>
        get() = dayTimes

    /**
     * Færir „Annar tími um helgar" (til og með v0.952) yfir í tíma fyrir
     * hvern dag. Keyrir einu sinni; gömlu lyklarnir eru aðeins lesnir hér.
     */
    fun migrateWeekendTime() {
        if (sp.getBoolean(KEY_PER_DAY_MIGRATED, false)) return
        val editor = sp.edit()
        if (sp.getBoolean(KEY_WEEKEND_ENABLED, false)) {
            val minutes = sp.getInt(KEY_WEEKEND_HOUR, 9) * 60 + sp.getInt(KEY_WEEKEND_MINUTE, 0)
            editor.putBoolean(KEY_PER_DAY_ENABLED, true)
            editor.putString(KEY_DAY_TIMES, "1=$minutes,7=$minutes")
        }
        editor.remove(KEY_WEEKEND_ENABLED)
            .remove(KEY_WEEKEND_HOUR)
            .remove(KEY_WEEKEND_MINUTE)
            .putBoolean(KEY_PER_DAY_MIGRATED, true)
            // commit: vekjarinn les þetta strax á eftir í sama ferli,
            // og tapist skrifin fer helgartíminn forgörðum.
            .commit()
    }

    /**
     * Rofinn „Mismunandi tími eftir dögum" var lagður niður í v0.972.
     * Hann FALDI tíma dagsins, eyddi honum ekki: sá sem stillti laugardag
     * 09:30 og slökkti svo á rofanum á 07:00 á laugardag. Yrði kortið
     * virkt án hreinsunar myndi vekjarinn færast — þegjandi.
     *
     * Þetta er ekki fræðilegt: migrateWeekendTime() skrifar bæði kortið OG
     * kveikir á rofanum, svo hver sem slökkti á honum eftir þá færslu á enn
     * laugardag og sunnudag í kortinu.
     *
     * Keyrir einu sinni, á eftir migrateWeekendTime() og á undan
     * AlarmScheduler.schedule().
     */
    fun migrateRetiredPerDaySwitch() {
        if (sp.getBoolean(KEY_DAY_TIMES_SWITCH_REMOVED, false)) return

        val kept = retiredPerDayTimes(
            enabled = sp.getBoolean(KEY_PER_DAY_ENABLED, false),
            raw = sp.getString(KEY_DAY_TIMES, null)
        )
        sp.edit()
            .putString(KEY_DAY_TIMES, kept)
            .putBoolean(KEY_PER_DAY_ENABLED, !kept.isNullOrEmpty())
            .putBoolean(KEY_DAY_TIMES_SWITCH_REMOVED, true)
            // commit af somu astaedu og i migrateWeekendTime: vekjarinn
            // les tetta strax a eftir i sama ferli.
            .commit()
    }

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

    /**
     * Hversu lengi blundur varir, i minutum. Notandinn velur sjalfur
     * innan SNOOZE_RANGE - coerceIn her er einungis vorn ef vistad gildi
     * skemmist, svo enginn fai blund upp a null minutur.
     */
    var snoozeMinutes: Int
        get() = sp.getInt(KEY_SNOOZE, 9).coerceIn(SNOOZE_RANGE)
        set(value) = sp.edit().putInt(KEY_SNOOZE, value.coerceIn(SNOOZE_RANGE)).apply()

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

    /**
     * Ein hringing sem á að sleppa, í millisekúndum.
     * 0 = ekkert sleppt. Vistað sem nákvæmlega sá triggerAt sem
     * TriggerTimes.next skilaði þegar notandinn ýtti á takkann —
     * svo tímabreyting eftir á ógildi sleppinguna náttúrulega.
     */
    var skipNextMillis: Long
        get() = sp.getLong(KEY_SKIP_NEXT, 0L)
        set(value) = sp.edit().putLong(KEY_SKIP_NEXT, value).apply()

    /**
     * Varaleið þegar bæn er búin eða vantar: false = kirkjuklukka,
     * true = Rás 1 í beinni. Brjóti streymið tekur klukkan við.
     */
    var fallbackRas1: Boolean
        get() = sp.getBoolean(KEY_FALLBACK_RAS1, false)
        set(value) = sp.edit().putBoolean(KEY_FALLBACK_RAS1, value).apply()

    /**
     * Vekjarahljóð sem notandinn valdi, afritað inn í geymslu appsins
     * (sjá AlarmSoundStore). null = kirkjuklukka Staðarfells.
     */
    var alarmSoundPath: String?
        get() = sp.getString(KEY_ALARM_SOUND_PATH, null)
        set(value) = sp.edit().putString(KEY_ALARM_SOUND_PATH, value).apply()

    /** Heiti hljóðsins eins og það birtist notandanum. */
    var alarmSoundTitle: String?
        get() = sp.getString(KEY_ALARM_SOUND_TITLE, null)
        set(value) = sp.edit().putString(KEY_ALARM_SOUND_TITLE, value).apply()

    /**
     * Við hvað vaknar notandinn?
     * WAKE_PRAYER: bænin er vekjarinn sjálfur (sjálfgefið, eins og alltaf).
     * WAKE_SOUND: vekjarahljóð fyrst — bænin á eftir, þegar hann er vaknaður
     * og getur í raun hlustað á hana.
     */
    var wakeMode: String
        get() = sp.getString(KEY_WAKE_MODE, WAKE_PRAYER) ?: WAKE_PRAYER
        set(value) = sp.edit().putString(KEY_WAKE_MODE, value).apply()

    val wakeWithSound: Boolean
        get() = wakeMode == WAKE_SOUND

    /**
     * Hvað gerist eftir að slökkt er á vekjarahljóðinu (aðeins í WAKE_SOUND):
     * AFTER_AUTO: bænin byrjar strax.
     * AFTER_ASK: skjárinn spyr hvort eigi að hlusta.
     * AFTER_LATER: tilkynning sem spilar bænina þegar hentar.
     */
    var afterWake: String
        get() = sp.getString(KEY_AFTER_WAKE, AFTER_AUTO) ?: AFTER_AUTO
        set(value) = sp.edit().putString(KEY_AFTER_WAKE, value).apply()

    /**
     * Ljost eda dokkt. THEME_SYSTEM fylgir stillingu simans, hitt tvennt
     * gengur gegn henni - sumir vilja dokkt app snemma morguns thott
     * siminn sjalfur se ljos.
     */
    var themeMode: String
        get() = sp.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = sp.edit().putString(KEY_THEME_MODE, value).apply()

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
        private const val KEY_OEM_GUIDE = "unused_apps_guide_done"
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
        private const val KEY_PER_DAY_ENABLED = "per_day_enabled"
        private const val KEY_DAY_TIMES = "day_times"
        private const val KEY_PER_DAY_MIGRATED = "per_day_migrated"
        private const val KEY_SKIP_NEXT = "skip_next_millis"
        private const val KEY_FALLBACK_RAS1 = "fallback_ras1"
        private const val KEY_ALARM_SOUND_PATH = "alarm_sound_path"
        private const val KEY_ALARM_SOUND_TITLE = "alarm_sound_title"
        private const val KEY_WAKE_MODE = "wake_mode"
        private const val KEY_AFTER_WAKE = "after_wake"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DAY_TIMES_SWITCH_REMOVED = "day_times_switch_removed"

        const val WAKE_PRAYER = "prayer"
        const val WAKE_SOUND = "sound"
        const val AFTER_AUTO = "auto"
        const val AFTER_ASK = "ask"
        const val AFTER_LATER = "later"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        /** Efri mork eru handahofskennd, en klukkutimi er ekki blundur. */
        val SNOOZE_RANGE = 1..60

        const val PREFS_NAME = "morgunbaen"

        // Manudagur (2) til fostudags (6) - venja, ekki takmorkun
        private val DEFAULT_DAYS = setOf("2", "3", "4", "5", "6")

        /**
         * Hvað verður eftir af tímakortinu þegar rofinn er lagður niður.
         * Slökktur rofi þýddi „þessir tímar gilda ekki", svo þeir mega
         * ekki lifa af breytinguna. Hreint fall svo það sé prófanlegt án
         * Context — sama mynstur og parseDayTimes.
         */
        internal fun retiredPerDayTimes(enabled: Boolean, raw: String?): String? =
            if (enabled) raw else null

        /**
         * Kortið eftir að notandinn valdi tíma fyrir einn dag.
         *
         * Jafngildi sjálfgefna tímans EYÐIR lyklinum í stað þess að skrá
         * hann. Þá þarf engan „Sjálfgefið"-hnapp: dagurinn verður daufur
         * aftur um leið og hann er stilltur á sömu tölu og hinir. Dagur
         * sem fylgir sjálfgefnu fylgir því líka þegar því er breytt síðar.
         */
        internal fun applyPickedTime(
            defaultMinutes: Int,
            dayTimes: Map<Int, Int>,
            day: Int,
            pickedMinutes: Int
        ): Map<Int, Int> =
            if (pickedMinutes == defaultMinutes) dayTimes - day
            else dayTimes + (day to pickedMinutes)

        /** Ógild færsla er hunsuð frekar en að fella vekjarann. */
        internal fun parseDayTimes(raw: String?): Map<Int, Int> =
            raw.orEmpty().split(",").mapNotNull { entry ->
                val (day, minutes) = entry.split("=").takeIf { it.size == 2 }
                    ?: return@mapNotNull null
                val d = day.toIntOrNull() ?: return@mapNotNull null
                val m = minutes.toIntOrNull() ?: return@mapNotNull null
                if (d in 1..7 && m in 0 until 24 * 60) d to m else null
            }.toMap()
    }
}
