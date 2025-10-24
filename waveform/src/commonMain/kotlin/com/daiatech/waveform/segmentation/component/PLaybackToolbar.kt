package com.daiatech.waveform.segmentation.component

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.Res
import com.daiatech.waveform.ic_pause
import com.daiatech.waveform.ic_play_arrow
import com.daiatech.waveform.millisecondsToMmSs
import com.daiatech.waveform.segmentation.SegmentationColors
import com.daiatech.waveform.segmentation.segmentationColors
import org.jetbrains.compose.resources.painterResource

@Composable
fun PLaybackToolbar(
    modifier: Modifier = Modifier,
    colors: SegmentationColors,
    progressMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    enableZoomIn: Boolean,
    enableZoomOut: Boolean,
    speed: Float,
    togglePlayback: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    updateSpeed: (Float) -> Unit,
    availableSpeeds: List<Float>
) {
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Speed", color = colors.contentPrimary)
            Spacer(Modifier.height(8.dp))
            SpeedButton(
                availableSpeeds = availableSpeeds,
                selectedSpeedIdx = availableSpeeds.indexOf(speed),
                onSpeedUpdate = {
                    val speed = availableSpeeds.getOrNull(it)
                    if (speed != null) {
                        updateSpeed(speed)
                    }
                },
                containerColor = Color.White.copy(0.1f),
                modifier = Modifier.height(48.dp).fillMaxWidth()
            )
        }


        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(48.dp)
                    .background(colors.waveformColor)
                    .clickable { togglePlayback() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource((if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play_arrow)),
                    contentDescription = null,
                    tint = colors.background
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${millisecondsToMmSs(progressMs)}/${millisecondsToMmSs(durationMs)}",
                color = colors.waveformColor
            )
        }

        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Zoom", color = colors.waveformColor)
            Spacer(Modifier.height(8.dp))
            ZoomButton(
                modifier = Modifier.height(48.dp).fillMaxWidth(),
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                enableZoomOut = enableZoomOut,
                enableZoomIn = enableZoomIn
            )
        }
    }
}

@Composable
fun PLaybackToolbarPreview() {
    PLaybackToolbar(
        colors = segmentationColors(),
        progressMs = 30000L,
        durationMs = 60000L,
        isPlaying = false,
        enableZoomIn = true,
        enableZoomOut = true,
        speed = 1.0f,
        togglePlayback = {},
        onZoomIn = {},
        onZoomOut = {},
        updateSpeed = {},
        availableSpeeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    )
}
