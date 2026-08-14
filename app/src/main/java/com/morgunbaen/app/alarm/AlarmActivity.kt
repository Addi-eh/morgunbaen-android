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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Bakktakkinn ma ekki loka vekjaranum - annars slekkur folk
        // a honum i svefnrofunum an tess ad atta sig a tvi.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Visvitandi tomt
            }
        })

        val prefs = Prefs(this)
        val title = prefs.cachedTitle
        val firstrun = prefs.cachedFirstrun

        setContent {
            MorgunbaenTheme {
                AlarmScreen(
                    episodeTitle = title,
                    firstrun = firstrun,
                    onDismiss = {
                        AlarmService.dismiss(this)
                        finish()
                    },
                    onSnooze = {
                        AlarmService.snooze(this)
                        finish()
                    }
                )
            }
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
                    text = formatFirstrun(firstrun),
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

/** "2026-08-13T06:55:00" -> "13. ágúst" */
private fun formatFirstrun(firstrun: String): String {
    val datePart = firstrun.substringBefore('T').substringBefore(' ')
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart)!!
        SimpleDateFormat("d. MMMM", Locale("is", "IS")).format(parsed)
    } catch (e: Exception) {
        datePart
    }
}

private fun currentTimeString(): String =
    SimpleDateFormat("HH:mm", Locale("is", "IS")).format(Date())
