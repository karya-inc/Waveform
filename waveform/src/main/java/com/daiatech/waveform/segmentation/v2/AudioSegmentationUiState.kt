package com.daiatech.waveform.segmentation.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.chunkToSize
import com.daiatech.waveform.fillToSize
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.minSpikePaddingDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.normalize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * State that changes with interaction
 */
data class AudioSegmentationUiState(
    val density: Density,
    val amplitudes: List<Int>,
    val spikeMaxHeight: Dp =48.dp,
    val spikeWidth: Dp = 2.dp,
    val spikeRadius: Dp = 1.dp,
    val spikePadding: Dp = 1.dp,
    val alignment: WaveformAlignment = WaveformAlignment.Center,
    val amplitudeType: AmplitudeType = AmplitudeType.MAX
) {
    val spikeTotalWidth =
        this.spikeWidth + spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp)

    // todo: playback states
    var isPlaying = false
    var isPaused = false
    var isStopped = false
    var currentProgressMs: Long = 0

    private val _segments = mutableStateListOf<Pair<Long, Long>>()
    val segment: SnapshotStateList<Pair<Long, Long>> = _segments

    private val _currentSegment = mutableStateOf<Pair<Long?, Long?>?>(null)
    val currentSegment: State<Pair<Long?, Long?>?> = _currentSegment

    val marker = derivedStateOf {
        val currentSegment = currentSegment.value
        when {
            currentSegment == null -> Marker.START
            currentSegment.first == null -> Marker.START
            currentSegment.first != null -> Marker.END
            else -> Marker.START
        }
    }

    private val _spikeAmplitudes = mutableStateOf(listOf<Float>())
    val spikeAmplitude: State<List<Float>> = _spikeAmplitudes

    /**
     * Add a marker to the current segment
     *
     * 1. If there is no current segment, (either currentSegment = null or currentSegment.first = null), create a new one
     * 2. Add [currentProgressMs] as start, keep end as null
     * 3. If there is a current segment, which means start is already present, add [currentProgressMs] as end
     * 4. Push the completed segment with start and end to [segments]
     * 5. Mark currentSegment as null
     */
    fun addMarker() {
        val currentSegment = currentSegment.value
        val start = currentSegment?.first
        val end = currentSegment?.second

        if (currentSegment == null || start == null) {
            _currentSegment.value = currentProgressMs to null
            return
        }

        if (end == null) {
            _segments.add(start to currentProgressMs)
            _currentSegment.value = null
            return
        }
    }

    suspend fun computeDrawableAmplitudes(canvasSize: IntSize) = withContext(Dispatchers.Default) {
        val maxHeight = canvasSize.height.toFloat()
        val spikes = with(density) { (canvasSize.width / spikeTotalWidth.toPx()).toInt() }
        val drawableAmps = amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes,
            maxHeight = maxHeight,
            minHeight = MIN_SPIKE_HEIGHT,
        )
        _spikeAmplitudes.value = drawableAmps
    }

    private suspend fun List<Int>.toDrawableAmplitudes(
        amplitudeType: AmplitudeType,
        spikes: Int,
        minHeight: Float,
        maxHeight: Float,
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
    }

}
