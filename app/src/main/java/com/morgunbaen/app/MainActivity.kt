package com.morgunbaen.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.morgunbaen.app.alarm.AlarmService
import com.morgunbaen.app.alarm.TriggerTimes
import com.morgunbaen.app.data.AlarmSoundStore
import com.morgunbaen.app.data.Dates
import com.morgunbaen.app.data.Episode
import com.morgunbaen.app.data.EpisodeRepository
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.data.RuvClient
import com.morgunbaen.app.ui.AlarmCard
import com.morgunbaen.app.ui.AppTheme
import com.morgunbaen.app.ui.AppearanceCard
import com.morgunbaen.app.ui.ClockMode
import com.morgunbaen.app.ui.InfoCard
import com.morgunbaen.app.ui.MorgunbaenTheme
import com.morgunbaen.app.ui.PrayerCard
import com.morgunbaen.app.ui.SoundCard
import com.morgunbaen.app.ui.TimePickDialog
import com.morgunbaen.app.ui.WEEK_ORDER
import com.morgunbaen.app.ui.WakeSettingsCard
import com.morgunbaen.app.ui.WarningCard
import com.morgunbaen.app.work.CatchUpScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        // Skjarinn nær undir stodu- og flettistiku. Scaffold skilar
        // innskotunum sjalft, og MorgunbaenTheme raedur birtu taknanna.
        enableEdgeToEdge()
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
    var nextAlarmText by remember { mutableStateOf(nextAlarmDescription(context, prefs)) }
    var countdownText by remember { mutableStateOf(countdownDescription(context, prefs)) }
    var health by remember { mutableStateOf(checkHealth(prefs)) }
    var oemGuideDone by remember { mutableStateOf(prefs.oemGuideDone) }
    var fadeIn by remember { mutableStateOf(prefs.fadeInEnabled) }
    var fadeSeconds by remember { mutableIntStateOf(prefs.fadeInSeconds) }
    var vibrate by remember { mutableStateOf(prefs.vibrateEnabled) }
    var snoozeMinutes by remember { mutableIntStateOf(prefs.snoozeMinutes) }
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var dayTimes by remember { mutableStateOf(prefs.dayTimes) }
    var cachedEpisodeId by remember { mutableStateOf(prefs.cachedEpisodeId) }
    var fallbackRas1 by remember { mutableStateOf(prefs.fallbackRas1) }
    var wakeMode by remember { mutableStateOf(prefs.wakeMode) }
    var afterWake by remember { mutableStateOf(prefs.afterWake) }
    // null = kirkjuklukkan. Lesid ur AlarmSoundStore, ekki beint ur prefs:
    // hvarf skrain er heitid lygi.
    var alarmSoundTitle by remember {
        mutableStateOf(AlarmSoundStore(context).file()?.let { prefs.alarmSoundTitle })
    }
    var soundImporting by remember { mutableStateOf(false) }
    var newsEnabled by remember { mutableStateOf(prefs.newsEnabled) }
    var newsFirstrun by remember { mutableStateOf(prefs.newsFirstrun) }
    var newsSyncing by remember { mutableStateOf(false) }
    // null = ekki reynt enn i tessari lotu. Adgreinir "vitum ekki" fra
    // "reyndum og fundum ekki" - annars segir appid ranglega ad frettatimi
    // se ekki kominn ut tegar tad hefur einfaldlega ekki leitad.
    var newsAttempted by remember { mutableStateOf(prefs.newsFirstrun != null) }
    var testArmed by remember { mutableStateOf(false) }
    var playingToday by remember { mutableStateOf(false) }
    var skipActive by remember {
        mutableStateOf(prefs.skipNextMillis > System.currentTimeMillis())
    }
    var skippedWhenText by remember {
        mutableStateOf(formatSkipTime(prefs.skipNextMillis))
    }

    // Stora talan er LEIDD af prefs, ekki af hour/minute: hun svarar
    // "hvenaer hringir hann naest?" og tarf tvi ad tikka. Tremur stodum -
    // minutumot, ON_RESUME og persistAndReschedule - er haldid i takt af
    // refreshAlarmView() her ad nedan.
    var clockMode by remember { mutableStateOf(ClockMode.DEFAULT) }
    var displayHour by remember { mutableIntStateOf(prefs.alarmHour) }
    var displayMinute by remember { mutableIntStateOf(prefs.alarmMinute) }
    var nextDay by remember { mutableStateOf<Int?>(null) }
    // Dagurinn i dag ma ekki frjosa a midnaetti - tikkid endurreiknar hann.
    var today by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) }

    // Hvad klukkuglugginn er ad stilla, eda null tegar hann er lokadur.
    var picking by remember { mutableStateOf<Picking?>(null) }

    // Sama ferli, svo StateFlow dugar - eins og listeningState.
    val ringingNow by AlarmService.ringingState.collectAsState()

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

    /**
     * Allt sem raest af KLUKKUNNI A VEGGNUM frekar en af innslaetti
     * notandans. Adur voru tessar linur afritadar a tremur stodum og
     * tvaer teirra gleymdu stoku gildi; nu er einn stadur ad gleyma i.
     */
    fun refreshAlarmView() {
        today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        nextAlarmText = nextAlarmDescription(context, prefs)
        countdownText = countdownDescription(context, prefs)
        skipActive = prefs.skipNextMillis > System.currentTimeMillis()
        skippedWhenText = formatSkipTime(prefs.skipNextMillis)

        val snoozeAt = prefs.snoozeUntilMillis
        val target = alarmTargetMillis(prefs)
        when {
            // Slokktur vekjari a enga naestu hringingu. Ta synir talan
            // sjalfgefna timann og er stillanleg sem slik - annars hverfur
            // adalstjorntaeki spjaldsins um leid og rofinn fer af.
            !prefs.alarmEnabled || target == null -> {
                clockMode = ClockMode.DEFAULT
                displayHour = prefs.alarmHour
                displayMinute = prefs.alarmMinute
                nextDay = null
            }
            target == snoozeAt -> {
                val cal = Calendar.getInstance().apply { timeInMillis = target }
                clockMode = ClockMode.SNOOZE
                displayHour = cal.get(Calendar.HOUR_OF_DAY)
                displayMinute = cal.get(Calendar.MINUTE)
                nextDay = null
            }
            else -> {
                val cal = Calendar.getInstance().apply { timeInMillis = target }
                clockMode = ClockMode.NEXT_RING
                displayHour = cal.get(Calendar.HOUR_OF_DAY)
                displayMinute = cal.get(Calendar.MINUTE)
                nextDay = cal.get(Calendar.DAY_OF_WEEK)
            }
        }
    }

    // Fyrsta reikningin: state ofan vid var sett a sjalfgefna timann svo
    // eitthvad se til adur en tetta keyrir.
    LaunchedEffect(Unit) { refreshAlarmView() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    exactAlarmOk = AlarmScheduler.canScheduleExact(context)
                    batteryOk = isIgnoringBatteryOptimizations(context)
                    notificationsOk = areNotificationsEnabled(context)
                    fullScreenOk = canUseFullScreenIntent(context)
                    // Vekjarinn getur hafa hringt - eda verid blundad -
                    // medan appid var i bakgrunni.
                    refreshAlarmView()
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

    // Afritar valid hljod inn i appid (sja AlarmSoundStore) og segir fra
    // ef tad gekk ekki - fyrra hljodid helst ta obreytt.
    fun importAlarmSound(uri: Uri, title: String) {
        soundImporting = true
        scope.launch {
            val result = AlarmSoundStore(context).importFrom(uri, title)
            soundImporting = false
            val message = when (result) {
                AlarmSoundStore.ImportResult.Ok -> {
                    alarmSoundTitle = title
                    context.getString(R.string.alarm_sound_set, title)
                }
                AlarmSoundStore.ImportResult.TooLarge ->
                    context.getString(R.string.alarm_sound_too_large)
                AlarmSoundStore.ImportResult.NotAudio ->
                    context.getString(R.string.alarm_sound_not_audio)
                is AlarmSoundStore.ImportResult.Failed ->
                    context.getString(R.string.alarm_sound_failed, result.reason)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
        } ?: return@rememberLauncherForActivityResult
        val title = try {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        } catch (e: Exception) {
            null
        } ?: context.getString(R.string.alarm_sound_system)
        importAlarmSound(uri, title)
    }

    val soundFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) importAlarmSound(uri, displayName(context, uri))
    }

    // Bidjum um tilkynningaheimild strax - an hennar birtist vekjarinn ekki.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Teljarinn verdur ad tikka af sjalfum ser - annars stendur hann i stad
    // medan skjarinn er opinn og lygur meira eftir tvi sem lengur lidur.
    // Vaknar a minutumotum svo talan breytist a somu stundu og klukkan i
    // simanum gerir tad, ekki einhvers stadar inni i minutunni.
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE_MILLIS - System.currentTimeMillis() % MINUTE_MILLIS + TICK_SLACK_MILLIS)
            // Hringi vekjarinn a medan skjarinn er opinn faerist naesti
            // timi a naesta dag - stora talan, teljarinn og textinn undir
            // teim verda ad snuast vid a sama augnabliki.
            refreshAlarmView()
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
        refreshAlarmView()
        health = checkHealth(prefs)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            // Hringi vekjarinn en skjarinn hafi ekki komist upp - annad
            // vekjaraforrit vard a undan - er tetta eina leidin sem getur
            // ekki brugdist: appid er i forgrunni og ma allt sem tarf.
            // Synilegt AÐEINS medan hann hringir.
            if (ringingNow) {
                InfoCard(
                    title = stringResource(R.string.ringing_now_title),
                    text = stringResource(R.string.ringing_now_text),
                    primaryLabel = stringResource(R.string.dismiss),
                    onPrimary = { AlarmService.dismissFromApp(context) },
                    secondaryLabel = stringResource(R.string.snooze),
                    onSecondary = { AlarmService.snooze(context) }
                )
                Spacer(Modifier.height(16.dp))
            }

            // Ein lina undir storu tolunni: dagurinn og bidtiminn saman.
            // Vikudagurinn einn tegar ekkert er ad telja nidur, bidtiminn
            // einn i blundi - tar a enginn vikudagur vid.
            val day = nextDay
            val left = countdownText
            val statusLine = when {
                day != null && left != null && enabled ->
                    stringResource(R.string.next_line, stringResource(nextDayName(day)), left)
                day != null -> stringResource(nextDayName(day))
                enabled && left != null -> stringResource(R.string.countdown_only, left)
                else -> null
            }

            AlarmCard(
                displayHour = displayHour,
                displayMinute = displayMinute,
                clockMode = clockMode,
                statusLine = statusLine,
                defaultHour = hour,
                defaultMinute = minute,
                enabled = enabled,
                days = days,
                dayTimes = dayTimes,
                nextAlarmText = nextAlarmText,
                skipActive = skipActive,
                skippedWhenText = skippedWhenText,
                testArmed = testArmed,
                testSeconds = TEST_ALARM_SECONDS,
                onEnabledChange = {
                    enabled = it
                    persistAndReschedule()
                },
                // Ýt á stóru töluna stillir ÞANN dag sem hringir næst, ekki
                // sjálfgefna tímann. Annars héldi sá sem vill sofa út á
                // laugardag að hann væri að stilla morgundaginn — og hreyfði
                // um leið alla hina dagana sem fylgja sjálfgefnu.
                onPickNext = { nextDay?.let { picking = Picking.Day(it, fromClock = true) } },
                onPickDefault = { picking = Picking.AllDays },
                today = today,
                onToggleDay = { day ->
                    days = if (day in days) days - day else days + day
                    persistAndReschedule()
                },
                onPickDayTime = { picking = Picking.Day(it) },
                onSkipNext = {
                    val toSkip = TriggerTimes.next(
                        days = days,
                        hour = hour,
                        minute = minute,
                        dayTimes = dayTimes,
                        skipMillis = 0L
                    )
                    prefs.skipNextMillis = toSkip ?: 0L
                    persistAndReschedule()
                },
                onUndoSkip = {
                    prefs.skipNextMillis = 0L
                    persistAndReschedule()
                },
                onTest = {
                    AlarmScheduler.scheduleTest(context, TEST_ALARM_SECONDS)
                    testArmed = true
                }
            )

            picking?.let { target ->
                // Vinstri flisin tarf alltaf dag. Opnist valmyndin ur
                // "Sjalfgefid" er tad dagurinn sem hringir naest.
                val chipDay = when (target) {
                    Picking.AllDays -> nextDay ?: days.minOrNull() ?: Calendar.MONDAY
                    is Picking.Day -> target.day
                }
                val current = when (target) {
                    Picking.AllDays -> hour * 60 + minute
                    is Picking.Day -> dayTimes[target.day] ?: (hour * 60 + minute)
                }
                val chipName = stringResource(WEEK_ORDER.first { it.day == chipDay }.name)
                TimePickDialog(
                    dayLabel = chipName,
                    dayTitle = when (chipDay) {
                        today -> stringResource(R.string.day_title_today, chipName)
                        today % 7 + 1 -> stringResource(R.string.day_title_tomorrow, chipName)
                        else -> chipName
                    },
                    // "Alla daga" bydst adeins ur storu tolunni.
                    showScope = days.size > 1 &&
                        (target is Picking.AllDays || (target as? Picking.Day)?.fromClock == true),
                    initialAllDays = target is Picking.AllDays,
                    initialHour = current / 60,
                    initialMinute = current % 60,
                    sleepPreview = { allDays, h, m ->
                        sleepPreview(
                            context, prefs, enabled, days, hour, minute, dayTimes,
                            if (allDays) Picking.AllDays else Picking.Day(chipDay), h, m
                        )
                    },
                    onDismiss = { picking = null },
                    onConfirm = { allDays, h, m ->
                        if (allDays) {
                            // "Alla daga" verdur ad gera tad sem hun segir:
                            // eigin timar daganna vikja, annars stodu teir
                            // eftir og flisin lygi.
                            hour = h
                            minute = m
                            dayTimes = emptyMap()
                            prefs.dayTimes = dayTimes
                        } else {
                            dayTimes = Prefs.applyPickedTime(
                                defaultMinutes = hour * 60 + minute,
                                dayTimes = dayTimes,
                                day = chipDay,
                                pickedMinutes = h * 60 + m
                            )
                            prefs.dayTimes = dayTimes
                        }
                        picking = null
                        persistAndReschedule()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            PrayerCard(
                title = cachedTitle,
                dateText = cachedDate?.let {
                    context.getString(R.string.aired_on, Dates.formatShort(it))
                },
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
                wakeMode = wakeMode,
                afterWake = afterWake,
                fadeIn = fadeIn,
                fadeSeconds = fadeSeconds,
                vibrate = vibrate,
                snoozeMinutes = snoozeMinutes,
                onWakeModeChange = {
                    wakeMode = it
                    prefs.wakeMode = it
                },
                onAfterWakeChange = {
                    afterWake = it
                    prefs.afterWake = it
                },
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
                onSnoozeChange = {
                    snoozeMinutes = it
                    prefs.snoozeMinutes = it
                }
            )

            Spacer(Modifier.height(16.dp))

            SoundCard(
                newsEnabled = newsEnabled,
                newsDescription = newsDescription(
                    newsEnabled = newsEnabled,
                    newsSyncing = newsSyncing,
                    newsAttempted = newsAttempted,
                    newsFirstrun = newsFirstrun,
                    alarmHour = hour,
                    dayTimes = dayTimes,
                    days = days
                ),
                fallbackRas1 = fallbackRas1,
                alarmSoundTitle = alarmSoundTitle,
                soundImporting = soundImporting,
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
                onFallbackRas1Change = {
                    fallbackRas1 = it
                    prefs.fallbackRas1 = it
                },
                onPickSystemSound = {
                    ringtoneLauncher.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TITLE,
                                context.getString(R.string.alarm_sound_label)
                            )
                        }
                    )
                },
                onPickSoundFile = { soundFileLauncher.launch(arrayOf("audio/*")) },
                onResetSound = {
                    AlarmSoundStore(context).clear()
                    alarmSoundTitle = null
                }
            )

            Spacer(Modifier.height(16.dp))

            // ---------- Utlit ----------
            AppearanceCard(
                themeMode = themeMode,
                onChange = {
                    themeMode = it
                    prefs.themeMode = it
                    // Allir fjorir skjairnir lesa tennan straum, svo
                    // vekjaraskjarinn skiptir lika um ham - ekki bara tessi.
                    AppTheme.mode.value = it
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

            // Android tekur heimildir af opnum sem enginn opnar i nokkra daga.
            // Vekjari a virkum dogum er onotadur yfir helgi.
            // Nedst, a eftir ollum spjoldum: enginn a ad rekast a tetta
            // a undan vekjaranum sjalfum.
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { AboutActivity.start(context) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.about_open))
            }

            if (!oemGuideDone) {
                InfoCard(
                    title = stringResource(R.string.oem_unused_title),
                    text = stringResource(R.string.oem_unused_body),
                    primaryLabel = stringResource(R.string.open_settings),
                    onPrimary = { OemBatteryGuide.open(context) },
                    secondaryLabel = stringResource(R.string.oem_done),
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
    alarmHour: Int,
    dayTimes: Map<Int, Int>,
    days: Set<Int>
): String = when {
    !newsEnabled -> stringResource(R.string.news_desc_off)
    newsSyncing -> stringResource(R.string.news_fetching)

    // TVO OLIK ASTOND - ekki rugla teim saman.
    //
    // 1) EINHVER vekjaratimi (hvada valinn dagur sem er) er fyrir kl. 07:00.
    //    Tha eru frettirnar aldrei til tegar sa dagur hringir.
    //    Aður var aðeins alarmHour skoðað — helgartími 06:30 með
    //    virkum degi kl. 08:00 sagði þá ranglega að fréttir næðust.
    alarmRingsBeforeNews(days, alarmHour, dayTimes) ->
        stringResource(R.string.news_alarm_too_early)

    // 2) KLUKKAN er undir 07:00 akkurat nu og frettatimi dagsins er tvi
    //    ekki kominn ut enn. Tad er edlilegt astand, ekki bilun, svo her
    //    lysum vid tvi sem rofinn GERIR i stad tess ad kvarta undan tvi
    //    sem vantar - "Nadi ekki i frettatima dagsins" a ekki heima a
    //    teim tima solarhringsins tegar hann er einfaldlega ekki kominn.
    currentHour() < RuvClient.FRETTIR_HOUR && !Dates.isToday(newsFirstrun) ->
        stringResource(R.string.news_desc)

    Dates.isToday(newsFirstrun) -> stringResource(
        R.string.news_ready,
        Dates.timePart(newsFirstrun!!)
    )
    !newsAttempted -> stringResource(R.string.news_none)
    else -> stringResource(R.string.news_missing)
}

/**
 * Hringir vekjarinn einhvern valinn dag fyrir fréttirnar kl. 07:00?
 * Hver dagur er skoðaður á sínum eigin tíma — snemmbúinn föstudagur má
 * ekki fela sig á bak við sjálfgefna tímann.
 */
private fun alarmRingsBeforeNews(
    days: Set<Int>,
    alarmHour: Int,
    dayTimes: Map<Int, Int>
): Boolean = days.any { day ->
    val hour = dayTimes[day]?.div(60) ?: alarmHour
    hour < RuvClient.FRETTIR_HOUR
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

/** Heiti skrar an endingar: "Hanagal.mp3" -> "Hanagal". */
private fun displayName(context: Context, uri: Uri): String {
    val name = try {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }
    return name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.alarm_sound_own_file)
}

private fun formatSkipTime(millis: Long): String? {
    if (millis <= System.currentTimeMillis()) return null
    val format = SimpleDateFormat("EEEE d. MMMM 'kl.' HH:mm", Locale("is", "IS"))
    return format.format(Date(millis))
}

/** Hvad klukkuglugginn er ad stilla i tetta skiptid. */
private sealed interface Picking {
    /** Allir dagar i einu: sjalfgefni timinn og eigin timar daganna hreinsadir. */
    object AllDays : Picking

    /**
     * Einn dagur, Calendar.MONDAY .. Calendar.SUNDAY.
     *
     * fromClock: opnad ur storu tolunni frekar en ur dalki dagsins. Adeins
     * ta bjodast "Alla daga". Dalkur dagsins er spurning um TANN dag, og
     * flis sem breytir allri vikunni a ekkert erindi tangad.
     */
    data class Day(val day: Int, val fromClock: Boolean = false) : Picking
}

/**
 * Nafn dagsins eins og tad stendur undir storu tolunni: "Laugardag".
 * Tolfall, ekki nefnifall - "Naesta hringing laugardagur" er ekki islenska.
 */
private fun nextDayName(dayOfWeek: Int): Int = when (dayOfWeek) {
    Calendar.MONDAY -> R.string.next_day_monday
    Calendar.TUESDAY -> R.string.next_day_tuesday
    Calendar.WEDNESDAY -> R.string.next_day_wednesday
    Calendar.THURSDAY -> R.string.next_day_thursday
    Calendar.FRIDAY -> R.string.next_day_friday
    Calendar.SATURDAY -> R.string.next_day_saturday
    else -> R.string.next_day_sunday
}

/**
 * Hvad tillagan i klukkuglugganum tydir, adur en hun er stadfest.
 *
 * Reiknad a TEIM degi sem verid er ad stilla, ekki a naestu hringingu.
 * Aetti linan alltaf vid naestu hringingu stodu hun kyrr tegar madur
 * stillir dag sem hringir ekki naest - og glugginn liti ut fyrir ad
 * bregdast ekki vid skifunni.
 *
 * Tess vegna eru ordin tvenns konar. Se dagurinn sjalfur naesta hringing
 * er talan svefn, sama tala og belgurinn a spjaldinu synir a eftir.
 * Annars er hun bid: "Hringir eftir 2 daga 6 klst". Enginn sefur i tvo
 * daga, og linan ma ekki halda tvi fram.
 *
 * null tegar engin hringing er til - slokktur vekjari eda enginn dagur
 * valinn. Tha er ekkert ad segja og linan er falin.
 */
private fun sleepPreview(
    context: Context,
    prefs: Prefs,
    enabled: Boolean,
    days: Set<Int>,
    defaultHour: Int,
    defaultMinute: Int,
    dayTimes: Map<Int, Int>,
    target: Picking,
    pickedHour: Int,
    pickedMinute: Int
): String? {
    if (!enabled || days.isEmpty()) return null

    val hour = if (target is Picking.AllDays) pickedHour else defaultHour
    val minute = if (target is Picking.AllDays) pickedMinute else defaultMinute
    val times = when (target) {
        // Samrymist onConfirm: "Alla daga" hreinsar eigin timana.
        Picking.AllDays -> emptyMap()
        is Picking.Day -> Prefs.applyPickedTime(
            defaultMinutes = defaultHour * 60 + defaultMinute,
            dayTimes = dayTimes,
            day = target.day,
            pickedMinutes = pickedHour * 60 + pickedMinute
        )
    }

    // Adeins tann dag sem verid er ad stilla, tegar tad a vid.
    val scope = if (target is Picking.Day) setOf(target.day) else days
    val picked = TriggerTimes.next(
        days = scope,
        hour = hour,
        minute = minute,
        dayTimes = times,
        skipMillis = prefs.skipNextMillis
    ) ?: return null

    val left = countdownText(context, picked) ?: return null

    // Naesta raunveruleg hringing yfir ALLA daga. Se hun su sama er talan
    // svefn; annars er hun bid fram ad teim degi.
    val nextOfAll = TriggerTimes.next(
        days = days,
        hour = hour,
        minute = minute,
        dayTimes = times,
        skipMillis = prefs.skipNextMillis
    )
    return if (picked == nextOfAll) {
        context.getString(R.string.sleep_preview, left)
    } else {
        context.getString(R.string.sleep_preview_other, left)
    }
}

/** Hversu langt profunarhringingin er fram i timann. */
private const val TEST_ALARM_SECONDS = 30

/** Klukkan a veggnum nuna - EKKI stilltur vekjaratimi. */
private fun currentHour(): Int =
    Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

/** Ein minuta i millisekundum - lengd tiksins hja teljaranum. */
private const val MINUTE_MILLIS = 60_000L

/** Litil bid framyfir minutumotin svo tikkid lendi orugglega hinum megin. */
private const val TICK_SLACK_MILLIS = 250L

/**
 * Timapunkturinn sem vekjarinn stefnir a: blundslok se blundad, annars
 * naesta hringing.
 *
 * Baedi nextAlarmDescription og countdownDescription lesa hedan svo
 * teljarinn og textinn undir honum tali um SAMA timapunkt. Vaeru teir
 * reiknadir hvor i sinu lagi gaeti teljarinn talid nidur ad hringingu
 * morgundagsins medan textinn segdi "Blundar til 07:09".
 */
private fun alarmTargetMillis(prefs: Prefs): Long? {
    val snoozeAt = prefs.snoozeUntilMillis
    if (snoozeAt > System.currentTimeMillis()) return snoozeAt
    return AlarmScheduler.nextTriggerTime(prefs)
}

/**
 * Bidtiminn i ordum: "7 min", "2 klst 7 min" eda "3 dagar 2 klst".
 * Skilar null tegar enginn dagur er valinn - tha er ekkert ad telja nidur.
 *
 * Dagarnir eru med tvi vekjaradagar geta verid strjalir: se adeins
 * mandagur valinn er naesta hringing allt ad viku i burtu, og "154 klst"
 * segir engum neitt.
 */
private fun countdownDescription(context: Context, prefs: Prefs): String? {
    val target = alarmTargetMillis(prefs) ?: return null
    return countdownText(context, target)
}

/** Sami texti og i belgnum, en um hvada timapunkt sem er. */
private fun countdownText(context: Context, targetMillis: Long): String? {
    val left = TriggerTimes.countdown(System.currentTimeMillis(), targetMillis) ?: return null
    return when {
        left.days == 1 -> context.getString(R.string.countdown_day_hours, left.days, left.hours)
        left.days > 1 -> context.getString(R.string.countdown_days_hours, left.days, left.hours)
        left.hours > 0 ->
            context.getString(R.string.countdown_hours_minutes, left.hours, left.minutes)
        else -> context.getString(R.string.countdown_minutes, left.minutes)
    }
}

private fun nextAlarmDescription(context: Context, prefs: Prefs): String {
    val target = alarmTargetMillis(prefs)
        ?: return context.getString(R.string.no_day_selected)

    // Blundur er eina tilfellid tar sem markid er ekki skradur vekjaratimi -
    // tha nefnum vid bara klukkuna, ekki vikudaginn.
    if (target == prefs.snoozeUntilMillis) {
        val format = SimpleDateFormat("HH:mm", Locale("is", "IS"))
        return context.getString(R.string.snoozing_until, format.format(Date(target)))
    }
    val format = SimpleDateFormat("EEEE d. MMMM 'kl.' HH:mm", Locale("is", "IS"))
    // Hastafur: dagurinn stendur her sem merki a eftir "Naest:", ekki inni
    // i setningu. Sleppitextinn ad nedan er annad mal - tar er hann i
    // midri setningu og a ad vera smaletradur.
    return context.getString(
        R.string.next_alarm,
        Dates.capitalized(format.format(Date(target)))
    )
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
