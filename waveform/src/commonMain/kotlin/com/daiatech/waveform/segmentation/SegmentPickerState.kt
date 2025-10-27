package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.daiatech.waveform.models.Segment
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.toDrawableAmplitudes
import kotlin.math.pow

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
    waveformAlignment: WaveformAlignment,
    val amplitudes: List<Int>,
    val durationMs: Long,
    val minimumSegmentDuration: Long,
    val inactive: List<Segment>
) {

    val spikeRadiusPx = with(density) {
        spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp).toPx()
    }
    val windowRadiusPx = with(density) { 8.dp.toPx() }
    val spikeCornerRadius = CornerRadius(spikeRadiusPx, spikeRadiusPx)
    val windowCornerRadius = CornerRadius(windowRadiusPx, windowRadiusPx)
    val spikeWidthPx = with(density) {
        spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp).toPx()
    }
    val spikeTotalWidthPx = with(density) { (spikeWidth + spikePadding).toPx() }

    /**
     * Height of the amplitude spike graph
     */
    val graphHeightPx = with(density) { MIN_GRAPH_HEIGHT.toPx() }

    /**
     *
     */
    private var zoom = mutableIntStateOf(1) // 1x, 2x, 3x, 4x, 5x

    /**
     * Duration in milliseconds between two timestamp markers.
     */
    val timestampMs = derivedStateOf { ((500 / 2.0.pow((zoom.value - 1).toDouble())).toLong()) }

    /**
     * No of spikes between two timestamp markers.
     */
    val spikeCountPerTimestampMs: Int = 10
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
                WaveformAlignment.Bottom -> graphHeightPx - amp
                WaveformAlignment.Center -> (graphHeightPx - amp) / 2f
            } + layout.spikesOffset
            Offset(x, y) to amp
        }
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

    private val _segment = mutableStateOf<Segment?>(null)
    val segment: State<Segment?> = _segment


    val window = derivedStateOf {
        _segment.value?.let {
            val startPx = durationToPx(it.start)
            val endPx = durationToPx(it.end)
            // canvasWidth
            // topLeft and size dependent on zoom
            Offset(x = startPx, y = layout.spikesOffset) to Size(
                width = endPx - startPx,
                height = graphHeightPx
            )
        }
    }

    /**
     * px
     * = width for 1ms * dur
     * = ([canvasWidthPx] / [durationMs]) * dur
     * = ([spikeTotalWidthPx] * [noOfSpikes] / [durationMs] ) * dur
     * = ([spikeTotalWidthPx] * ([durationMs] * [spikeCountPerTimestampMs] / [timestampMs])) / [durationMs] * dur
     * = ([spikeTotalWidthPx] * [spikeCountPerTimestampMs] / [timestampMs]) * dur
     */
    fun durationToPx(dur: Long): Float {
        return (spikeTotalWidthPx * spikeCountPerTimestampMs * dur) / timestampMs.value
    }

    /**
     * dur
     * = dur in 1 px * pixels
     * = (duration/waveformWidth) * pixels
     * = (duration/(spikeTotalWidthPx * totalSpikesCount)) * pixels
     * = (duration/(spikeTotalWidth * (duration * spikeCountPerTimestampMp)/TimestampMs) * pixels
     * = (timestampMp * dragAmount)/(spikeTotalWidth * spikeCountPerTimestampMp)
     */
    fun pxToDuration(px: Float): Long {
        return ((timestampMs.value * px) / (spikeTotalWidthPx * spikeCountPerTimestampMs)).toLong()
    }

    fun addSegment(start: Long) {
        if (_segment.value != null) {
            println("Cannot add segment, start is before last inactive end")
            return
        }
        val lastInactive = inactive.lastOrNull()
        if(lastInactive != null && start < lastInactive.end) {
            println("Cannot add segment, start is before last inactive end")
            return
        }
        val end = (start + minimumSegmentDuration)
        if (end > durationMs) return
        _segment.value = Segment(start, end)
    }

    fun removeSegment() {
        _segment.value = null
    }

    fun addToStart(by: Int) {
        val current = _segment.value ?: return
        val minStart = inactive.lastOrNull()?.end ?: 0
        val newStart = (current.start + by).coerceIn(minStart, current.end - minimumSegmentDuration)
        _segment.value = current.copy(start = newStart)
    }

    fun addToEnd(by: Int) {
        val current = _segment.value ?: return
        val newEnd = (current.end + by).coerceIn(current.start + minimumSegmentDuration, durationMs)
        _segment.value = current.copy(end = newEnd)
    }

}

@Composable
fun rememberSegmentPickerState(
    amplitudes: List<Int>,
    durationMs: Long,
    minimumSegmentMs: Long,
    inactive: List<Segment>
): SegmentPickerState {
    val density = LocalDensity.current
    return remember {
        SegmentPickerState(
            density = density,
            spikeWidth = 2.dp,
            spikeRadius = 2.dp,
            spikePadding = 2.dp,
            amplitudes = amplitudes,
            durationMs = durationMs,
            waveformAlignment = WaveformAlignment.Center,
            minimumSegmentDuration = minimumSegmentMs,
            inactive = inactive
        )
    }
}