package com.daiatech.waveform.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.times
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.daiatech.waveform.icons.Pause
import com.daiatech.waveform.icons.Play
import com.daiatech.waveform.millisecondsToMmSs
import com.daiatech.waveform.segmentation.speed.PlaybackSpeed
import com.daiatech.waveform.segmentation.speed.SpeedButton
import com.daiatech.waveform.segmentation.zoom.Zoom
import com.daiatech.waveform.segmentation.zoom.ZoomButton
import kotlinx.coroutines.launch

@Composable
fun PlayerToolbar(
    state: AudioPlayerState,
    progressMs: Long,
    isPlaying: Boolean,
    togglePlayback: () -> Unit,
    speed: PlaybackSpeed,
    updateSpeed: (PlaybackSpeed) -> Unit,
    colors: AudioPlayerColors = audioPlayerColors(),
    speedLabel: String? = "Speed",
    zoomLabel: String? = "Zoom",
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val durationMs = remember { state.durationMs }
    val zoom by state.zoom
    val dimensions = LocalAudioPlayerDimensions.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(dimensions.toolbarPadding),
        horizontalArrangement = Arrangement.spacedBy(dimensions.toolbarItemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (speedLabel != null) {
                Text(speedLabel, color = colors.contentPrimary)
            }
            Spacer(Modifier.height(dimensions.toolbarLabelSpacing))
            SpeedButton(
                availableSpeeds = PlaybackSpeed.entries,
                selectedSpeed = speed,
                onSpeedUpdate = updateSpeed,
                containerColor = Color.White.copy(0.1f),
                contentColor = colors.contentPrimary,
                modifier = Modifier
                    .height(dimensions.controlButtonHeight)
                    .fillMaxWidth(),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(dimensions.playButtonSize)
                    .background(colors.waveformColor)
                    .clickable { togglePlayback() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Pause else Play,
                    contentDescription = null,
                    tint = colors.background,
                    modifier = Modifier.size(dimensions.iconSize),
                )
            }
            Spacer(Modifier.height(dimensions.toolbarLabelSpacing))
            Text(
                text = "${millisecondsToMmSs(progressMs)}/${millisecondsToMmSs(durationMs)}",
                color = colors.waveformColor,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (zoomLabel != null) {
                Text(zoomLabel, color = colors.waveformColor)
            }
            Spacer(Modifier.height(dimensions.toolbarLabelSpacing))
            ZoomButton(
                modifier = Modifier
                    .height(dimensions.controlButtonHeight)
                    .fillMaxWidth(),
                onZoomIn = { coroutineScope.launch { state.zoomIn() } },
                onZoomOut = { coroutineScope.launch { state.zoomOut() } },
                enableZoomIn = zoom != Zoom.max,
                enableZoomOut = zoom != Zoom.min,
            )
        }
    }
}

@Composable
private fun ToolbarPreviewScaffold(
    dimensions: AudioPlayerDimensions = AudioPlayerDimensions(),
    content: @Composable (state: AudioPlayerState) -> Unit,
) {
    val density = LocalDensity.current
    ProvidePlayerDimensions(dimensions) {
        val state = remember {
            AudioPlayerState(
                density = density,
                spikeWidth = 2.dp,
                spikeRadius = 2.dp,
                spikePadding = 2.dp,
                amplitudes = listOf(100, 200, 300, 500, 100, 20).times(20),
                durationMs = 8000L,
                graphHeight = dimensions.graphHeight,
                verticalItemSpacing = dimensions.verticalItemSpacing,
                markerFontSize = dimensions.markerFontSize,
            )
        }
        Surface { content(state) }
    }
}

@Preview
@Composable
fun PlayerToolbarPreview() {
    val isPlaying = remember { mutableStateOf(false) }
    ToolbarPreviewScaffold {
        PlayerToolbar(
            state = it,
            progressMs = 3200L,
            isPlaying = isPlaying.value,
            togglePlayback = { isPlaying.value = !isPlaying.value },
            speed = PlaybackSpeed.X1_00,
            updateSpeed = {},
        )
    }
}

@Preview
@Composable
fun PlayerToolbarCompactPreview() {
    val isPlaying = remember { mutableStateOf(false) }
    ToolbarPreviewScaffold(dimensions = compactPlayerDimensions()) {
        PlayerToolbar(
            state = it,
            progressMs = 3200L,
            isPlaying = isPlaying.value,
            togglePlayback = { isPlaying.value = !isPlaying.value },
            speed = PlaybackSpeed.X1_00,
            updateSpeed = {},
        )
    }
}

@Preview
@Composable
fun PlayerToolbarPlayingPreview() {
    ToolbarPreviewScaffold {
        PlayerToolbar(
            state = it,
            progressMs = 6500L,
            isPlaying = true,
            togglePlayback = {},
            speed = PlaybackSpeed.X0_50,
            updateSpeed = {},
        )
    }
}
