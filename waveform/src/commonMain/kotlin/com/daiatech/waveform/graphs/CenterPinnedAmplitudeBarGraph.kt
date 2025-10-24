package com.daiatech.waveform.graphs

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.models.WaveformColors
import com.daiatech.waveform.models.waveformColors
import com.daiatech.waveform.toDrawableAmplitudes
import org.jetbrains.compose.ui.tooling.preview.Preview

const val timeIntervalMs = 500
const val spikeIntervalMs = 1000 / 20
val graphBarHeight = 48.dp
val bottomSpaceHeight = 16.dp
val canvasHeight = graphBarHeight + bottomSpaceHeight
const val timeIntervalSpikes = timeIntervalMs / spikeIntervalMs

/**
 * Displays an animated amplitude bar graph (waveform) with a fixed center line representing
 * the current progress in time. The waveform scrolls horizontally as the progress updates.
 *
 * Typically used for visualizing audio input or playback.
 *
 * @param modifier Modifier applied to the root layout.
 * @param style Style used to draw waveform spikes (e.g., [Fill]).
 * @param colors Custom colors for waveform elements.
 * @param waveformAlignment Alignment of spikes relative to the graph height. Can be [WaveformAlignment.Top], [WaveformAlignment.Center], or [WaveformAlignment.Bottom].
 * @param amplitudeType Defines how to aggregate amplitude values, e.g., average or max, using [AmplitudeType].
 * @param spikeAnimationSpec Animation specification for spike height transitions.
 * @param spikeWidth Width of each individual amplitude spike.
 * @param spikeRadius Corner radius for each spike (used for rounded corners).
 * @param spikePadding Padding between adjacent spikes.
 * @param progressMs Current time progress in milliseconds. The waveform scrolls to center this timestamp.
 * @param amplitudes List of raw amplitude values sampled at 50ms intervals.
 * @param durationMs Total duration of the waveform in milliseconds.
 * @param start Optional start time (in ms) for rendering the waveform. Defaults to `0` if `null`.
 * @param end Optional end time (in ms) for rendering the waveform. Defaults to `durationMs` if `null`.
 */
@Suppress("LocalVariableName")
@Composable
fun CenterPinnedAmplitudeBarGraph(
    modifier: Modifier = Modifier,
    style: DrawStyle = Fill,
    colors: WaveformColors = waveformColors(),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.AVG,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
    spikeWidth: Dp = 4.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 3.dp,
    progressMs: Long = 0L,
    amplitudes: List<Int>,
    durationMs: Long,
    start: Long? = null,
    end: Long? = null
) {
    val density = LocalDensity.current

    val _spikeRadius = remember(spikeRadius) {
        spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp)
    }
    val _spikeWidth = remember(spikeWidth) { spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp) }
    val _spikePadding = remember(spikePadding) { spikePadding.coerceIn(1.dp, maxSpikePaddingDp) }
    val _spikeTotalWidth = remember(spikeWidth, spikePadding) { _spikeWidth + _spikePadding }

    val startRange = remember(start) { start ?: 0 }
    val endRange = remember(end, durationMs) { end ?: durationMs }

    // Number of spikes for 0.5s
    val spikesCount = remember(startRange, endRange, spikeIntervalMs) {
        ((endRange - startRange) / spikeIntervalMs).toInt()
    }

    val spikesAmplitudes = remember(amplitudes, spikesCount, amplitudeType) {
        amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikesCount,
            minHeight = MIN_SPIKE_HEIGHT,
            maxHeight = with(density) { graphBarHeight.toPx() }.coerceAtLeast(MIN_SPIKE_HEIGHT)
        )
    }.map { animateFloatAsState(it, spikeAnimationSpec, label = "spike amplitudes").value }

    val canvasWidth = remember(spikesCount, _spikeTotalWidth) { (spikesCount * (_spikeTotalWidth)) }
    val textMeasurer = rememberTextMeasurer()
    val markersTextStyle = remember { TextStyle(fontSize = 12.sp, color = colors.markerColor) }

    var screenWidthDp by remember { mutableStateOf(0.dp) }
    var canvasOffset by remember(screenWidthDp) { mutableStateOf(screenWidthDp / 2) }
    LaunchedEffect(progressMs, screenWidthDp, durationMs) {
        val p = (
                if (start != null && end != null) {
                    (progressMs - start) / (end - start).toFloat()
                } else if (durationMs != 0L) {
                    progressMs.toFloat() / durationMs.toFloat()
                } else {
                    0F
                }
                ).coerceIn(0F, 1F)

        canvasOffset = screenWidthDp / 2 - (p * canvasWidth)
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            Slider(
                value = progressMs.toFloat(),
                onValueChange = { },
                modifier = Modifier
                    .weight(1f)
                    .padding()
                    .height(16.dp),
                valueRange = startRange.toFloat()..endRange.toFloat()
            )
        }

        Box {
            // Progress line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(graphBarHeight)
                    .clip(CircleShape)
                    .background(colors.primaryProgressColor)
                    .align(Alignment.TopCenter)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { layoutCoordinates ->
                        screenWidthDp = with(density) { layoutCoordinates.size.width.toDp() }
                    }
                    .clipToBounds()

            ) {
                Canvas(
                    modifier = Modifier
                        .height(canvasHeight)
                        .width(canvasWidth)
                        .offset(x = canvasOffset)
                ) {
                    spikesAmplitudes.forEachIndexed { index, amplitude ->
                        val x = index * (_spikeTotalWidth.toPx())
                        drawRoundRect(
                            brush = SolidColor(colors.waveformColor),
                            topLeft = Offset(
                                x = x,
                                y = when (waveformAlignment) {
                                    WaveformAlignment.Top -> 0F
                                    WaveformAlignment.Bottom -> graphBarHeight.toPx() - amplitude
                                    WaveformAlignment.Center -> graphBarHeight.toPx() / 2F - amplitude / 2F
                                }
                            ),
                            size = Size(
                                width = _spikeWidth.toPx(),
                                height = amplitude
                            ),
                            cornerRadius = CornerRadius(_spikeRadius.toPx(), _spikeRadius.toPx()),
                            style = style
                        )

                        // Draw vertical line and time label every 0.5 seconds
                        if (index % timeIntervalSpikes == 0) {
                            val timeInSeconds = (index * spikeIntervalMs) / 1000f
                            val tm =
                                textMeasurer.measure(timeInSeconds.toString(), markersTextStyle)
                            if (timeInSeconds <= durationMs / 1000f) {
                                // Draw vertical line
                                drawLine(
                                    color = colors.markerColor,
                                    start = Offset(x = x, y = 0f),
                                    end = Offset(x = x, y = graphBarHeight.toPx()),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    style = markersTextStyle,
                                    text = timeInSeconds.toString(),
                                    topLeft = Offset(x, graphBarHeight.toPx()),
                                    size = Size(
                                        width = tm.size.width.toFloat(),
                                        height = tm.size.height.toFloat()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CenterPinnedAmplitudeBarGraphPrev() {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface
    ) {
        CenterPinnedAmplitudeBarGraph(
            amplitudes = listOf(100, 200, 300, 500, 100, 20),
            durationMs = 2000,
            progressMs = 1000
        )
    }
}


@Preview
@Composable
private fun CenterPinnedAmplitudeBarGraphPrev2() {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface
    ) {
        CenterPinnedAmplitudeBarGraph(
            amplitudes = listOf(100, 200, 300, 500, 100, 20),
            durationMs = 2000
        )
    }
}
