package com.daiatech.waveform.segmentPicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.chunkToSize
import com.daiatech.waveform.fillToSize
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.minSpikePaddingDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.normalize
import com.daiatech.waveform.segmentation.end
import com.daiatech.waveform.segmentation.start
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class SegmentPickerState(
    val audioFilePath: String,
    val durationMs: Long,
    val amplitudes: List<Int>,
    val graphHeight: Dp,
    val style: DrawStyle,
    val waveformAlignment: WaveformAlignment,
    val amplitudeType: AmplitudeType,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    minimumSegmentDuration: Long,
    maximumSegmentDuration: Long,
    window: Segment,
    segment: Segment,
) {

    val minimumWindowDuration = 500
    var canvasSize: Size = Size.Zero
    val spikeWidth = spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp)
    val progressBarWidth = spikeWidth.times(2f)
    val spikeRadius = spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp)
    private val minimumSegmentDuration = min(durationMs, minimumSegmentDuration.coerceAtLeast(50))
    private val maximumSegmentDuration = maximumSegmentDuration.coerceAtMost(durationMs)
    val spikeTotalWidth =
        this.spikeWidth + spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp)

    /***************************  UI States   *****************************/

    private val _segment = mutableStateOf(segment)
    val segment: State<Segment> = _segment

    private val _window = mutableStateOf(window)
    val window: State<Segment> = _window

    private val _spikeAmplitudes = mutableStateOf(listOf<Float>())
    val spikeAmplitude: State<List<Float>> = _spikeAmplitudes

    private val _activeWindow = mutableStateOf(ActiveWindow.WINDOW)
    val activeWindow: State<ActiveWindow> = _activeWindow

    val activeSegment
        get() = when (_activeWindow.value) {
            ActiveWindow.WINDOW -> _window.value
            ActiveWindow.SEGMENT -> _segment.value
        }


    private val _zoomedInAmplitudes = mutableStateOf(listOf<Float>())
    val zoomedInAmplitudes: State<List<Float>> = _zoomedInAmplitudes

    suspend fun computeDrawableAmplitudes(
        spikes: Int,
        spikesMultiplier: Float
    ) = withContext(Dispatchers.Default) {
        val maxHeight = canvasSize.height.coerceAtLeast(MIN_SPIKE_HEIGHT)
        val drawableAmps = amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes,
            maxHeight = maxHeight,
            minHeight = MIN_SPIKE_HEIGHT,
            multiplier = spikesMultiplier
        )
        _spikeAmplitudes.value = drawableAmps
    }

    suspend fun computeZoomedInDrawableAmplitudes(
        spikes: Int,
        spikesMultiplier: Float
    ) = withContext(Dispatchers.Default) {
        val maxHeight = canvasSize.height.coerceAtLeast(MIN_SPIKE_HEIGHT)
        val zoomedInAmps = window.value.let { window ->
            val viewStartMs = (window.start).coerceAtLeast(0)
            val viewEndMs = (window.end).coerceAtMost(durationMs)
            val startIdx = (amplitudes.size.toFloat() / durationMs.toFloat()
                    * viewStartMs.toFloat()).toInt()
                .coerceIn(0, amplitudes.size)
            val endIdx = (amplitudes.size.toFloat() / durationMs.toFloat()
                    * viewEndMs.toFloat()).toInt()
                .coerceIn(0, amplitudes.size)
            amplitudes.subList(startIdx, endIdx).toDrawableAmplitudes(
                amplitudeType = amplitudeType,
                spikes = spikes,
                minHeight = MIN_SPIKE_HEIGHT,
                maxHeight = maxHeight,
                multiplier = spikesMultiplier
            )
        }
        _zoomedInAmplitudes.value = zoomedInAmps
    }

    private suspend fun List<Int>.toDrawableAmplitudes(
        amplitudeType: AmplitudeType,
        spikes: Int,
        minHeight: Float,
        maxHeight: Float,
        multiplier: Float = 1f
    ): List<Float> = withContext(Dispatchers.Default) {
        val amplitudes = map(Int::toFloat)
        if (amplitudes.isEmpty() || spikes == 0) {
            return@withContext List(spikes) { minHeight }
        }
        val transform = { data: List<Float> ->
            when (amplitudeType) {
                AmplitudeType.AVG -> data.average()
                AmplitudeType.MAX -> data.max()
                AmplitudeType.MIN -> data.min()
            }.toFloat()
        }
        return@withContext when {
            spikes > amplitudes.count() -> amplitudes.fillToSize(spikes, transform)
            else -> amplitudes.chunkToSize(spikes, transform)
        }.normalize(minHeight, maxHeight)
            .map { it.times(multiplier).coerceIn(minHeight, maxHeight) }
    }

    /***************************  UI States   *****************************/

    /***************************  UI INTERACTIONS   *****************************/
    fun onSelect(what: ActiveWindow) {
        _activeWindow.value = what
    }

    fun addToStart(by: Int) {
        when (activeWindow.value) {
            ActiveWindow.WINDOW -> {
                val newStart = (window.value.start + by)
                    .coerceIn(0, (durationMs - minimumWindowDuration))
                _window.value = window.value.copy(first = newStart)
            }

            ActiveWindow.SEGMENT -> {
                val newStart = (segment.value.start + by)
                    .coerceIn(0, (durationMs - minimumSegmentDuration))
                _segment.value = segment.value.copy(first = newStart)
            }
        }

    }

    fun addToEnd(by: Int) {
        when (activeWindow.value) {
            ActiveWindow.WINDOW -> {
                val newEnd = (window.value.end + by)
                    .coerceIn(window.value.start + minimumWindowDuration, durationMs)
                _window.value = window.value.copy(second = newEnd)
            }

            ActiveWindow.SEGMENT -> {
                val newEnd = (segment.value.end + by)
                    .coerceIn(segment.value.start + minimumSegmentDuration, durationMs)
                _segment.value = segment.value.copy(second = newEnd)
            }
        }
    }

    fun moveWindow(by: Int) {
        when (activeWindow.value) {
            ActiveWindow.WINDOW -> {
                val newEnd = (window.value.end + by)
                    .coerceIn(
                        window.value.start + minimumWindowDuration,
                        durationMs
                    )
                val newStart = (window.value.start + by)
                    .coerceIn(0, (durationMs - minimumWindowDuration))
                if (newEnd == window.value.end || newStart == window.value.start) return
                _window.value = _window.value.copy(second = newEnd)
                _window.value = Segment(newStart, newEnd)
            }

            ActiveWindow.SEGMENT -> {
                val newStart = (segment.value.start + by)
                    .coerceIn(window.value.start, (window.value.end - minimumSegmentDuration))

                val newEnd = (segment.value.end + by)
                    .coerceIn(
                        segment.value.start + minimumSegmentDuration,
                        window.value.end
                    )
                if (newEnd == segment.value.end || newStart == segment.value.start) return
                _segment.value = segment.value.copy(second = newEnd)
                _segment.value = Segment(newStart, newEnd)
            }
        }
    }

    suspend fun onWindowDrag(
        change: PointerInputChange,
        dragAmount: Float,
        touchTargetSize: Float
    ) = withContext(Dispatchers.Default) {
        val xStart = durationToPx(window.value.start)
        val xEnd = durationToPx(window.value.end)
        val by = pxToDuration(dragAmount).toInt()
/*
        // end is being dragged
        if (abs(xEnd - change.position.x) <= touchTargetSize) {
            addToEnd(by)
            return@withContext
        }

        // start is being dragged
        if (abs(xStart - change.position.x) <= touchTargetSize) {
            addToStart(by)
            return@withContext
        }
*/
        // entire window is being dragged
        if (change.position.x in ((xStart + touchTargetSize)..(xEnd - touchTargetSize))) {
            moveWindow(by)
            return@withContext
        }
    }


    /***************************  UI INTERACTIONS   *****************************/

    /***************************  UTILITIES   *****************************/

    /**
     * Returns the position of pixel which lies on the [time]
     *
     * Known values (x,y) = (s,0) and (e,w)
     *
     * required pixel value (y)
     *
     * total width (w) = canvas.width
     *
     * current time (x)
     *
     * start (s) = { [start] - if start!=null ; 0 - otherwise }
     *
     * end (e) = { [end] - if end!=null ; durationMS - otherwise}
     *
     *  y = mx + c
     *
     *  y = (y2 - y1)x/(x2-x1) + c
     *
     *  y = (w-0)x/(e-s) + c ....(1)
     *
     *  Putting (s,0) in (1)
     *
     *  0 = ws/(e-s) + c
     *
     *  c = -ws/(e-s)
     *
     *  Substituting c in (1)
     *
     *  y = wx/(e-s) - ws/(e-s)
     *
     *  y = w(x-s)/(e-s)
     */
    fun durationToPx(time: Long, start: Long? = null, end: Long? = null): Float {
        val s = start ?: 0
        val e = end ?: durationMs
        val w = canvasSize.width
        return w * (time - s) / (e - s)
    }

    /**
     * Returns the corresponding duration which a pixel represents.
     * This function is inverse of [durationToPx].
     *
     * For underlying calculations see [durationToPx]
     * @See durationToPx
     */
    fun pxToDuration(px: Float, start: Long? = null, end: Long? = null): Long {
        val s = start ?: 0L
        val e = end ?: durationMs
        val w = canvasSize.width

        return (px * (e - s) / w).toLong() + s
    }
    /***************************  UTILITIES   *****************************/
}

@Composable
fun rememberAudioSegmentPickerState(
    key: Any? = null,
    audioFilePath: String,
    durationMs: Long,
    amplitudes: List<Int>,
    graphHeight: Dp = MIN_GRAPH_HEIGHT,
    style: DrawStyle = Fill,
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.MAX,
    spikeWidth: Dp = 2.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    minimumSegmentDuration: Long = 50,
    maximumSegmentDuration: Long = 15000,
    window: Segment = Segment(0, durationMs),
    segment: Segment = Segment(0, durationMs / 4),
): SegmentPickerState {
    return remember(key) {
        SegmentPickerState(
            audioFilePath = audioFilePath,
            durationMs = durationMs,
            amplitudes = amplitudes,
            graphHeight = graphHeight,
            style = style,
            waveformAlignment = waveformAlignment,
            amplitudeType = amplitudeType,
            spikeWidth = spikeWidth,
            spikeRadius = spikeRadius,
            spikePadding = spikePadding,
            minimumSegmentDuration = minimumSegmentDuration,
            maximumSegmentDuration = maximumSegmentDuration,
            window = window,
            segment = segment,
        )
    }
}
