package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.Dp

/**
 * Immutable layout dimensions for the waveform graph.
 * Cached to prevent unnecessary recalculations.
 *
 * @property spikeRadiusPx The radius of each spike in pixels.
 * @property spikeWidthPx The width of each spike in pixels.
 * @property spikePaddingPx The padding between spikes in pixels.
 * @property canvasHeightPx The height of the waveform canvas in dp.
 * @property evenMarkerY The y-coordinate of the even timestamp marker's text.
 * @property oddMarkerY The y-coordinate of the odd timestamp marker's text.
 * @property markerY The y-coordinate of the timestamp marker.
 * @property markerHeight The height of the timestamp marker's text.
 * @property graphY The offset of the waveform graph from the top of the canvas in pixels.
 */
@Immutable
internal data class WaveformLayout(
    val spikeRadiusPx: Float,
    val spikeWidthPx: Float,
    val spikePaddingPx: Float,
    val canvasHeightPx: Float,
    val graphHeightPx: Float,
    val evenMarkerY: Float,
    val oddMarkerY: Float,
    val markerY: Float,
    val markerHeight: Float,
    val graphY: Float,
    var windowRadiusPx: Float
) {
    /**
     * The total width of the spikes.
     */
    val spikeTotalWidthPx = spikeWidthPx + spikePaddingPx

    /**
     * The corner radius of the spikes.
     */
    val spikeCornerRadius = CornerRadius(spikeRadiusPx, spikeRadiusPx)

    /**
     * The corner radius of the segment window on top of the waveform
     */
    var windowCornerRadius = CornerRadius(windowRadiusPx, windowRadiusPx)
}
