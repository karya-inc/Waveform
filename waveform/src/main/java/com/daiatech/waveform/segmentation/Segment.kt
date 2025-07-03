package com.daiatech.waveform.segmentation

internal typealias Segment = Pair<Long, Long>

internal val Pair<Long, Long>.start get() = this.first

internal val Pair<Long, Long>.end get() = this.second
