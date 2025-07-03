package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class SegmentationState(
    val audioFilePath: String,
    val durationMs: Long,
    val amplitudes: List<Int>,
    initialSegments: List<Segment> = listOf(),
    val enableAdjustment: Boolean,
    val graphHeight: Dp,
    val style: DrawStyle,
    val waveformBrush: Brush,
    val progressBrush: Brush,
    val markerBrush: Brush,
    val waveformAlignment: WaveformAlignment,
    val amplitudeType: AmplitudeType,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    minimumSegmentDuration: Long,
    maximumSegmentDuration: Long
) {

    var canvasSize: Size = Size.Zero
    val spikeWidth = spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp)
    val progressBarWidth = spikeWidth.times(2f)
    val spikeRadius = spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp)
    private val minimumSegmentDuration = min(durationMs, minimumSegmentDuration.coerceAtLeast(50))
    private val maximumSegmentDuration = maximumSegmentDuration.coerceAtMost(durationMs)
    val spikeTotalWidth =
        this.spikeWidth + spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp)

    /***************************  UI States   *****************************/

    private val _segments = mutableStateListOf(
        *(
            initialSegments.filter {
                // Remove the segments which are less than minimum duration
                it.end - it.start >= minimumSegmentDuration
            }
            ).toTypedArray()
    )
    val segments: SnapshotStateList<Segment> = _segments

    private val _spikeAmplitudes = mutableStateOf(listOf<Float>())
    val spikeAmplitude: State<List<Float>> = _spikeAmplitudes

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
        val zoomedInAmps = activeSegment.value?.let {
            val segment = segments[it]
            val prevSegment = segments.getOrNull(it - 1)
            val nextSegment = segments.getOrNull(it + 1)

            val viewStartMs = (segment.start).coerceAtLeast(prevSegment?.end ?: 0)
            val viewEndMs = (segment.end).coerceAtMost(nextSegment?.start ?: durationMs)

            val startIdx =
                (amplitudes.size.toFloat() / durationMs.toFloat() * viewStartMs.toFloat()).toInt()
                    .coerceIn(0, amplitudes.size)
            val endIdx =
                (amplitudes.size.toFloat() / durationMs.toFloat() * viewEndMs.toFloat()).toInt()
                    .coerceIn(0, amplitudes.size)

            amplitudes.subList(startIdx, endIdx).toDrawableAmplitudes(
                amplitudeType = amplitudeType,
                spikes = spikes,
                minHeight = MIN_SPIKE_HEIGHT,
                maxHeight = maxHeight,
                multiplier = spikesMultiplier
            )
        } ?: listOf()
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

    private val _activeSegment = mutableStateOf(if (segments.isEmpty()) null else 0)
    val activeSegment: State<Int?> = _activeSegment

    private val _groupedSegments = mutableStateListOf<Int>()
    val groupedSegments: SnapshotStateList<Int> = _groupedSegments

    private val _undoList = mutableStateListOf<List<Segment>>()
    val undoList: SnapshotStateList<List<Segment>> = _undoList

    private val _redoList = mutableStateListOf<List<Segment>>()
    val redoList: SnapshotStateList<List<Segment>> = _redoList

    /***************************  UI States   *****************************/

    /***************************  UI INTERACTIONS   *****************************/
    fun addToStart(by: Int) {
        activeSegment.value?.let {
            val segment = _segments[it]
            val prevSegment = _segments.getOrNull(it - 1)
            val newXStart = (segment.start + by)
                .coerceIn(
                    max(prevSegment?.end ?: 0, segment.end - maximumSegmentDuration),
                    segment.end - minimumSegmentDuration
                )
            _segments[it] = Segment(newXStart, segment.end)
        }
    }

    fun addToEnd(by: Int) {
        activeSegment.value?.let {
            val segment = _segments[it]
            val nextSegment = _segments.getOrNull(it + 1)
            val newXEnd = (segment.end + by)
                .coerceIn(
                    (segment.start + minimumSegmentDuration),
                    min(nextSegment?.start ?: durationMs, segment.start + maximumSegmentDuration)
                )
            _segments[it] = Segment(segment.start, newXEnd)
        }
    }

    fun onLongPress(pressedAt: Offset) {
        val pressedSegmentIdx = segments.indexOfFirst { segment ->
            val xStart = durationToPx(segment.start)
            val xEnd = durationToPx(segment.end)
            pressedAt.x in xStart..xEnd
        }
        if (pressedSegmentIdx != -1) {
            _activeSegment.value = null
            _groupedSegments.clear()
            _groupedSegments.add(pressedSegmentIdx)
        }
    }

    fun onTap(tappedAt: Offset) {
        val tappedSegmentIdx = segments.indexOfFirst { segment ->
            val xStart = durationToPx(segment.start)
            val xEnd = durationToPx(segment.end)
            tappedAt.x in xStart..xEnd
        }

        if (tappedSegmentIdx != -1) {
            if (groupedSegments.isEmpty()) {
                _activeSegment.value = if (activeSegment.value == tappedSegmentIdx) {
                    null
                } else {
                    tappedSegmentIdx
                }
            } else {
                // add this to grouping
                val first = groupedSegments.toMutableList()
                if (first.contains(tappedSegmentIdx)) {
                    first.remove(tappedSegmentIdx)
                } else {
                    first.add(tappedSegmentIdx)
                }

                val gs = if (first.isNotEmpty()) {
                    first.sort()
                    (first.first()..first.last()).toList()
                } else {
                    listOf()
                }
                _groupedSegments.clear()
                _groupedSegments.addAll(gs)
            }
        }
    }

    fun onHorizontalDrag(change: PointerInputChange, dragAmount: Float, touchTargetSize: Float) {
        activeSegment.value?.let {
            val segment = segments[it]
            val xStart = durationToPx(segment.start)
            val xEnd = durationToPx(segment.end)

            // end is being dragged
            if (abs(xEnd - change.position.x) <= touchTargetSize) {
                addToEnd(pxToDuration(dragAmount).toInt())
            }

            // start is being dragged
            if (abs(xStart - change.position.x) <= touchTargetSize) {
                addToStart(pxToDuration(dragAmount).toInt())
            }
        }
    }

    fun addSegment() {
        val lastSegment = segments.lastOrNull()
        if ((lastSegment?.end ?: 0).plus(minimumSegmentDuration) <= durationMs) {
            val start = lastSegment?.end ?: 0L
            val end = (start + minimumSegmentDuration).coerceAtMost(durationMs)
            _segments.add(Segment(start, end))
            _activeSegment.value = segments.lastIndex
        }
    }

    fun removeActiveSegment() {
        activeSegment.value?.let { segment ->
            _segments.removeAt(segment)
            _activeSegment.value = if (_segments.isEmpty() || segment - 1 < 0) null else segment - 1
        }
    }

    fun removeAllSegments() {
        _activeSegment.value = null
        _undoList.add(_segments.toList())
        _segments.clear()
    }

    fun mergeSegments() {
        // Check for requirements
        if (groupedSegments.isEmpty()) return
        val firstIdx = groupedSegments.first()
        val lastIdx = groupedSegments.last()
        val first = segments.getOrNull(firstIdx)
        val last = segments.getOrNull(lastIdx)
        if (first == null || last == null) return

        // All requirements met then, merge segments
        _undoList.add(_segments.toList())
        val newSegment = Segment(first.start, last.end)
        _segments.removeRange(
            groupedSegments.first(),
            groupedSegments.last() + 1
        )
        _segments.add(firstIdx, newSegment)
        groupedSegments.clear()
    }

    fun undoMerge() {
        _activeSegment.value = null
        if (_undoList.isEmpty()) return
        val s = _undoList.removeAt(_undoList.lastIndex)
        _redoList.add(_segments.toList())
        _segments.clear()
        _segments.addAll(s)
    }

    fun redoMerge() {
        _activeSegment.value = null
        if (_redoList.isEmpty()) return
        val s = _redoList.removeAt(_redoList.lastIndex)
        _undoList.add(_segments.toList())
        _segments.clear()
        _segments.addAll(s)
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
fun rememberAudioSegmentationState(
    key: Any? = null,
    audioFilePath: String,
    durationMs: Long,
    amplitudes: List<Int>,
    initialSegments: List<Segment> = listOf(),
    enableAdjustment: Boolean,
    graphHeight: Dp = MIN_GRAPH_HEIGHT,
    style: DrawStyle = Fill,
    waveformBrush: Brush = SolidColor(Color.White),
    progressBrush: Brush = SolidColor(Color.Cyan),
    markerBrush: Brush = SolidColor(Color.Green),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.MAX,
    spikeWidth: Dp = 2.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    minimumSegmentDuration: Long = 1000,
    maximumSegmentDuration: Long = 15000
): SegmentationState {
    return remember(key) {
        SegmentationState(
            audioFilePath,
            durationMs,
            amplitudes,
            initialSegments,
            enableAdjustment,
            graphHeight,
            style,
            waveformBrush,
            progressBrush,
            markerBrush,
            waveformAlignment,
            amplitudeType,
            spikeWidth,
            spikeRadius,
            spikePadding,
            minimumSegmentDuration,
            maximumSegmentDuration
        )
    }
}
