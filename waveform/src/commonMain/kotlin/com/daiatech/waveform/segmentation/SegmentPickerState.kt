package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.toDrawableAmplitudes
import kotlin.math.pow

/**
 * Number of amplitude spikes between two timestamp markers.
 */
internal const val spikeCountPerTimestampMs: Int = 10

/**
 * Font size for timestamp markers
 */
internal val markerFontSize: TextUnit = 12.sp

/**
 * Vertical spacing between elements
 */
internal val verticalItemSpacing: Dp = 8.dp

class SegmentPickerState(
    density: Density,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    val amplitudes: List<Int>,
    val durationMs: Long
) {

    val spikeRadiusPx = with(density) {
        spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp).toPx()
    }
    val spikeCornerRadius = CornerRadius(spikeRadiusPx, spikeRadiusPx)
    val spikeWidthPx = with(density) {
        spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp).toPx()
    }
    val spikeTotalWidthPx = with(density) { (spikeWidth + spikePadding).toPx() }
    val graphHeight = with(density) { MIN_GRAPH_HEIGHT.toPx() }

    private var zoom = mutableIntStateOf(1) // 1x, 2x, 3x, 4x, 5x
    val timestampMs = derivedStateOf { ((500 / 2.0.pow((zoom.value - 1).toDouble())).toLong()) }
    val enableZoomIn = derivedStateOf { zoom.value != 5 }
    val enableZoomOut = derivedStateOf { zoom.value != 1 }
    val noOfSpikes = derivedStateOf {
        ((durationMs * spikeCountPerTimestampMs) / timestampMs.value).toInt()
    }
    val canvasWidthPx = derivedStateOf { spikeTotalWidthPx * noOfSpikes.value }
    val canvasWidthDp = derivedStateOf { with(density) { canvasWidthPx.value.toDp() } }

    val drawableAmplitudes = derivedStateOf {
        val amps = amplitudes.toDrawableAmplitudes(
            amplitudeType = AmplitudeType.AVG,
            spikes = noOfSpikes.value,
            minHeight = MIN_SPIKE_HEIGHT,
            maxHeight = with(density) { MIN_GRAPH_HEIGHT.toPx() }
        ).mapIndexed { idx, amp ->
            val x = idx * spikeTotalWidthPx
            val y = when (waveformAlignment) {
                WaveformAlignment.Top -> 0f
                WaveformAlignment.Bottom -> graphHeight - amp
                WaveformAlignment.Center -> (graphHeight - amp) / 2f
            } + layout.spikesOffset
            Offset(x, y) to amp
        }
        println("Amps:$amps")
        amps
    }

    internal val layout = with(density) {
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


    fun zoomIn() {
        if (zoom.value < 5) {
            zoom.value += 1
        }
    }

    fun zoomOut() {
        if (zoom.value > 1) {
            zoom.value -= 1
        }
    }

}

@Composable
fun rememberSegmentPickerState(
    amplitudes: List<Int>,
    durationMs: Long
): SegmentPickerState {
    val density = LocalDensity.current
    return remember {
        SegmentPickerState(
            density = density,
            spikeWidth = 2.dp,
            spikeRadius = 2.dp,
            spikePadding = 2.dp,
            amplitudes = amplitudes,
            durationMs = durationMs
        )
    }
}