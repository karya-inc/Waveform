package com.daiatech.waveform

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun RowScope.AudioPlayer(
    audioFilePath: String,
    durationMs: Long,
    onProgressUpdate: (Long) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember(audioFilePath) { mutableStateOf(false) }
    var progressMs by remember(audioFilePath) { mutableLongStateOf(0L) }
    val exoPlayer = remember(audioFilePath) { ExoPlayer.Builder(context).build() }
    var isPaused by remember(audioFilePath) { mutableStateOf(false) }

    DisposableEffect(audioFilePath) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                progressMs = exoPlayer.currentPosition
                onProgressUpdate(progressMs)
                handler.postDelayed(this, AUDIO_PLAYER_REFRESH_RATE_MS)
            }
        }

        val listener = object : Player.Listener {
            init {
                handler.postDelayed(runnable, 0)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_ENDED, ExoPlayer.STATE_IDLE -> {
                        handler.removeCallbacks(runnable)
                        progressMs = 0
                    }

                    else -> {}
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                super.onIsPlayingChanged(playing)
                isPlaying = playing
                if (playing) {
                    isPaused = false
                    handler.postDelayed(runnable, AUDIO_PLAYER_REFRESH_RATE_MS)
                } else {
                    handler.removeCallbacks(runnable)
                }
            }
        }

        exoPlayer.addListener(listener)

        // Cleanup when component is destroyed
        onDispose {
            exoPlayer.removeListener(listener)
            handler.removeCallbacks(runnable)
            exoPlayer.release()
        }
    }

    AudioPlayer(
        isPlaying = isPlaying,
        currentPosition = progressMs,
        durationMS = durationMs,
        onPlay = {
            if (!isPaused) {
                val mediaItem = MediaItem.fromUri(audioFilePath)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                // startMs?.let { exoPlayer.seekTo(it) }
            }
            exoPlayer.play()
        },
        onPause = {
            exoPlayer.pause()
            isPaused = true
        }
    )
}

@Composable
private fun RowScope.AudioPlayer(
    isPlaying: Boolean,
    currentPosition: Long,
    durationMS: Long,
    onPlay: () -> Unit,
    onPause: () -> Unit
) {
    val progress = if (durationMS == 0L) 0f else currentPosition.toFloat().div(durationMS)
    Icon(
        painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
        contentDescription = stringResource(R.string.play),
        modifier = Modifier
            .size(48.dp)
            .clickable { if (isPlaying) onPause() else onPlay() },
        tint = MaterialTheme.colorScheme.primary
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.weight(1f)
    )
    Text(
        text = "\t${millisecondsToMmSs(currentPosition)}/${millisecondsToMmSs(durationMS)}\t",
        color = MaterialTheme.colorScheme.inversePrimary
    )
}

@Preview
@Composable
private fun AudioPlayerPreview() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AudioPlayer(
            isPlaying = false,
            currentPosition = 4000,
            durationMS = 8000,
            onPlay = {},
            onPause = {}
        )
    }
}