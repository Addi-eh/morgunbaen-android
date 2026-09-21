package com.morgunbaen.app.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.R
import com.morgunbaen.app.data.Dates
import com.morgunbaen.app.data.Prefs
import com.morgunbaen.app.ui.MorgunbaenTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Skjarinn sem notandinn vaknar vid.
 *
 * Vidmotid er viljandi mjog einfalt - manneskja sem er nyvoknud
 * a ekki ad turfa ad leita ad neinu.
 */
class AlarmActivity : ComponentActivity() {

    // AlarmService reynir ad koma skjanum aftur upp medan hann sest ekki.
    // Sja AlarmService.scheduleScreenRetries().
    override fun onResume() {
        super.onResume()
        AlarmService.screenVisible.value = true
    }

    override fun onPause() {
        super.onPause()
        // Ytt a Slokkva eda Blunda: skjarinn lokar ser (onPause) ADUR en
        // tjonustan faer skipunina. Segdi hann sig osynilegan tarna gaeti
        // endurtilraun lent a milli og opnad skjainn aftur eftir ad
        // notandinn slokkti. Stodvunin sjalf hreinsar tilraunirnar.
        if (!isFinishing) AlarmService.screenVisible.value = false
    }

    override fun onDestroy() {
        AlarmService.screenVisible.value = false
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Bakktakkinn ma ekki loka vekjaranum - annars slekkur folk
        // a honum i svefnrofunum an tess ad atta sig a tvi. Eftir ad
        // notandinn er vaknadur (spurning, hlustun) ma hann fara.
        val backBlocker = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Visvitandi tomt
            }
        }
        onBackPressedDispatcher.addCallback(this, backBlocker)

        val prefs = Prefs(this)
        val title = prefs.cachedTitle
        val firstrun = prefs.cachedFirstrun

        // Opnad ur hlustunartilkynningunni? Tha beint i hlustun.
        val initial = if (AlarmService.listeningState.value) Phase.LISTENING else Phase.RINGING

        setContent {
            MorgunbaenTheme {
                var phase by remember { mutableStateOf(initial) }

                LaunchedEffect(phase) {
                    backBlocker.isEnabled = phase == Phase.RINGING
                    // Vaknadur notandi sem hlustar tarf ekki upplystan skja.
                    if (phase == Phase.RINGING) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                // Hlustun lokid (baenin buin eda stodvud ur tilkynningu) -
                // skjarinn lokar ser. Adeins vid true -> false: rett eftir
                // awake() er tjonustan ekki enn byrjud ad spila.
                if (phase == Phase.LISTENING) {
                    LaunchedEffect(Unit) {
                        var started = false
                        AlarmService.listeningState.collect { now ->
                            if (now) started = true
                            else if (started) finish()
                        }
                    }
                }

                when (phase) {
                    Phase.RINGING -> AlarmScreen(
                        episodeTitle = if (prefs.wakeWithSound) null else title,
                        firstrun = if (prefs.wakeWithSound) null else firstrun,
                        onDismiss = {
                            if (prefs.wakeWithSound) {
                                AlarmService.awake(this)
                                when (prefs.afterWake) {
                                    Prefs.AFTER_AUTO -> phase = Phase.LISTENING
                                    Prefs.AFTER_ASK -> phase = Phase.ASK
                                    else -> finish()
                                }
                            } else {
                                AlarmService.dismiss(this)
                                finish()
                            }
                        },
                        onSnooze = {
                            AlarmService.snooze(this)
                            finish()
                        }
                    )

                    Phase.ASK -> AskScreen(
                        episodeTitle = title,
                        onListen = {
                            AlarmService.listen(this)
                            phase = Phase.LISTENING
                        },
                        onClose = { finish() }
                    )

                    Phase.LISTENING -> ListeningScreen(
                        episodeTitle = title,
                        firstrun = firstrun,
                        onStop = {
                            AlarmService.dismiss(this)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

/** Hvar notandinn er staddur: vekjarinn hringir, spurning, eda hlustun. */
private enum class Phase { RINGING, ASK, LISTENING }

/**
 * "Spyrja mig": vekjarinn er thagnadur, baenin bidur eftir svari.
 * Venjulegir takkar - notandinn er vaknadur og tarf ekki ad sanna tad.
 */
@Composable
private fun AskScreen(
    episodeTitle: String?,
    onListen: () -> Unit,
    onClose: () -> Unit
) {
    CalmScreen {
        Text(
            text = stringResource(R.string.listen_ask_heading),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        if (episodeTitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = episodeTitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onListen,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(stringResource(R.string.listen_ask_play))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.listen_ask_close))
        }
    }
}

/** Baenin spilar. Einn takki, venjulegt yt - enginn a ad turfa ad halda inni. */
@Composable
private fun ListeningScreen(
    episodeTitle: String?,
    firstrun: String?,
    onStop: () -> Unit
) {
    CalmScreen {
        Text(
            text = stringResource(R.string.listen_playing_heading),
            style = MaterialTheme.typography.titleLarge
        )
        if (episodeTitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = episodeTitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        if (firstrun != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = Dates.formatShort(firstrun),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(48.dp))
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(R.string.listen_stop))
        }
    }
}

/** Klukkan efst og efnid fyrir midju - sama rammi og vekjaraskjarinn. */
@Composable
private fun CalmScreen(content: @Composable () -> Unit) {
    var clock by remember { mutableStateOf(currentTimeString()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            clock = currentTimeString()
            delay(1_000)
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = clock,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(40.dp))
            content()
        }
    }
}

@Composable
private fun AlarmScreen(
    episodeTitle: String?,
    firstrun: String?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var clock by remember { mutableStateOf(currentTimeString()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            clock = currentTimeString()
            delay(1_000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = clock,
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.alarm_screen_heading),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            if (episodeTitle != null) {
                Text(
                    text = episodeTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            if (firstrun != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Dates.formatShort(firstrun),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(64.dp))

            HoldToDismissButton(onDismiss = onDismiss)

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = stringResource(R.string.snooze))
            }
        }
    }
}

/**
 * Takki sem tarf ad halda inni i eina og halfa sekundu.
 *
 * Astaedan: manneskja i svefnrofunum slekkur a vekjara med einu smelli an tess
 * ad vakna almennilega - og sefur svo yfir sig. Langt yt krefst nogu mikillar
 * medvitundar til ad hun se raunverulega voknud.
 *
 * Framvindan fyllist synilega ur vinstri til haegri svo notandinn skilji
 * strax hvad er ad gerast; annars heldur hann ad takkinn se bilaður.
 * Blundtakkinn er afram venjulegt yt - tad a ekki ad vera erfitt ad sofna aftur,
 * heldur ad slokkva alveg.
 */
@Composable
private fun HoldToDismissButton(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }

    // Mjukur afturhvarfshreyfing tegar sleppt er - annars stekkur
    // framvindan i null og litur ut eins og villa.
    val animated by animateFloatAsState(targetValue = progress, label = "hold")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val holdJob = scope.launch {
                            val start = System.currentTimeMillis()
                            while (isActive) {
                                val elapsed = System.currentTimeMillis() - start
                                progress = (elapsed / HOLD_MILLIS.toFloat()).coerceIn(0f, 1f)
                                if (progress >= 1f) {
                                    onDismiss()
                                    break
                                }
                                delay(16)
                            }
                        }
                        // Sleppti notandinn adur en tvi lauk? Ta byrjar hann upp a nytt.
                        tryAwaitRelease()
                        holdJob.cancel()
                        progress = 0f
                    }
                )
            }
    ) {
        // Framvindan sjalf - fyllist undir textanum.
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )

        Text(
            text = stringResource(R.string.hold_to_dismiss),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Hversu lengi tarf ad halda inni til ad slokkva. */
private const val HOLD_MILLIS = 1500L

private fun currentTimeString(): String =
    SimpleDateFormat("HH:mm", Locale("is", "IS")).format(Date())
