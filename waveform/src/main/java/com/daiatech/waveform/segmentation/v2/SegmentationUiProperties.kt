package com.daiatech.waveform.segmentation.v2

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Initial properties of the UI
 */
data class SegmentationUiProperties(
    val spikeWidth: Dp = 2.dp,
    val spikePadding: Dp = 2.dp,
    val spikeMaxHeight: Dp = 48.dp,
)

data class Colors(
    val containerColor: Color = Color(0xFF002827),
    val primaryContentColor: Color = Color(0xFFFCFCFC),
    val actionsColor: Color = Color(0xFF009688)
)