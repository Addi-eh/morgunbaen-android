package com.morgunbaen.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.morgunbaen.app.data.Dates
import com.morgunbaen.app.data.Episode
import com.morgunbaen.app.data.RuvClient
import com.morgunbaen.app.ui.MorgunbaenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fyrri bænir.
 *
 * RUV geymir thaettina an tess ad teir renni ut, svo tetta kostar ekkert
 * annad en ad birta tad sem thegar er til. Appid verdur tannig lika litid
 * safn, ekki bara vekjari.
 *
 * Thaettirnir eru STREYMDIR her - ekki hladnir nidur. Baen dagsins er tad eina
 * sem tarf ad vera til stadar an nettengingar; tetta er efni sem folk velur
 * ad hlusta a medvitad, og ta er tad hvort ed er med simann i hondunum.
 */
class HistoryActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                // true = ExoPlayer sér sjálfur um hljóðfókus. Ólíkt vekjaranum
                // á þetta að hegða sér eins og venjulegur miðill.
                true
            )
            .build()

        setContent {
            MorgunbaenTheme {
                HistoryScreen(
                    player = player,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Ekki halda afram ad spila tegar notandinn fer ur skjanum.
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HistoryActivity::class.java))
        }
    }
}

private sealed class LoadState {
    data object Loading : LoadState()
    data class Loaded(val episodes: List<Episode>) : LoadState()
    data class Failed(val message: String) : LoadState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(player: ExoPlayer?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var playingId by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Activity.onStop slokknar á spiluninni. Án þessa sæi
                // notandinn Stopp-takkann þegar hann kæmi til baka.
                playingId = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playingId = null
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playingId = null
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(Unit) {
        state = withContext(Dispatchers.IO) {
            try {
                val episodes = RuvClient()
                    .fetchEpisodes()
                    .sortedByDescending { it.firstrun }
                    .take(MAX_EPISODES)

                if (episodes.isEmpty()) {
                    LoadState.Failed(context.getString(R.string.history_empty))
                } else {
                    LoadState.Loaded(episodes)
                }
            } catch (e: Exception) {
                LoadState.Failed(e.message ?: context.getString(R.string.history_empty))
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val current = state) {
                is LoadState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is LoadState.Failed -> {
                    Text(
                        text = current.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is LoadState.Loaded -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(current.episodes, key = { it.id }) { episode ->
                            EpisodeRow(
                                episode = episode,
                                isPlaying = playingId == episode.id,
                                onToggle = {
                                    if (playingId == episode.id) {
                                        player?.pause()
                                        playingId = null
                                    } else {
                                        player?.apply {
                                            setMediaItem(MediaItem.fromUri(episode.fileUrl))
                                            prepare()
                                            play()
                                        }
                                        playingId = episode.id
                                    }
                                },
                                onShare = { shareEpisode(context, episode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onShare: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = Dates.formatWithWeekday(episode.firstrun),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.cd_share)
                )
            }

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.cd_stop else R.string.cd_play
                    )
                )
            }
        }
    }
}

/** Sidustu tvaer vikur. Lengra aftur er tetta ordid safn frekar en saga. */
private const val MAX_EPISODES = 14

/**
 * Deilir baen med hlekk a Spilara RUV.
 *
 * Vid deilum EKKI hljodskranni sjalfri - hun er efni RUV. Hlekkurinn sendir
 * folk til teirra, sem er baedi retta leidin og sú sem heldur afram ad virka
 * eftir ad appid er löngu gleymt.
 */
fun shareEpisode(context: Context, episode: Episode) {
    val date = Dates.formatWithWeekday(episode.firstrun)
    val url = RuvClient.episodeWebUrl(episode.id)

    val text = context.getString(R.string.share_text, episode.title, date, url)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.share))
    )
}
