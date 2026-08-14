package com.morgunbaen.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.morgunbaen.app.alarm.AlarmScheduler
import com.morgunbaen.app.data.Dates
import com.morgunbaen.app.data.Episode
import com.morgunbaen.app.data.EpisodeRepository
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.data.RuvClient
import com.morgunbaen.app.ui.AlarmCard
import com.morgunbaen.app.ui.InfoCard
import com.morgunbaen.app.ui.MorgunbaenTheme
import com.morgunbaen.app.ui.PrayerCard
import com.morgunbaen.app.ui.WakeSettingsCard
import com.morgunbaen.app.ui.WarningCard
import com.morgunbaen.app.work.CatchUpScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorgunbaenTheme {
                MainScreen()
            }
        }
    }
}

/**
 * Samhaefingarlagid: allt state, allir atburdir, oll hlidarverk.
 * Utlitid sjalft byr i ui/AlarmCard, ui/PrayerCard og ui/WakeSettingsCard
 * - tessi skra var komin yfir 1000 linur adur en hun var klofin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(prefs.alarmEnabled) }
    var hour by remember { mutableIntStateOf(prefs.alarmHour) }
    var minute by remember { mutableIntStateOf(prefs.alarmMinute) }
    var days by remember { mutableStateOf(prefs.alarmDays) }
    var status by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var cachedTitle by remember { mutableStateOf(prefs.cachedTitle) }
    var cachedDate by remember { mutableStateOf(prefs.cachedFirstrun) }

    // Kerfisstillingar geta breyst medan appid er opid - t.d. tegar notandinn
    // fer i stillingar og kemur til baka. Tess vegna eru taer i state og
    // endurmetnar i hvert sinn sem skjarinn kemur i forgrunn.
    var exactAlarmOk by remember { mutableStateOf(AlarmScheduler.canScheduleExact(context)) }
    var batteryOk by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var notificationsOk by remember { mutableStateOf(areNotificationsEnabled(context)) }
    var fullScreenOk by remember { mutableStateOf(canUseFullScreenIntent(context)) }
    var nextAlarmText by remember { mutableStateOf(nextAlarmDescription(prefs)) }
    var health by remember { mutableStateOf(checkHealth(prefs)) }
    var oemGuideDone by remember { mutableStateOf(prefs.oemGuideDone) }
    var fadeIn by remember { mutableStateOf(prefs.fadeInEnabled) }
    var fadeSeconds by remember { mutableIntStateOf(prefs.fadeInSeconds) }
    var vibrate by remember { mutableStateOf(prefs.vibrateEnabled) }
    var snoozeMinutes by remember { mutableIntStateOf(prefs.snoozeMinutes) }
    var weekendEnabled by remember { mutableStateOf(prefs.weekendTimeEnabled) }
    var weekendHour by remember { mutableIntStateOf(prefs.weekendHour) }
    var weekendMinute by remember { mutableIntStateOf(prefs.weekendMinute) }
    var cachedEpisodeId by remember { mutableStateOf(prefs.cachedEpisodeId) }
    var newsEnabled by remember { mutableStateOf(prefs.newsEnabled) }
    var newsFirstrun by remember { mutableStateOf(prefs.newsFirstrun) }
    var newsSyncing by remember { mutableStateOf(false) }
    // null = ekki reynt enn i tessari lotu. Adgreinir "vitum ekki" fra
    // "reyndum og fundum ekki" - annars segir appid ranglega ad frettatimi
    // se ekki kominn ut tegar tad hefur einfaldlega ekki leitad.
    var newsAttempted by remember { mutableStateOf(prefs.newsFirstrun != null) }
    var testArmed by remember { mutableStateOf(false) }
    var playingToday by remember { mutableStateOf(false) }

    // Lettur spilari fyrir "Spila baenina" - hegdar ser eins og venjulegur
    // midill (USAGE_MEDIA + sjalfvirkur hljodfokus), OLIKT vekjaranum.
    val previewPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .build()
            .apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == androidx.media3.common.Player.STATE_ENDED) {
                            playingToday = false
                        }
                    }
                })
            }
    }
    DisposableEffect(Unit) {
        onDispose { previewPlayer.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    exactAlarmOk = AlarmScheduler.canScheduleExact(context)
                    batteryOk = isIgnoringBatteryOptimizations(context)
                    notificationsOk = areNotificationsEnabled(context)
                    fullScreenOk = canUseFullScreenIntent(context)
                    nextAlarmText = nextAlarmDescription(prefs)
                    cachedTitle = prefs.cachedTitle
                    cachedDate = prefs.cachedFirstrun
                    cachedEpisodeId = prefs.cachedEpisodeId
                    newsFirstrun = prefs.newsFirstrun
                    health = checkHealth(prefs)
                    // Profunartextinn a ekki ad lifa profid sjalft - komi
                    // notandinn til baka eftir hringinguna er "Hringir eftir
                    // 30 sekundur" ordid osatt.
                    testArmed = false
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Spilarinn fylgir skjanum. An tessa helt baenin afram
                    // ad spila eftir heim-takkann - an tilkynningar, an
                    // tjonustu og an nokkurs synilegs stodvunartakka.
                    if (playingToday) {
                        previewPlayer.pause()
                        playingToday = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Bidjum um tilkynningaheimild strax - an hennar birtist vekjarinn ekki.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun persistAndReschedule() {
        prefs.alarmEnabled = enabled
        prefs.alarmHour = hour
        prefs.alarmMinute = minute
        prefs.alarmDays = days
        AlarmScheduler.schedule(context)
        // Glugginn les vekjaradaga og frettastillingu - breytist annad hvort
        // tarf hann nyjan tima. Ohaett ad kalla oft.
        CatchUpScheduler.schedule(context)
        nextAlarmText = nextAlarmDescription(prefs)
        health = checkHealth(prefs)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            AlarmCard(
                hour = hour,
                minute = minute,
                enabled = enabled,
                days = days,
                weekendEnabled = weekendEnabled,
                weekendHour = weekendHour,
                weekendMinute = weekendMinute,
                weekendDaysMissing = weekendEnabled &&
                    days.none { it == Calendar.SATURDAY || it == Calendar.SUNDAY },
                nextAlarmText = nextAlarmText,
                testArmed = testArmed,
                testSeconds = TEST_ALARM_SECONDS,
                onEnabledChange = {
                    enabled = it
                    persistAndReschedule()
                },
                onPickTime = {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            hour = h
                            minute = m
                            persistAndReschedule()
                        },
                        hour, minute, true
                    ).show()
                },
                onDaysChange = {
                    days = it
                    persistAndReschedule()
                },
                onWeekendEnabledChange = {
                    weekendEnabled = it
                    prefs.weekendTimeEnabled = it
                    persistAndReschedule()
                },
                onPickWeekendTime = {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            weekendHour = h
                            weekendMinute = m
                            prefs.weekendHour = h
                            prefs.weekendMinute = m
                            persistAndReschedule()
                        },
                        weekendHour, weekendMinute, true
                    ).show()
                },
                onTest = {
                    AlarmScheduler.scheduleTest(context, TEST_ALARM_SECONDS)
                    testArmed = true
                }
            )

            Spacer(Modifier.height(16.dp))

            PrayerCard(
                title = cachedTitle,
                dateText = cachedDate?.let { "Flutt " + Dates.formatShort(it) },
                status = status,
                syncing = syncing,
                playingToday = playingToday,
                canShare = cachedEpisodeId != null &&
                    cachedTitle != null && cachedDate != null,
                onFetch = {
                    syncing = true
                    status = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val repo = EpisodeRepository(context)
                            val r = repo.sync()
                            // Frettirnar fylgja med - adur sotti tessi takki
                            // adeins baenina.
                            if (prefs.newsEnabled) repo.syncNews()
                            r
                        }
                        newsAttempted = prefs.newsEnabled
                        syncing = false
                        cachedTitle = prefs.cachedTitle
                        cachedDate = prefs.cachedFirstrun
                        cachedEpisodeId = prefs.cachedEpisodeId
                        newsFirstrun = prefs.newsFirstrun
                        status = when (result) {
                            is EpisodeRepository.SyncResult.Downloaded ->
                                context.getString(R.string.sync_downloaded)
                            is EpisodeRepository.SyncResult.AlreadyHave ->
                                context.getString(R.string.sync_already_have)
                            is EpisodeRepository.SyncResult.StreamOnly ->
                                context.getString(R.string.sync_stream_only)
                            is EpisodeRepository.SyncResult.Failed ->
                                context.getString(R.string.sync_failed, result.reason)
                        }
                    }
                },
                onPlayToggle = {
                    if (playingToday) {
                        previewPlayer.pause()
                        playingToday = false
                    } else {
                        val src = EpisodeRepository(context).playbackSource()
                        val uri = when (src) {
                            is EpisodeRepository.PlaybackSource.LocalFile ->
                                Uri.fromFile(src.file)
                            is EpisodeRepository.PlaybackSource.Stream ->
                                Uri.parse(src.url)
                            null -> null
                        }
                        if (uri != null) {
                            previewPlayer.setMediaItem(MediaItem.fromUri(uri))
                            previewPlayer.prepare()
                            previewPlayer.play()
                            playingToday = true
                        }
                    }
                },
                onOpenHistory = { HistoryActivity.start(context) },
                onShare = {
                    // canShare tryggir ad tetta se allt til - en state getur
                    // breyst milli endurteikninga, svo vid latum null falla
                    // hljodlaust frekar en ad hrynja.
                    val shareId = cachedEpisodeId ?: return@PrayerCard
                    val shareTitle = cachedTitle ?: return@PrayerCard
                    val shareDate = cachedDate ?: return@PrayerCard
                    shareEpisode(
                        context,
                        Episode(
                            id = shareId,
                            title = shareTitle,
                            firstrun = shareDate,
                            fileUrl = ""
                        )
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            WakeSettingsCard(
                fadeIn = fadeIn,
                fadeSeconds = fadeSeconds,
                vibrate = vibrate,
                newsEnabled = newsEnabled,
                newsDescription = newsDescription(
                    newsEnabled = newsEnabled,
                    newsSyncing = newsSyncing,
                    newsAttempted = newsAttempted,
                    newsFirstrun = newsFirstrun,
                    alarmHour = hour
                ),
                snoozeMinutes = snoozeMinutes,
                onFadeInChange = {
                    fadeIn = it
                    prefs.fadeInEnabled = it
                },
                onFadeSecondsChange = {
                    fadeSeconds = it
                    prefs.fadeInSeconds = it
                },
                onVibrateChange = {
                    vibrate = it
                    prefs.vibrateEnabled = it
                },
                onNewsChange = {
                    newsEnabled = it
                    prefs.newsEnabled = it
                    CatchUpScheduler.schedule(context)

                    // Saekja strax tegar kveikt er - annars bidur notandinn
                    // i allt ad sex klst eftir ad sja hvort tetta virki.
                    if (it) {
                        newsSyncing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                EpisodeRepository(context).syncNews()
                            }
                            newsSyncing = false
                            newsAttempted = true
                            newsFirstrun = prefs.newsFirstrun
                        }
                    }
                },
                onSnoozeChange = {
                    snoozeMinutes = it
                    prefs.snoozeMinutes = it
                }
            )

            Spacer(Modifier.height(16.dp))

            // ---------- Heilsuvoktun ----------
            // Rautt = eitthvad hefur tegar farid urskeidis.
            // Tetta er mikilvaegara en stillingavidvaranirnar tvi tad er
            // eina merkid um bilun sem notandinn faer - siminn segir ekkert.
            if (Health.MISSED_ALARM in health) {
                WarningCard(
                    text = stringResource(R.string.warn_missed_alarm),
                    actionLabel = stringResource(R.string.acknowledge),
                    onAction = {
                        prefs.missedAlarmAcknowledged = System.currentTimeMillis()

                        // Thida merkid. schedule() frystir tad medan
                        // lidinn ohringdur timi stendur - an tessa saeti
                        // tad fast i FYRSTA klikkinu og vidvorunin
                        // birtist aldrei aftur, tott vekjarinn thegdi
                        // hvern einasta morgun eftir tad.
                        prefs.lastScheduledTriggerMillis =
                            AlarmScheduler.nextTriggerTime(prefs) ?: 0L

                        health = checkHealth(prefs)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
            if (Health.STALE_SYNC in health) {
                WarningCard(
                    text = stringResource(R.string.warn_stale_sync),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openBatterySettings(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            // ---------- Aminningar um kerfisstillingar ----------
            // Tessar tvaer eru fremstar tvi an teirra er EKKERT sem notandinn
            // getur ytt a til ad slokkva a vekjaranum.
            if (!notificationsOk) {
                WarningCard(
                    text = stringResource(R.string.warn_notifications),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openNotificationSettings(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!fullScreenOk) {
                WarningCard(
                    text = stringResource(R.string.warn_fullscreen),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openFullScreenIntentSettings(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!exactAlarmOk) {
                WarningCard(
                    text = stringResource(R.string.warn_exact_alarm),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openExactAlarmSettings(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!batteryOk) {
                WarningCard(
                    text = stringResource(R.string.warn_battery),
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openBatterySettings(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            // ---------- Samsung ----------
            // Samsung svaefir opp sem hafa ekki verid opnud i trja daga.
            // Hringi vekjarinn adeins a virkum dogum er appid ONOTAD yfir
            // helgi. Ekkert API laetur vita og ekkert API slekkur a tessu;
            // notandinn verdur ad gera tad sjalfur.
            if (isSamsung() && !oemGuideDone) {
                InfoCard(
                    title = stringResource(R.string.samsung_title),
                    text = stringResource(R.string.samsung_body),
                    primaryLabel = stringResource(R.string.open_settings),
                    onPrimary = { openDeviceCare(context) },
                    secondaryLabel = stringResource(R.string.samsung_done),
                    onSecondary = {
                        prefs.oemGuideDone = true
                        oemGuideDone = true
                    }
                )
            }
        }
    }
}

/**
 * Lysingartextinn undir frettarofanum. Fimm astond i forgangsrod;
 * athugasemdirnar um "hour" gegn currentHour() eru blodi skrifadar -
 * su villa for trjar umferdir milli lagfaeringa.
 */
