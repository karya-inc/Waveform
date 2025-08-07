package com.daiatech.waveform.segmentation.v2

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * State that changes with interaction
 */
data class AudioSegmentationUiState(val amplitudes: List<Int>) {
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
}


