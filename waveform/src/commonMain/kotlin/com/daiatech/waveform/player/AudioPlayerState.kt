package com.daiatech.waveform.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.segmentation.DURATION_MS_BETWEEN_TIMESTAMP
import com.daiatech.waveform.segmentation.WaveformLayout
import com.daiatech.waveform.segmentation.millisNow
import com.daiatech.waveform.segmentation.noOfSpikesInTwoTimestamps
import com.daiatech.waveform.segmentation.zoom.Zoom
import com.daiatech.waveform.toDrawableAmplitudes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class AudioPlayerState(
    density: Density,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    val amplitudes: List<Int>,
    val durationMs: Long,
    /**
     * Lifecycle-bound scope used for background pre-warming of non-visible zoom
     * levels. When the owning composable leaves composition this scope is
     * cancelled, so pre-warm work stops instead of wasting cycles/memory.
     */
    private val scope: CoroutineScope,
    graphHeight: Dp = MIN_GRAPH_HEIGHT,
    verticalItemSpacing: Dp = 8.dp,
    markerFontSize: TextUnit = 12.sp,
) {
    /**
     * Cache of processed amplitudes per zoom level. Guarded by [storeMutex] —
     * it is read/written from multiple [Dispatchers.Default] coroutines and a
     * plain map is not safe for concurrent access.
     */
    private val drawableAmplitudesStore = mutableMapOf<Zoom, List<Float>>()
    private val storeMutex = Mutex()
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

    /**
     * Computes the drawable amplitudes for the currently selected zoom level,
     * publishes them to [drawableAmplitudes], then pre-warms the remaining zoom
     * levels in the background.
     *
     * Correctness comes from the cache + [storeMutex], not from a re-entry flag:
     * - The store is only ever touched under the mutex, so concurrent callers
     *   on [Dispatchers.Default] can never observe a torn map (the original
     *   cause of the `NullPointerException` at the map read).
     * - The visible zoom is computed into a local and assigned from that local,
     *   so the published value can never be null regardless of how the
     *   background pre-warm interleaves.
     * - If the user changes zoom while we compute, we skip the stale assignment.
     */
    suspend fun calculateDrawableAmplitudes() = withContext(Dispatchers.Default) {
        val zoomValue = _zoom.value

        val cached = storeMutex.withLock { drawableAmplitudesStore[zoomValue] }
        val amps = cached ?: computeForZoom(zoomValue).also { computed ->
            storeMutex.withLock { drawableAmplitudesStore.getOrPut(zoomValue) { computed } }
        }

        // Assign from the local — never re-read the map, never `!!`.
        if (_zoom.value == zoomValue) {
            _drawableAmplitudes.value = amps
            _processing.value = false
        }

        prewarmOtherZoomLevels(zoomValue)
    }

    /**
     * Pure computation of drawable amplitudes for [zoom]. Touches no shared
     * mutable state, so it is safe to run concurrently for different zooms.
     */
    private suspend fun computeForZoom(zoom: Zoom): List<Float> {
        val noOfSpikes = (
            noOfSpikesInTwoTimestamps(zoom) * durationMs /
                DURATION_MS_BETWEEN_TIMESTAMP
        ).toInt()
        val chunkSize = 5000
        return if (noOfSpikes > chunkSize) {
            processAmplitudesInChunks(noOfSpikes, chunkSize)
        } else {
            amplitudes.toDrawableAmplitudes(
                amplitudeType = AmplitudeType.AVG,
                spikes = noOfSpikes,
                minHeight = MIN_SPIKE_HEIGHT,
                maxHeight = layout.graphHeightPx
            )
        }
    }

    /**
     * Lazily computes the remaining zoom levels on the lifecycle-bound [scope]
     * so switching zoom is instant after the first visit. Cancellable: when the
     * player leaves composition the scope is cancelled and this work stops.
     */
    private fun prewarmOtherZoomLevels(current: Zoom) {
        scope.launch(Dispatchers.Default) {
            Zoom.entries.filter { it != current }.forEach { zoom ->
                val alreadyCached = storeMutex.withLock { drawableAmplitudesStore.containsKey(zoom) }
                if (!alreadyCached) {
                    val computed = computeForZoom(zoom)
                    storeMutex.withLock { drawableAmplitudesStore.getOrPut(zoom) { computed } }
                }
                yield()
            }
        }
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
): AudioPlayerState {
    val density = LocalDensity.current
    val dimensions = LocalAudioPlayerDimensions.current
    val scope = rememberCoroutineScope()
    return remember {
        AudioPlayerState(
            density = density,
            spikeWidth = 2.dp,
            spikeRadius = 2.dp,
            spikePadding = 2.dp,
            amplitudes = amplitudes,
            durationMs = durationMs,
            scope = scope,
            graphHeight = dimensions.graphHeight,
            verticalItemSpacing = dimensions.verticalItemSpacing,
            markerFontSize = dimensions.markerFontSize,
        )
    }
}