@Composable
private fun newsDescription(
    newsEnabled: Boolean,
    newsSyncing: Boolean,
    newsAttempted: Boolean,
    newsFirstrun: String?,
    alarmHour: Int
): String = when {
    !newsEnabled -> stringResource(R.string.news_desc_off)
    newsSyncing -> stringResource(R.string.news_fetching)

    // TVO OLIK ASTOND - ekki rugla teim saman.
    //
    // 1) VEKJARATIMINN (alarmHour) er fyrir kl. 07:00. Tha eru frettirnar
    //    aldrei til tegar hringt er, sama hvada dag. Varanlegt astand sem
    //    notandinn getur adeins leyst med tvi ad faera vekjarann.
    alarmHour < RuvClient.FRETTIR_HOUR ->
        stringResource(R.string.news_alarm_too_early)

    // 2) KLUKKAN er undir 07:00 akkurat nu og frettatimi dagsins er tvi
    //    ekki kominn ut enn. Skammvinnt astand sem leysist af sjalfu ser.
    currentHour() < RuvClient.FRETTIR_HOUR && !Dates.isToday(newsFirstrun) ->
        stringResource(R.string.news_not_yet)

    Dates.isToday(newsFirstrun) -> stringResource(
        R.string.news_ready,
        Dates.timePart(newsFirstrun!!)
    )
    !newsAttempted -> stringResource(R.string.news_none)
    else -> stringResource(R.string.news_missing)
}

