package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.Segment
import com.daiatech.waveform.segmentation.zoom.Zoom
import com.daiatech.waveform.toDrawableAmplitudes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Font size for timestamp markers
 */
internal val markerFontSize: TextUnit = 12.sp

/**
 * Vertical spacing between elements
 */
internal val verticalItemSpacing: Dp = 8.dp

/**
 * No of spikes between two timestamp markers.
 */
private const val SPIKE_COUNT_BETWEEN_TIMESTAMP: Int = 10

class SegmentPickerState(
    density: Density,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    val amplitudes: List<Int>,
    val durationMs: Long,
    val minimumSegmentDuration: Long,
    val inactive: List<Segment>
) {

    private val drawableAmplitudesStore = mutableMapOf<Zoom, List<Float>>()
    private val _processing = mutableStateOf(true)
    private var _zoom = mutableStateOf(Zoom.X1)
    private val _drawableAmplitudes = mutableStateOf(listOf<Float>())
    private val _segment = mutableStateOf<Segment?>(null)

    internal val layout = with(density) {
        val triangleSpace = verticalItemSpacing.toPx()
        val textHeight = markerFontSize.toPx()
        val graphHeight = MIN_GRAPH_HEIGHT.toPx()

        WaveformLayout(
            canvasHeightPx = (triangleSpace * 2 + textHeight + triangleSpace +
                    triangleSpace + graphHeight + triangleSpace +
                    triangleSpace + textHeight + triangleSpace + triangleSpace),
            graphHeightPx = graphHeight,
            evenMarkerY = triangleSpace * 2,
            oddMarkerY = triangleSpace * 2 + textHeight + triangleSpace +
                    triangleSpace + graphHeight + triangleSpace,
            markerY = triangleSpace * 2 + textHeight + triangleSpace,
            markerHeight = triangleSpace + graphHeight + triangleSpace,
            graphY = triangleSpace * 2 + textHeight + triangleSpace * 2,
            spikeRadiusPx = spikeRadius.toPx(),
            spikeWidthPx = spikeWidth.toPx(),
            spikePaddingPx = spikePadding.toPx(),
            windowRadiusPx = 8.dp.toPx()
        )
    }

    val zoom: State<Zoom> = _zoom
    val processing: State<Boolean> = _processing
    val spikeCountPerTimestampMs = SPIKE_COUNT_BETWEEN_TIMESTAMP
    val drawableAmplitudes: State<List<Float>> = _drawableAmplitudes
    val segment: State<Segment?> = _segment
    val window = derivedStateOf {
        _segment.value?.let {
            val startPx = durationToPx(it.start)
            val endPx = durationToPx(it.end)
            Pair(startPx, endPx)
        }
    }

    suspend fun calculateDrawableAmplitudes() = withContext(Dispatchers.Default) {
        val zoomValue = _zoom.value
        if (drawableAmplitudesStore.containsKey(zoomValue)) {
            _drawableAmplitudes.value = drawableAmplitudesStore[zoomValue]!!
            _processing.value = false
            return@withContext
        }
        val startMs = millisNow
        println("SegmentPickerState:: Processing audio started at $startMs")
        val priorityZooms = listOf(zoomValue) + Zoom.entries.filter { it != zoomValue }
        val processingJobs = priorityZooms.map { zoom ->
            async {
                if (!drawableAmplitudesStore.containsKey(zoom)) {
                    println("SegmentPickerState:: Processing for $zoom level")
                    val levelStartMs = millisNow
                    val timestampDuration = durationBetweenTwoTimestampMarkers(zoom)
                    val noOfSpikes =
                        (durationMs * SPIKE_COUNT_BETWEEN_TIMESTAMP / timestampDuration).toInt()
                    val chunkSize = 5000
                    val drawableAmps = if (noOfSpikes > chunkSize) {
                        processAmplitudesInChunks(noOfSpikes, chunkSize)
                    } else {
                        amplitudes.toDrawableAmplitudes(
                            amplitudeType = AmplitudeType.AVG,
                            spikes = noOfSpikes,
                            minHeight = MIN_SPIKE_HEIGHT,
                            maxHeight = layout.graphHeightPx
                        )
                    }
                    drawableAmplitudesStore[zoom] = drawableAmps
                    val levelEndMs = millisNow
                    println("SegmentPickerState:: Completed $zoom in ${levelEndMs - levelStartMs}ms")
                }
            }
        }
        processingJobs.first().await()
        _drawableAmplitudes.value = drawableAmplitudesStore[zoomValue]!!
        _processing.value = false
        val endMs = millisNow
        println("SegmentPickerState:: Initial processing complete in ${endMs - startMs}ms")
        processingJobs.drop(1)
            .forEach { it.await() } // Continue processing other zoom levels in background
        val finalMs = millisNow
        println("SegmentPickerState:: All zoom levels processed in ${finalMs - startMs}ms")
    }

    private suspend fun processAmplitudesInChunks(
        totalSpikes: Int,
        chunkSize: Int
    ): List<Float> = withContext(Dispatchers.Default) {
        val result = mutableListOf<Float>()
        val chunks = (totalSpikes + chunkSize - 1) / chunkSize
        for (chunkIndex in 0 until chunks) {
            val startIdx = chunkIndex * chunkSize
            val endIdx = minOf((chunkIndex + 1) * chunkSize, totalSpikes)
            val chunkSpikes = endIdx - startIdx
            // calculate which amplitudes correspond to this chunk
            val amplitudeStartIdx = (startIdx.toLong() * amplitudes.size / totalSpikes).toInt()
            val amplitudeEndIdx = (endIdx.toLong() * amplitudes.size / totalSpikes).toInt()

            val chunkAmplitudes = amplitudes.subList(
                amplitudeStartIdx.coerceIn(0, amplitudes.size),
                amplitudeEndIdx.coerceIn(0, amplitudes.size)
            )
            val chunkDrawable = chunkAmplitudes.toDrawableAmplitudes(
                amplitudeType = AmplitudeType.AVG,
                spikes = chunkSpikes,
                minHeight = MIN_SPIKE_HEIGHT,
                maxHeight = layout.graphHeightPx
            )
            result.addAll(chunkDrawable)
            yield() // Yield to allow other coroutines to run
        }
        result
    }


    suspend fun zoomIn() {
        _zoom.value = _zoom.value.increment()
        calculateDrawableAmplitudes()
    }

    suspend fun zoomOut() {
        _zoom.value = _zoom.value.decrement()
        calculateDrawableAmplitudes()
    }


    /**
     * px
     * = width for 1ms * dur
     * = (canvasWidthPx / durationMs) * dur
     * = (spikeTotalWidthPx * noOfSpikes / durationMs ) * dur
     * = (spikeTotalWidthPx * (durationMs * spikeCountPerTimestampMs / timestampMs)) / durationMs * dur
     * = (spikeTotalWidthPx * spikeCountPerTimestampMs / timestampMs) * dur
     */
    fun durationToPx(dur: Long): Float {
        return (layout.spikeTotalWidthPx * spikeCountPerTimestampMs * dur) /
                durationBetweenTwoTimestampMarkers(zoom.value)
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
        return ((durationBetweenTwoTimestampMarkers(zoom.value) * px) /
                (layout.spikeTotalWidthPx * spikeCountPerTimestampMs)).toLong()
    }

    fun addSegment(start: Long) {
        if (_segment.value != null) {
            println("Cannot add segment, start is before last inactive end")
            return
        }
        val lastInactive = inactive.lastOrNull()
        if (lastInactive != null && start < lastInactive.end) {
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

private val durationCache = mutableMapOf<Zoom, Long>()
fun durationBetweenTwoTimestampMarkers(zoom: Zoom): Long {
    return durationCache.getOrPut(zoom) {
        (500 - (zoom.value - 1) * 100).toLong()
    }
}


@OptIn(ExperimentalTime::class)
val millisNow get() = Clock.System.now().toEpochMilliseconds()

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
            minimumSegmentDuration = minimumSegmentMs,
            inactive = inactive
        )
    }
}