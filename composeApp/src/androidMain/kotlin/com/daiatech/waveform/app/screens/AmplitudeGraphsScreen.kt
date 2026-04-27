package com.daiatech.waveform.app.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.daiatech.waveform.icons.Play
import com.daiatech.waveform.icons.Pause
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.waveform.AUDIO_PLAYER_REFRESH_RATE_MS
import com.daiatech.waveform.app.model.AudioMeta
import com.daiatech.waveform.app.utils.LocalAudioManager
import com.daiatech.waveform.graphs.AmplitudeBarGraph
import com.daiatech.waveform.graphs.CenterPinnedAmplitudeBarGraph

@Composable
fun AmplitudeGraphsScreen(audioFilePath: String) {
    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    val audioManager = LocalAudioManager.current
    LaunchedEffect(Unit) {
        val amplitudes = audioManager.getAmplitudes(path = audioFilePath)
        val duration = audioManager.getDuration(path = audioFilePath)
        meta = AudioMeta(amplitudes, duration)
    }
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }


    var isPlaying by remember { mutableStateOf(false) }
    var progressMs by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(progressMs.toFloat()) }
    var isUserInteracting by remember { mutableStateOf(false) }
    LaunchedEffect(progressMs) {
        if (!isUserInteracting) {
            sliderPosition = progressMs.toFloat()
        }
    }


    DisposableEffect(exoPlayer) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                progressMs = exoPlayer.currentPosition.toInt()
                handler.postDelayed(this, AUDIO_PLAYER_REFRESH_RATE_MS)
            }
        }

        val listener = object : Player.Listener {
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

    val onPlay = {
        if (!isPaused) {
            val mediaItem = MediaItem.Builder()
                .setUri(audioFilePath)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    val onPause = {
        exoPlayer.pause()
        isPaused = true
    }



    meta?.let { meta ->
        Column {
            val amplitudes = meta.amplitudes
            val durationMS = meta.duration

            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Image(
                    painter = rememberVectorPainter(if(isPlaying) Pause else Play),
                    contentDescription = "play",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { if (isPlaying) onPause() else onPlay() }
                )

                Spacer(Modifier.width(8.dp))
                AmplitudeBarGraph(
                    amplitudes = amplitudes,
                    progress = if (durationMS == 0L) {
                        0f
                    } else {
                        progressMs.toFloat().div(durationMS)
                    },
                    onProgressChange = { fraction ->
                        val newPosition = fraction.times(durationMS)
                        if (!isUserInteracting) {
                            isUserInteracting = true
                            onPause()
                        }
                        sliderPosition = newPosition
                    },
                    onProgressChangeFinished = {
                        val newSeekPosition = sliderPosition.toLong()
                        exoPlayer.seekTo(newSeekPosition)
                        isUserInteracting = false
                        onPlay()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    spikeWidth = 2.dp
                )
            }

            CenterPinnedAmplitudeBarGraph(
                amplitudes = amplitudes,
                durationMs = durationMS,
                progressMs = progressMs.toLong(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}