/** Einkenni sem heilsuvoktunin fann. Tomt mengi = allt i lagi. */
private enum class Health { MISSED_ALARM, STALE_SYNC }

/**
 * Athugar hvort eitthvad hafi thegar farid urskeidis.
 *
 * Tvo einkenni benda til tess ad siminn se ad stodva appid:
 *
 *  1. Skráður hringitími sem er liðinn án þess að vekjarinn hafi hringt.
 *  2. Bakgrunnssokn sem hefur ekki naad ad keyra i meira en 36 klst.
 *
 * Hvorugt greinist sjalfkrafa af Android - appid verdur ad taka eftir tvi sjalft.
 *
 * Við notum lastScheduledTriggerMillis, ekki previousTriggerTime. Sá síðari
 * reiknast upp á nýtt út frá núverandi stillingum — breyti notandinn 07:00
 * í 06:30 eftir velheppnaða hringingu lítur það út eins og klikkaður vekjari.
 */
private fun checkHealth(prefs: Prefs): Set<Health> {
    if (!prefs.alarmEnabled) return emptySet()

    // Mengi, ekki stakt gildi: klikkadur vekjari og stodnud sokn koma
    // oftast SAMAN - soknin sem la nidri er orsok klikksins - og notandinn
    // a ad sja badar hlidar strax, ekki adra eftir ad hann kvittar.
    val result = mutableSetOf<Health>()
    val now = System.currentTimeMillis()

    // Vid segjum ekkert fyrr en vekjarinn hefur hringt ad minnsta kosti einu
    // sinni - annars fengi hver nyr notandi vidvorun a fyrsta degi.
    if (prefs.lastAlarmFiredMillis > 0L) {
        val expected = prefs.lastScheduledTriggerMillis
        if (expected > 0L &&
            expected < now - TimeUnit.MINUTES.toMillis(5) &&
            prefs.lastAlarmFiredMillis + TimeUnit.MINUTES.toMillis(5) < expected &&
            expected > prefs.missedAlarmAcknowledged
        ) {
            result += Health.MISSED_ALARM
        }
    }

    if (prefs.lastSyncMillis > 0L &&
        now - prefs.lastSyncMillis > TimeUnit.HOURS.toMillis(36)
    ) {
        result += Health.STALE_SYNC
    }

    return result
}

