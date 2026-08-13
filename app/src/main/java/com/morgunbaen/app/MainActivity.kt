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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.morgunbaen.app.alarm.AlarmScheduler
import com.morgunbaen.app.data.EpisodeRepository
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.ui.MorgunbaenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var nextAlarmText by remember { mutableStateOf(nextAlarmDescription(prefs)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmOk = AlarmScheduler.canScheduleExact(context)
                batteryOk = isIgnoringBatteryOptimizations(context)
                nextAlarmText = nextAlarmDescription(prefs)
                cachedTitle = prefs.cachedTitle
                cachedDate = prefs.cachedFirstrun
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
                                    EpisodeRepository(context).sync()
                                }
                                syncing = false
                                cachedTitle = prefs.cachedTitle
                                cachedDate = prefs.cachedFirstrun
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
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------- Aminningar um kerfisstillingar ----------
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
