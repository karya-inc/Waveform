package com.daiatech.waveform.models

data class Segment(
    val start: Long,
    val end: Long
) {
    val asPair: Pair<Long, Long>
        get() = Pair(start, end)
}
