package com.daiatech.waveform.marker

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.distanceFrom
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.minSpikePaddingDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.removeCircleRadius
import com.daiatech.waveform.safeDiv
import com.daiatech.waveform.toDrawableAmplitudes

// FIXME: Make marker List<Long>
@Composable
fun AudioMarkerUi(
    modifier: Modifier = Modifier,
    durationMs: Long,
    progressMs: Long,
    graphHeight: Dp = MIN_GRAPH_HEIGHT,
    style: DrawStyle = Fill,
    waveformBrush: Brush = SolidColor(Color.White),
    progressBrush: Brush = SolidColor(Color.Blue),
    markerBrush: Brush = SolidColor(Color.Red),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.AVG,
    spikeWidth: Dp = 2.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    amplitudes: List<Int>,
    windowSize: Float = 0.2F,
    markers: List<Float>,
    addMarker: (Float) -> Unit,
    removeMarker: (idx: Int) -> Unit
) {

    var canvasSize by remember { mutableStateOf(Size(0F, 0F)) }
    var zoomedCanvasSize by remember { mutableStateOf(Size(0F, 0F)) }
    var windowOffset by remember { mutableStateOf(0F) }
    val _windowOffset by animateFloatAsState(windowOffset, label = "window offset")
    val zoomedAmps = remember(windowOffset, amplitudes) {
        val start = amplitudes.size.times(windowOffset)
            .toInt().coerceIn(0, amplitudes.size)
        val end = amplitudes.size.times(windowOffset + windowSize)
            .toInt().coerceIn(0, amplitudes.size)
        amplitudes.subList(start, end)
    }

    Column(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                canvasSize = Size(it.size.width.toFloat(), canvasSize.height)
            }
    ) {
        // Base Waveform
        AudioWaveform(
            modifier = Modifier
                .pointerInteropFilter {
                    return@pointerInteropFilter when (it.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            if (it.x in 0F..canvasSize.width) {
                                val maximumWindowOffset = canvasSize.width.times(1 - windowSize)
                                windowOffset = if (it.x in maximumWindowOffset..canvasSize.width) {
                                    (maximumWindowOffset / canvasSize.width)
                                } else {
                                    (it.x / canvasSize.width)
                                }
                                true
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
                }
                .onGloballyPositioned {
                    canvasSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
                },
            height = graphHeight,
            style = style,
            waveformBrush = waveformBrush,
            markerBrush = markerBrush,
            waveformAlignment = waveformAlignment,
            amplitudeType = amplitudeType,
            spikeWidth = spikeWidth,
            spikeRadius = spikeRadius,
            spikePadding = spikePadding,
            amplitudes = amplitudes,
            markers = markers,
            drawOnTop = {
                // draw window on top
                drawRoundRect(
                    brush = SolidColor(Color.Gray.copy(alpha = 0.4f)),
                    size = Size(
                        width = size.width.times(windowSize),
                        height = size.height
                    ),
                    topLeft = Offset(
                        x = size.width.times(_windowOffset),
                        y = 0f
                    ),
                    cornerRadius = CornerRadius(12F, 12F)
                )

                drawRoundRect(
                    brush = progressBrush,
                    size = Size(
                        width = 2.dp.toPx(),
                        height = size.height
                    ),
                    topLeft = Offset(
                        x = progressMs.safeDiv(durationMs) * size.width,
                        y = 0f
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Zoomed-in Waveform
        AudioWaveform(
            modifier = Modifier
                .pointerInput(markers, windowSize, windowOffset, canvasSize) {
                    val radius = removeCircleRadius.toPx()
                    detectTapGestures { point ->
                        val tappedMarkerIdx = markers.indexOfFirst { marker ->
                            val xCoordinate =
                                canvasSize.width * ((marker - windowOffset) / (windowSize)) + (spikeWidth.toPx() / 2)
                            val center = Offset(
                                x = xCoordinate,
                                y = (canvasSize.height / 2)
                            )
                            val distance = center.distanceFrom(point)
                            distance < radius.times(1.2) // increase touch proximity by +0.2 times for better touch target
                        }

                        if (tappedMarkerIdx == -1) {
                            val newMarker = (point.x / canvasSize.width) * windowSize + windowOffset
                            addMarker(newMarker)
                        } else {
                            removeMarker(tappedMarkerIdx)
                        }
                    }
                }
                .onGloballyPositioned {
                    zoomedCanvasSize = Size(it.size.width.toFloat(), it.size.height.toFloat())
                },
            height = graphHeight,
            style = style,
            waveformBrush = waveformBrush,
            markerBrush = markerBrush,
            waveformAlignment = waveformAlignment,
            amplitudeType = amplitudeType,
            spikeWidth = spikeWidth,
            spikeRadius = spikeRadius,
            spikePadding = spikePadding,
            amplitudes = zoomedAmps,
            markers = markers
                .filter { it in windowOffset..(windowOffset + windowSize) }
                .map { (it - windowOffset) / (windowSize) },
            drawOnTop = {}
        )
    }
}


@Composable
internal fun AudioWaveform(
    modifier: Modifier,
    height: Dp,
    style: DrawStyle,
    waveformBrush: Brush,
    markerBrush: Brush,
    waveformAlignment: WaveformAlignment,
    amplitudeType: AmplitudeType,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    amplitudes: List<Int>,

    // extra params
    markers: List<Float>,
    drawOnTop: DrawScope.() -> Unit
) {
    val _spikeWidth = remember(spikeWidth) {
        spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp)
    }
    val _spikePadding = remember(spikePadding) {
        spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp)
    }
    val _spikeRadius = remember(spikeRadius) {
        spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp)
    }
    val _spikeTotalWidth = remember(spikeWidth, spikePadding) { _spikeWidth + _spikePadding }
    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    var spikes by remember { mutableStateOf(0F) }

    val spikesAmplitudes = remember(amplitudes, spikes, amplitudeType) {
        val maxHeight = canvasSize.height.coerceAtLeast(MIN_SPIKE_HEIGHT)
        amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes.toInt(),
            minHeight = MIN_SPIKE_HEIGHT,
            maxHeight = maxHeight
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(height)
            .then(modifier)
    ) {
        canvasSize = size
        spikes = size.width / _spikeTotalWidth.toPx()

        spikesAmplitudes.forEachIndexed { index, amplitude ->
            drawRoundRect(
                brush = waveformBrush,
                topLeft = Offset(
                    x = index * _spikeTotalWidth.toPx(),
                    y = when (waveformAlignment) {
                        WaveformAlignment.Top -> 0F
                        WaveformAlignment.Bottom -> size.height - amplitude
                        WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
                    }
                ),
                size = Size(
                    width = _spikeWidth.toPx(),
                    height = amplitude
                ),
                cornerRadius = CornerRadius(_spikeRadius.toPx(), _spikeRadius.toPx()),
                style = style
            )
        }

        markers.forEach { loc ->
            val xCoordinate = size.width.times(loc).coerceIn(0F, size.width)
            drawRoundRect(
                brush = markerBrush,
                topLeft = Offset(
                    x = xCoordinate,
                    y = 0f
                ),
                size = Size(
                    width = _spikeWidth.toPx(),
                    height = size.height
                ),
                cornerRadius = CornerRadius(_spikeRadius.toPx(), _spikeRadius.toPx())
            )
        }
        drawOnTop()
    }
}

@Preview
@Composable
private fun AudioMarkerUiPrev() {
    AudioMarkerUi(
        amplitudes = listOf(100, 200, 300, 500, 100, 20),
        progressMs = 500,
        durationMs = 1000,
        markers = listOf(50F, 100F, 400F),
        addMarker = {},
        removeMarker = {}
    )
}