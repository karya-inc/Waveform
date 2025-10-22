package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Immutable layout dimensions for the waveform graph.
 * Cached to prevent unnecessary recalculations.
 */
@Immutable
internal data class WaveformLayout(
    val canvasHeight: Dp,
    val evenMarkerY: Float,
    val oddMarkerY: Float,
    val markerY: Float,
    val markerHeight: Float,
    val spikesOffset: Float
)
