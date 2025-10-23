package com.daiatech.waveform.segmentation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.Res
import com.daiatech.waveform.ic_pause
import com.daiatech.waveform.ic_play_arrow
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.millisecondsToMmSs
import com.daiatech.waveform.minSpikePaddingDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.models.WaveformColors
import com.daiatech.waveform.models.waveformColors
import com.daiatech.waveform.safeDiv
import com.daiatech.waveform.segmentation.component.SegmentPickerToolbar
import com.daiatech.waveform.segmentation.component.SpeedButton
import com.daiatech.waveform.segmentation.component.ZoomButton
import com.daiatech.waveform.toDrawableAmplitudes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.pow

/**
 * Width of the amplitude spike
 */
private val spikeWidth: Dp = 2.dp

/**
 * Radii of amplitude spikes
 */
private val spikeRadius: Dp = 2.dp

/**
 * Gap between two consecutive spikes
 */
private val spikePadding: Dp = 2.dp

/**
 * Vertical spacing between elements
 */
private val verticalItemSpacing: Dp = 8.dp

/**
 * Font size for timestamp markers
 */
private val markerFontSize: TextUnit = 12.sp


/**
 * Displays an animated amplitude bar graph (waveform) with a fixed center line representing
 * the current progress in time. The waveform scrolls horizontally as the progress updates.
 *
 * Typically used for visualizing audio input or playback.
 *
 * @param colors Custom colors for waveform elements.
 * @param waveformAlignment Alignment of spikes relative to the graph height.
 * @param spikeAnimationSpec Animation specification for spike height transitions.
 * @param amplitudes List of raw amplitude values sampled at 50ms intervals.
 * @param progressMs Current time progress in milliseconds.
 * @param durationMs Total duration of the waveform in milliseconds.
 * @param timestampMs Duration between two timestamp markers in milliseconds.
 * @param spikeCountPerTimestampMs Number of amplitude spikes between two timestamp markers.
 */
