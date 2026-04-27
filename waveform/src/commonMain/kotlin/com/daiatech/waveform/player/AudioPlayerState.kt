package com.daiatech.waveform.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.segmentation.DURATION_MS_BETWEEN_TIMESTAMP
import com.daiatech.waveform.segmentation.WaveformLayout
import com.daiatech.waveform.segmentation.markerFontSize
import com.daiatech.waveform.segmentation.millisNow
import com.daiatech.waveform.segmentation.noOfSpikesInTwoTimestamps
import com.daiatech.waveform.segmentation.verticalItemSpacing
import com.daiatech.waveform.segmentation.zoom.Zoom
import com.daiatech.waveform.toDrawableAmplitudes
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class AudioPlayerState(
    density: Density,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    val amplitudes: List<Int>,
    val durationMs: Long,
    graphHeight: Dp = MIN_GRAPH_HEIGHT,
) {
    private val drawableAmplitudesStore = mutableMapOf<Zoom, List<Float>>()
    private val _processing = mutableStateOf(true)
    private val _zoom = mutableStateOf(Zoom.X1)
    private val _drawableAmplitudes = mutableStateOf(listOf<Float>())

    internal val layout = with(density) {
        val triangleSpace = verticalItemSpacing.toPx()
        val textHeight = markerFontSize.toPx()
        val graphHeight = graphHeight.toPx()

        WaveformLayout(
            canvasHeightPx = (
                triangleSpace * 2 + textHeight + triangleSpace +
                    triangleSpace + graphHeight + triangleSpace +
                    triangleSpace + textHeight + triangleSpace + triangleSpace
            ),
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
    val drawableAmplitudes: State<List<Float>> = _drawableAmplitudes

    private var processingJobs: List<Deferred<Unit>> = listOf()

    suspend fun calculateDrawableAmplitudes() = withContext(Dispatchers.Default) {
        val zoomValue = _zoom.value
        if (drawableAmplitudesStore.containsKey(zoomValue)) {
            _drawableAmplitudes.value = drawableAmplitudesStore[zoomValue]!!
            _processing.value = false
            return@withContext
        }
        val startMs = millisNow
        println("AudioPlayerState:: Processing audio started at $startMs")
        val priorityZooms = listOf(zoomValue) + Zoom.entries.filter { it != zoomValue }
        if (processingJobs.isNotEmpty()) {
            println("AudioPlayerState:: Already processing, returning")
            return@withContext
        }
        processingJobs = priorityZooms.map { zoom ->
            async {
                if (!drawableAmplitudesStore.containsKey(zoom)) {
                    println("AudioPlayerState:: Processing for $zoom level")
                    val levelStartMs = millisNow
                    val noOfSpikes = (
                        noOfSpikesInTwoTimestamps(zoom) * durationMs /
                            DURATION_MS_BETWEEN_TIMESTAMP
                    ).toInt()
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
                    println("AudioPlayerState:: Completed $zoom in ${levelEndMs - levelStartMs}ms")
                }
            }
        }
        processingJobs.first().await()
        _drawableAmplitudes.value = drawableAmplitudesStore[zoomValue]!!
        _processing.value = false
        val endMs = millisNow
        println("AudioPlayerState:: Initial processing complete in ${endMs - startMs}ms")
        processingJobs.drop(1).forEach { it.await() }
        val finalMs = millisNow
        println("AudioPlayerState:: All zoom levels processed in ${finalMs - startMs}ms")
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
            yield()
        }
        result
    }

    suspend fun zoomIn() {
        if (processing.value) {
            println("Cannot zoom in, processing...")
            return
        }
        _zoom.value = _zoom.value.increment()
        calculateDrawableAmplitudes()
    }

    suspend fun zoomOut() {
        if (processing.value) {
            println("Cannot zoom out, processing...")
            return
        }
        _zoom.value = _zoom.value.decrement()
        calculateDrawableAmplitudes()
    }

    fun durationToPx(dur: Long): Float {
        return (noOfSpikesInTwoTimestamps(zoom.value) * layout.spikeTotalWidthPx * dur) / DURATION_MS_BETWEEN_TIMESTAMP
    }

    fun pxToDuration(px: Float): Long {
        return (
            (px * DURATION_MS_BETWEEN_TIMESTAMP) /
                (noOfSpikesInTwoTimestamps(zoom.value) * layout.spikeTotalWidthPx)
        ).toLong()
    }
}

@Composable
fun rememberAudioPlayerState(
    amplitudes: List<Int>,
    durationMs: Long,
    graphHeight: Dp = 48.dp,
): AudioPlayerState {
    val density = LocalDensity.current
    return remember {
        AudioPlayerState(
            density = density,
            spikeWidth = 2.dp,
            spikeRadius = 2.dp,
            spikePadding = 2.dp,
            amplitudes = amplitudes,
            durationMs = durationMs,
            graphHeight = graphHeight,
        )
    }
}
