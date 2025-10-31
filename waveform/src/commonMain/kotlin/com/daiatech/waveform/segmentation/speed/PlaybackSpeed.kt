package com.daiatech.waveform.segmentation.speed

/**
 * Playback speed options
 *
 * @property float speed multiplier value
 */
enum class PlaybackSpeed(val float: Float) {
    /** Quarter speed playback */
    X0_25(0.25F),

    /** Half speed playback */
    X0_50(0.50F),

    /** Normal speed playback */
    X1_00(1.00F),
}