@Composable
fun AudioSegmentPicker(
    colors: WaveformColors = waveformColors(),
    amplitudes: List<Int>,
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
    availableSpeeds: List<Float> = listOf(0.25f, 0.5f, 1f),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
    timestampMs: Long = 100,
    spikeCountPerTimestampMs: Int = 10,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val spikeRadius = spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp)
    val spikeWidth = spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp)
    val spikePadding = spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp)
    val spikeTotalWidth = spikeWidth + spikePadding

    val layout = remember(verticalItemSpacing, markerFontSize, density) {
        with(density) {
            val triangleSpace = verticalItemSpacing.toPx()
            val textHeight = markerFontSize.toPx()
            val graphHeight = MIN_GRAPH_HEIGHT.toPx()

            WaveformLayout(
                canvasHeight = (triangleSpace * 2 + textHeight + triangleSpace +
                        triangleSpace + graphHeight + triangleSpace +
                        triangleSpace + textHeight + triangleSpace + triangleSpace).toDp(),
                evenMarkerY = triangleSpace * 2,
                oddMarkerY = triangleSpace * 2 + textHeight + triangleSpace +
                        triangleSpace + graphHeight + triangleSpace,
                markerY = triangleSpace * 2 + textHeight + triangleSpace,
                markerHeight = triangleSpace + graphHeight + triangleSpace,
                spikesOffset = triangleSpace * 2 + textHeight + triangleSpace * 2
            )
        }
    }

    val markersTextStyle = remember(markerFontSize, colors.markerColor) {
        TextStyle(fontSize = markerFontSize, color = colors.markerColor)
    }

    val noOfSpikes = remember(timestampMs, spikeCountPerTimestampMs, durationMs) {
        ((durationMs * spikeCountPerTimestampMs) / timestampMs).toInt()
    }

    val canvasWidthDp = remember(noOfSpikes, spikeTotalWidth) {
        spikeTotalWidth * noOfSpikes
    }

    val drawableAmplitudes = remember(amplitudes, noOfSpikes, density) {
        amplitudes.toDrawableAmplitudes(
            amplitudeType = AmplitudeType.AVG,
            spikes = noOfSpikes,
            minHeight = MIN_SPIKE_HEIGHT,
            maxHeight = with(density) { MIN_GRAPH_HEIGHT.toPx() }
        )
    }

    var screenWidthDp by remember { mutableStateOf(0.dp) }

    val canvasOffset by animateDpAsState(
        targetValue = screenWidthDp / 2 - ((progressMs.toFloat() safeDiv durationMs.toFloat())
            .coerceIn(0f, 1f)) * canvasWidthDp,
        label = "canvasOffset"
    )

    val centerMarkerPath =
        remember(screenWidthDp, verticalItemSpacing, spikeWidth, markerFontSize, density) {
            if (screenWidthDp == 0.dp) return@remember Path()

            with(density) {
                val centerX = screenWidthDp.toPx() / 2
                val vSpacing = verticalItemSpacing.toPx()
                val spikeW = spikeWidth.toPx()
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
                    screenWidthDp = with(density) { layoutCoordinates.size.width.toDp() }
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
                Modifier
                    .width(canvasWidthDp)
                    .height(layout.canvasHeight)
                    .offset(x = canvasOffset)
            ) {
                val spikeWidthPx = spikeWidth.toPx()
                val spikeTotalWidthPx = spikeTotalWidth.toPx()
                val spikeRadiusPx = spikeRadius.toPx()
                val cornerRadius = CornerRadius(spikeRadiusPx, spikeRadiusPx)
                val graphHeight = MIN_GRAPH_HEIGHT.toPx()

                drawableAmplitudes.forEachIndexed { index, amplitude ->
                    val x = index * spikeTotalWidthPx

                    // Draw waveform spike
                    val spikeY = when (waveformAlignment) {
                        WaveformAlignment.Top -> 0f
                        WaveformAlignment.Bottom -> graphHeight - amplitude
                        WaveformAlignment.Center -> (graphHeight - amplitude) / 2f
                    } + layout.spikesOffset

                    drawRoundRect(
                        brush = SolidColor(colors.waveformColor),
                        topLeft = Offset(x, spikeY),
                        size = Size(spikeWidthPx, amplitude),
                        cornerRadius = cornerRadius,
                        style = Fill
                    )

                    // Draw timestamp markers
                    if (index % spikeCountPerTimestampMs == 0) {
                        drawRoundRect(
                            brush = SolidColor(colors.markerColor),
                            topLeft = Offset(x, layout.markerY),
                            size = Size(spikeWidthPx, layout.markerHeight),
                            cornerRadius = cornerRadius,
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
                            text = timeInSeconds.toString(),
                            topLeft = Offset(x - (tm.size.width.toFloat() / 2), y),
                            size = Size(
                                width = tm.size.width.toFloat(),
                                height = tm.size.height.toFloat()
                            )
                        )
                    }
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
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
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
    var progressMs by remember { mutableLongStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var zoom by remember { mutableIntStateOf(1) }
    var speed by remember { mutableFloatStateOf(1f) }

    Surface(color = colors.containerColor) {
        Column {
            AudioSegmentPicker(
                colors = colors,
                amplitudes = listOf(
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20,
                    100, 200, 300, 500, 100, 20
                ),
                durationMs = 4000,
                progressMs = progressMs,
                timestampMs = ((500 / 2.0.pow((zoom - 1).toDouble())).toLong()),
                isPlaying = isPlaying,
                togglePlayback = {
                    if (isPlaying) {
                        progressJobRef.value?.cancel()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        progressJobRef.value = coroutineScope.launch {
                            while (progressMs < 4000L && isPlaying) {
                                progressMs += (100.times(speed)).toLong()
                                delay(100)
                            }
                            if (progressMs >= 4000L) {
                                progressMs = 0L
                            }
                            isPlaying = false
                        }
                    }
                },
                onZoomIn = {
                    if (zoom < 5) {
                        zoom += 1
                    }
                },
                onZoomOut = {
                    if (zoom > 1) {
                        zoom -= 1
                    }
                },
                enableZoomOut = zoom != 1,
                enableZoomIn = zoom != 5,
                updateSpeed = { speed = it },
                speed = speed
            )
        }
    }
}
