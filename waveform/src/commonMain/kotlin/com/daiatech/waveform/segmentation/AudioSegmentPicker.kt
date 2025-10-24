package com.daiatech.waveform.segmentation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.times
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.models.WaveformColors
import com.daiatech.waveform.models.waveformColors
import com.daiatech.waveform.safeDiv
import com.daiatech.waveform.segmentation.component.SegmentPickerToolbar
import com.daiatech.waveform.times
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *
 * @param colors Custom colors for waveform elements.
 * @param waveformAlignment Alignment of spikes relative to the graph height.
 * @param spikeAnimationSpec Animation specification for spike height transitions.
 * @param progressMs Current time progress in milliseconds.
 */
@Composable
fun AudioSegmentPicker(
    state: SegmentPickerState,
    colors: WaveformColors = waveformColors(),
    progressMs: Long,
    isPlaying: Boolean,
    speed: Float,
    togglePlayback: () -> Unit,
    updateSpeed: (Float) -> Unit,
    availableSpeeds: List<Float> = listOf(0.25f, 0.5f, 1f),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val markersTextStyle = remember {
        TextStyle(fontSize = markerFontSize, color = colors.markerColor)
    }

    var screenWidth by remember { mutableIntStateOf(0) }
    val screenWidthDp by derivedStateOf { with(density) { screenWidth.toDp() } }

    val durationMs = state.durationMs
    val canvasWidthDp = state.canvasWidthDp.value
    val spikeWidthPx = state.spikeWidthPx
    val layout = state.layout
    val spikeCornerRadius = state.spikeCornerRadius
    val windowCornerRadius = state.windowCornerRadius
    val enableZoomIn = state.enableZoomIn.value
    val enableZoomOut = state.enableZoomOut.value
    val timestampMs = state.timestampMs.value
    val drawableAmplitudes = state.drawableAmplitudes.value
    val window = state.window.value
    val spikeCountPerTimestampMs = state.spikeCountPerTimestampMs

    val canvasOffset by animateDpAsState(
        targetValue = screenWidthDp / 2 - ((progressMs.toFloat() safeDiv durationMs.toFloat())
            .coerceIn(0f, 1f)) * canvasWidthDp,
        label = "canvasOffset"
    )

    val centerMarkerPath = remember(screenWidth) {
        if (screenWidth == 0) return@remember Path()

        with(density) {
            val centerX = screenWidth / 2
            val vSpacing = verticalItemSpacing.toPx()
            val spikeW = spikeWidthPx
            val totalHeight =
                (vSpacing * 7 + MIN_GRAPH_HEIGHT.toPx() + 2 * markerFontSize.toPx())

            Path().apply {
                moveTo(centerX - vSpacing, 0f)
                lineTo(centerX + vSpacing, 0f)
                lineTo(centerX + spikeW, vSpacing)
                lineTo(centerX + spikeW, totalHeight)
                lineTo(centerX + vSpacing, totalHeight + vSpacing)
                lineTo(centerX - vSpacing, totalHeight + vSpacing)
                lineTo(centerX - spikeW, totalHeight)
                lineTo(centerX - spikeW, vSpacing)
                close()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    screenWidth = layoutCoordinates.size.width
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(colors.fadeColor, Color.Transparent)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width / 2, size.height)
                    )
                    drawPath(
                        path = centerMarkerPath,
                        brush = SolidColor(colors.primaryProgressColor)
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .width(canvasWidthDp)
                    .height(layout.canvasHeight)
                    .offset(canvasOffset)
            ) {
                drawableAmplitudes.forEachIndexed { index, amplitude ->
                    drawRoundRect(
                        brush = SolidColor(colors.waveformColor),
                        topLeft = amplitude.first,
                        size = Size(spikeWidthPx, amplitude.second),
                        cornerRadius = spikeCornerRadius,
                        style = Fill
                    )

                    // Draw timestamp markers
                    if (index % spikeCountPerTimestampMs == 0) {
                        drawRoundRect(
                            brush = SolidColor(colors.markerColor),
                            topLeft = Offset(amplitude.first.x, layout.markerY),
                            size = Size(spikeWidthPx, layout.markerHeight),
                            cornerRadius = spikeCornerRadius,
                            style = Fill
                        )

                        val timeInSeconds =
                            (index * timestampMs) / (spikeCountPerTimestampMs * 1000f)
                        val timeText = "${timeInSeconds}s"
                        val tm = textMeasurer.measure(timeText, markersTextStyle)
                        val y = if (index % (2 * spikeCountPerTimestampMs) == 0) {
                            layout.evenMarkerY
                        } else {
                            layout.oddMarkerY
                        }

                        drawText(
                            textMeasurer = textMeasurer,
                            style = markersTextStyle,
                            text = timeText,
                            topLeft = Offset(amplitude.first.x - (tm.size.width.toFloat() / 2), y),
                            size = Size(
                                width = tm.size.width.toFloat(),
                                height = tm.size.height.toFloat()
                            )
                        )
                    }
                }

                window?.let { window ->
                    drawRoundRect(
                        brush = SolidColor(colors.activeWindowColor),
                        topLeft = window.first,
                        size = window.second,
                        cornerRadius = windowCornerRadius,
                        style = Stroke(spikeWidthPx)
                    )
                }
            }
        }

        SegmentPickerToolbar(
            colors = colors,
            progressMs = progressMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            enableZoomIn = enableZoomIn,
            enableZoomOut = enableZoomOut,
            speed = speed,
            togglePlayback = togglePlayback,
            onZoomIn = state::zoomIn,
            onZoomOut = state::zoomOut,
            updateSpeed = updateSpeed,
            availableSpeeds = availableSpeeds
        )
    }
}

@Composable
fun AudioSegmentPickerPreview(
    colors: WaveformColors = waveformColors()
) {
    val coroutineScope = rememberCoroutineScope()
    val progressJobRef = remember { mutableStateOf<Job?>(null) }

    // Keep playback related variables/methods out of state
    var progressMs by remember { mutableLongStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }

    val state = rememberSegmentPickerState(
        amplitudes = listOf(100, 200, 300, 500, 100, 20).times(20),
        durationMs = 8000,
        minimumSegmentMs = 500,
    )

    Surface(color = colors.containerColor) {
        Column {
            AudioSegmentPicker(
                state = state,
                colors = colors,
                progressMs = progressMs,
                isPlaying = isPlaying,
                togglePlayback = {
                    if (isPlaying) {
                        progressJobRef.value?.cancel()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        progressJobRef.value = coroutineScope.launch {
                            while (progressMs < 8000L && isPlaying) {
                                progressMs += (100.times(speed)).toLong()
                                delay(100)
                            }
                            if (progressMs >= 8000L) {
                                progressMs = 0L
                            }
                            isPlaying = false
                        }
                    }
                },
                updateSpeed = { speed = it },
                speed = speed
            )

            Button(onClick = { state.addSegment(progressMs) }) {
                Text("Add Segment")
            }
        }
    }
}
