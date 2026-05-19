package com.daiatech.waveform.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.daiatech.waveform.segmentation.speed.PlaybackSpeed
import com.daiatech.waveform.times
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.graphs.graphBarHeight

@Composable
fun WaveformAudioPlayerUi(
    state: AudioPlayerState,
    progressMs: Long,
    isPlaying: Boolean,
    togglePlayback: () -> Unit,
    seek: (Long) -> Unit,
    speed: PlaybackSpeed,
    updateSpeed: (PlaybackSpeed) -> Unit,
    colors: AudioPlayerColors = audioPlayerColors(),
    speedLabel: String? = "Speed",
    zoomLabel: String? = "Zoom",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background),
    ) {
        PlayerWaveform(
            state = state,
            progressMs = progressMs,
            seek = seek,
            colors = colors,
        )
        PlayerToolbar(
            state = state,
            progressMs = progressMs,
            isPlaying = isPlaying,
            togglePlayback = togglePlayback,
            speed = speed,
            updateSpeed = updateSpeed,
            colors = colors,
            speedLabel = speedLabel,
            zoomLabel = zoomLabel,
        )
    }
}

@Composable
private fun PlayerPreviewScaffold(
    dimensions: AudioPlayerDimensions = AudioPlayerDimensions(),
    content: @Composable (
        state: AudioPlayerState,
        progressMs: Long,
        isPlaying: Boolean,
        speed: PlaybackSpeed,
        togglePlayback: () -> Unit,
        seek: (Long) -> Unit,
        updateSpeed: (PlaybackSpeed) -> Unit,
    ) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val progressJobRef = remember { mutableStateOf<Job?>(null) }
    var progressMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(PlaybackSpeed.X1_00) }

    ProvidePlayerDimensions(dimensions) {
        val state = rememberAudioPlayerState(
            amplitudes = listOf(100, 200, 300, 500, 100, 20).times(20),
            durationMs = 8000L
        )

        Surface {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                content(
                    state,
                    progressMs,
                    isPlaying,
                    speed,
                    {
                        if (isPlaying) {
                            progressJobRef.value?.cancel()
                            isPlaying = false
                        } else {
                            isPlaying = true
                            progressJobRef.value = coroutineScope.launch {
                                while (progressMs < 8000L && isPlaying) {
                                    progressMs += (50.times(speed.float)).toLong()
                                    delay(50)
                                }
                                if (progressMs >= 8000L) progressMs = 0L
                                isPlaying = false
                            }
                        }
                    },
                    { progressMs = (progressMs + it).coerceIn(0, 8000L) },
                    { speed = it },
                )
            }
        }
    }
}

@Preview
@Composable
fun WaveformAudioPlayerUiPreview() {
    PlayerPreviewScaffold { state, progressMs, isPlaying, speed, togglePlayback, seek, updateSpeed ->
        WaveformAudioPlayerUi(
            state = state,
            progressMs = progressMs,
            isPlaying = isPlaying,
            togglePlayback = togglePlayback,
            seek = seek,
            speed = speed,
            updateSpeed = updateSpeed,
        )
    }
}

@Preview
@Composable
fun WaveformAudioPlayerUiCompactPreview() {
    PlayerPreviewScaffold(dimensions = compactPlayerDimensions()) { state, progressMs, isPlaying, speed, togglePlayback, seek, updateSpeed ->
        WaveformAudioPlayerUi(
            state = state,
            progressMs = progressMs,
            isPlaying = isPlaying,
            togglePlayback = togglePlayback,
            seek = seek,
            speed = speed,
            updateSpeed = updateSpeed,
            speedLabel = null,
            zoomLabel = null
        )
    }
}
