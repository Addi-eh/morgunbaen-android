package com.morgunbaen.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.morgunbaen.app.ui.MorgunbaenTheme

/**
 * Um appið og styrkir.
 *
 * Greiðsluupplýsingarnar eru EKKI í appinu. „Styrkja" opnar vefsíðu sem
 * höfundurinn hýsir sjálfur; kennitala og reikningsnúmer eru þar. Ástæðan
 * er einföld: repóið er opinbert og APK-skrár eru varanlegar, svo allt sem
 * er sett í þær verður ekki tekið til baka. Á vefsíðu má breyta þeim eða
 * fjarlægja hvenær sem er.
 *
 * Styrkur opnar ekkert í appinu. Vekjarinn hegðar sér eins fyrir alla.
 */
class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorgunbaenTheme {
                AboutScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.about_verse),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        text = stringResource(R.string.about_verse_ref),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.about_intro),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Hnappurinn hverfur i Play-utgafu: reglur Google um greidslur
            // takmarka hlekki a styrki utan Play. Adeins eitt gildi
            // (BuildConfig) tarf ad breytast, engin onnur skra.
            if (BuildConfig.SHOW_DONATION) {
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.about_donate_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.about_donate_body),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { openUrl(context, BuildConfig.DONATE_URL) }) {
                            Text(stringResource(R.string.about_donate_button))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.about_help_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_help_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { shareApp(context) }) {
                        Text(stringResource(R.string.about_share_app))
                    }
                    TextButton(onClick = { openUrl(context, REPO_URL) }) {
                        Text(stringResource(R.string.about_open_github))
                    }
                    TextButton(onClick = { openUrl(context, ISSUES_URL) }) {
                        Text(stringResource(R.string.about_report_bug))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Utgafunumerid er her svo hagt se ad spyrja "hvada utgafu
            // ertu med?" tegar villa er tilkynnt. Heimildin fyrir
            // kirkjuklukkunni er kurteisi vid tann sem tok hana upp.
            Text(
                text = stringResource(R.string.about_credits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Deilir appinu sjalfu - ekki baen dagsins, sem shareEpisode ser um. */
private fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.about_share_text, REPO_URL))
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.about_share_app))
    )
}

/**
 * Enginn vafri er ekki hrun. Siminn getur verid an vafra, eda hann
 * slokktur i vinnusnidi - tha segjum vid fra i stad tess ad falla.
 */
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.about_no_browser, url), Toast.LENGTH_LONG)
            .show()
    }
}

private const val REPO_URL = "https://github.com/Addi-eh/morgunbaen-android"
private const val ISSUES_URL = "https://github.com/Addi-eh/morgunbaen-android/issues"
