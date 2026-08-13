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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.morgunbaen.app.alarm.AlarmScheduler
import com.morgunbaen.app.data.EpisodeRepository
import com.morgunbaen.app.data.Episode
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.data.RuvClient
import com.morgunbaen.app.ui.MorgunbaenTheme
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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

            // ---------- Vekjaratimi ----------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                            style = MaterialTheme.typography.displayMedium
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                persistAndReschedule()
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                hour = h
                                minute = m
                                persistAndReschedule()
                            },
                            hour, minute, true
                        ).show()
                    }) {
                        Text(stringResource(R.string.change_time))
                    }

                    Spacer(Modifier.height(8.dp))
                    DayPicker(selected = days, onChange = {
                        days = it
                        persistAndReschedule()
                    })

                    Spacer(Modifier.height(16.dp))

                    // Morgunbaenin er ekki flutt um helgar - tha spilar appid
                    // sidustu baen vikunnar. Margir vilja sofa lengur ta an
                    // tess ad sleppa henni alveg.
                    SettingRow(
                        label = stringResource(R.string.weekend_label),
                        description = if (weekendEnabled) {
                            stringResource(
                                R.string.weekend_time,
                                String.format(
                                    Locale.getDefault(),
                                    "%02d:%02d", weekendHour, weekendMinute
                                )
                            )
                        } else {
                            stringResource(R.string.weekend_desc)
                        },
                        checked = weekendEnabled,
                        onCheckedChange = {
                            weekendEnabled = it
                            prefs.weekendTimeEnabled = it
                            persistAndReschedule()
                        }
                    )

                    if (weekendEnabled) {
                        TextButton(onClick = {
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
                        }) {
                            Text(stringResource(R.string.change_time))
                        }
                    }

                    if (enabled) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = nextAlarmText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Stada baenarinnar ----------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.prayer_status),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    if (cachedTitle != null) {
                        Text(cachedTitle!!, style = MaterialTheme.typography.bodyLarge)
                        cachedDate?.let {
                            Text(
                                text = "Flutt " + formatIsoDate(it),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.no_prayer_yet),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            syncing = true
                            status = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    val repo = EpisodeRepository(context)
                                    val r = repo.sync()
                                    // Frettirnar fylgja med - adur sotti
                                    // tessi takki adeins baenina.
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
                        enabled = !syncing
                    ) {
                        if (syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.fetch_now))
                    }

                    status?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { HistoryActivity.start(context) }) {
                            Text(stringResource(R.string.history_open))
                        }

                        // Deiling krefst tess ad vid vitum HVADA thattur tetta er.
                        val shareId = cachedEpisodeId
                        val shareTitle = cachedTitle
                        val shareDate = cachedDate
                        if (shareId != null && shareTitle != null && shareDate != null) {
                            TextButton(onClick = {
                                shareEpisode(
                                    context,
                                    Episode(
                                        id = shareId,
                                        title = shareTitle,
                                        firstrun = shareDate,
                                        fileUrl = ""
                                    )
                                )
                            }) {
                                Text(stringResource(R.string.share))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Vakningarstillingar ----------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    SettingRow(
                        label = stringResource(R.string.fade_in_label),
                        description = if (fadeIn) {
                            stringResource(R.string.fade_in_desc, fadeSeconds)
                        } else {
                            stringResource(R.string.fade_in_off_desc)
                        },
                        checked = fadeIn,
                        onCheckedChange = {
                            fadeIn = it
                            prefs.fadeInEnabled = it
                        }
                    )

                    // Lengdin skiptir adeins mali tegar fade-in er virkt.
                    if (fadeIn) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.fade_length),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(4.dp))
                        FadeLengthPicker(
                            selected = fadeSeconds,
                            onChange = {
                                fadeSeconds = it
                                prefs.fadeInSeconds = it
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    SettingRow(
                        label = stringResource(R.string.vibrate_label),
                        description = if (fadeIn) {
                            stringResource(R.string.vibrate_desc_fade)
                        } else {
                            stringResource(R.string.vibrate_desc)
                        },
                        checked = vibrate,
                        onCheckedChange = {
                            vibrate = it
                            prefs.vibrateEnabled = it
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    // Frettirnar eru naesti dagskrarlidur a eftir Morgunbaeninni,
                    // svo tetta speglar utsendinguna sjalfa.
                    SettingRow(
                        label = stringResource(R.string.news_label),
                        description = when {
                            !newsEnabled -> stringResource(R.string.news_desc_off)
                            newsSyncing -> stringResource(R.string.news_fetching)
                            // TVO OLIK ASTOND - ekki rugla teim saman.
                            //
                            // 1) VEKJARATIMINN er fyrir kl. 07:00. Tha eru
                            //    frettirnar aldrei til tegar hringt er, sama
                            //    hvada dag. Varanlegt astand sem notandinn
                            //    getur adeins leyst med tvi ad faera vekjarann.
                            //
                            //    "hour" er stillti vekjaratiminn, ekki klukkan.
                            hour < RuvClient.FRETTIR_HOUR ->
                                stringResource(R.string.news_alarm_too_early)

                            // 2) KLUKKAN er undir 07:00 akkurat nu og
                            //    frettatimi dagsins er tvi ekki kominn ut enn.
                            //    Skammvinnt astand sem leysist af sjalfu ser.
                            currentHour() < RuvClient.FRETTIR_HOUR &&
                                !newsFirstrun.isTodays() ->
                                stringResource(R.string.news_not_yet)
                            newsFirstrun.isTodays() -> stringResource(
                                R.string.news_ready,
                                newsFirstrun!!.substringAfter('T').substring(0, 5)
                            )
                            !newsAttempted -> stringResource(R.string.news_none)
                            else -> stringResource(R.string.news_missing)
                        },
                        checked = newsEnabled,
                        onCheckedChange = {
                            newsEnabled = it
                            prefs.newsEnabled = it

                            // Saekja strax tegar kveikt er - annars bidur
                            // notandinn i allt ad sex klst eftir ad sja
                            // hvort tetta virki yfirleitt.
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
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.snooze_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.snooze_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    MinutePicker(
                        options = listOf(5, 9, 10, 15, 20),
                        selected = snoozeMinutes,
                        onChange = {
                            snoozeMinutes = it
                            prefs.snoozeMinutes = it
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Heilsuvoktun ----------
            // Rautt = eitthvad hefur tegar farid urskeidis.
            // Tetta er mikilvaegara en stillingavidvaranirnar tvi tad er
            // eina merkid um bilun sem notandinn faer - siminn segir ekkert.
            when (health) {
                Health.MISSED_ALARM -> {
                    WarningCard(
                        text = stringResource(R.string.warn_missed_alarm),
                        actionLabel = stringResource(R.string.acknowledge),
                        onAction = {
                            prefs.missedAlarmAcknowledged = System.currentTimeMillis()
                            health = checkHealth(prefs)
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Health.STALE_SYNC -> {
                    WarningCard(
                        text = stringResource(R.string.warn_stale_sync),
                        actionLabel = stringResource(R.string.open_settings),
                        onAction = { openBatterySettings(context) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Health.OK -> Unit
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
            // Vekjari sem hringir bara a virkum dogum er ONOTADUR yfir helgi
            // - og tegja tvi a manudagsmorgni. Ekkert API laetur vita af tessu
            // og ekkert API slekkur a tvi; notandinn verdur ad gera tad sjalfur.
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
 * Dagavalid. Notar FlowRow svo allir sjo dagarnir komist fyrir
 * - i venjulegri Row dettur sunnudagurinn ut fyrir skjabrunina.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayPicker(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    // Calendar.SUNDAY = 1 ... Calendar.SATURDAY = 7
    val labels = listOf(
        Calendar.MONDAY to "Má",
        Calendar.TUESDAY to "Þr",
        Calendar.WEDNESDAY to "Mi",
        Calendar.THURSDAY to "Fi",
        Calendar.FRIDAY to "Fö",
        Calendar.SATURDAY to "La",
        Calendar.SUNDAY to "Su"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { (day, label) ->
            val isOn = day in selected
            FilterChip(
                selected = isOn,
                onClick = {
                    onChange(if (isOn) selected - day else selected + day)
                },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun WarningCard(text: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Fastir valkostir frekar en sleði. Enginn tarf ad velja 47 sekundur,
 * og fastir kostir eru miklu audveldari i notkun a litlum skja.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MinutePicker(options: List<Int>, selected: Int, onChange: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { minutes ->
            FilterChip(
                selected = minutes == selected,
                onClick = { onChange(minutes) },
                label = { Text("$minutes mín") }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FadeLengthPicker(selected: Int, onChange: (Int) -> Unit) {
    val options = listOf(10, 30, 60, 120)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { seconds ->
            FilterChip(
                selected = seconds == selected,
                onClick = { onChange(seconds) },
                label = {
                    Text(
                        if (seconds < 60) "$seconds sek"
                        else "${seconds / 60} mín"
                    )
                }
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    text: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onPrimary) { Text(primaryLabel) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}

/** Nidurstada heilsuvoktunar. */
private enum class Health { OK, MISSED_ALARM, STALE_SYNC }

/**
 * Athugar hvort eitthvad hafi thegar farid urskeidis.
 *
 * Tvo einkenni benda til tess ad siminn se ad stodva appid:
 *
 *  1. Vekjaratimi sem er lidinn hja an tess ad vekjarinn hafi hringt.
 *  2. Bakgrunnssokn sem hefur ekki naad ad keyra i meira en solarhring.
 *
 * Hvorugt greinist sjalfkrafa af Android - appid verdur ad taka eftir tvi sjalft.
 */
private fun checkHealth(prefs: Prefs): Health {
    if (!prefs.alarmEnabled) return Health.OK

    val now = System.currentTimeMillis()

    // Vid segjum ekkert fyrr en vekjarinn hefur hringt ad minnsta kosti einu
    // sinni - annars fengi hver nyr notandi vidvorun a fyrsta degi.
    if (prefs.lastAlarmFiredMillis > 0L) {
        val expected = AlarmScheduler.previousTriggerTime(prefs)
        if (expected != null &&
            expected > prefs.lastAlarmFiredMillis + TimeUnit.MINUTES.toMillis(5) &&
            expected > prefs.missedAlarmAcknowledged
        ) {
            return Health.MISSED_ALARM
        }
    }

    if (prefs.lastSyncMillis > 0L &&
        now - prefs.lastSyncMillis > TimeUnit.HOURS.toMillis(36)
    ) {
        return Health.STALE_SYNC
    }

    return Health.OK
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

/** Er tessi timastimpill fra deginum i dag? */
private fun String?.isTodays(): Boolean {
    val raw = this ?: return false
    val date = raw.substringBefore('T').substringBefore(' ')
    return date == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

/** Klukkan a veggnum nuna - EKKI stilltur vekjaratimi. */
private fun currentHour(): Int =
    Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

/** "2026-08-13T06:55:00" -> "13. ágúst" */
private fun formatIsoDate(raw: String): String {
    val datePart = raw.substringBefore('T').substringBefore(' ')
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart)!!
        SimpleDateFormat("d. MMMM", Locale("is", "IS")).format(parsed)
    } catch (e: Exception) {
        datePart
    }
}

private fun nextAlarmDescription(prefs: Prefs): String {
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
    context.startActivity(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}