private fun areNotificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/**
 * Fra Android 14 er full-screen intent ekki lengur sjalfgefid leyfd.
 * Google veitir hana adeins oppum sem Play Store hefur flokkad sem vekjara-
 * eda simtalsopp - hlidarhladin APK-skra faer hana EKKI.
 *
 * An hennar spilar hljodid en enginn skjar birtist a laesta skjanum.
 */
private fun canUseFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    return context.getSystemService(android.app.NotificationManager::class.java)
        .canUseFullScreenIntent()
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    try {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    } catch (e: Exception) {
        openNotificationSettings(context)
    }
}

private fun isSamsung(): Boolean =
    Build.MANUFACTURER.equals("samsung", ignoreCase = true)

/**
 * Reynir ad opna Device Care hja Samsung, tar sem "Svefnopp" listinn byr.
 * Samsung gefur enga opinbera leid ad tessum skja, svo tetta getur brugdist
 * - tha opnum vid venjulegu app-stillingarnar i stadinn.
 */
private fun openDeviceCare(context: Context) {
    val deviceCare = Intent().apply {
        setClassName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        )
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(deviceCare)
    } catch (e: Exception) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}

/** Hversu langt profunarhringingin er fram i timann. */
private const val TEST_ALARM_SECONDS = 30

/** Klukkan a veggnum nuna - EKKI stilltur vekjaratimi. */
private fun currentHour(): Int =
    Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

private fun nextAlarmDescription(prefs: Prefs): String {
    val snoozeAt = prefs.snoozeUntilMillis
    if (snoozeAt > System.currentTimeMillis()) {
        val format = SimpleDateFormat("HH:mm", Locale("is", "IS"))
        return "Blundar til " + format.format(Date(snoozeAt))
    }
    val next = AlarmScheduler.nextTriggerTime(prefs) ?: return "Enginn dagur valinn"
    val format = SimpleDateFormat("EEEE d. MMMM 'kl.' HH:mm", Locale("is", "IS"))
    return "Næst: " + format.format(Date(next))
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(android.os.PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

@Suppress("BatteryLife")
private fun openBatterySettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (e2: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
}
