package com.daiatech.waveform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val minSpikeWidthDp: Dp = 1.dp
internal val maxSpikeWidthDp: Dp = 24.dp

internal val minSpikePaddingDp: Dp = 0.dp
internal val maxSpikePaddingDp: Dp = 12.dp

internal val minSpikeRadiusDp: Dp = 0.dp
internal val maxSpikeRadiusDp: Dp = 12.dp

internal val removeCircleRadius: Dp = 8.dp

internal val touchTargetSize = 12.dp

internal val MIN_GRAPH_HEIGHT: Dp = 48.dp

internal const val MIN_PROGRESS: Float = 0F
internal const val MAX_PROGRESS: Float = 1F

internal const val MIN_SPIKE_HEIGHT: Float = 0.5f

internal const val ON_TAP_ADJUSTMENT_MS = 10
internal const val ON_LONG_TAP_ADJUSTMENT_MS = 50

const val AUDIO_PLAYER_REFRESH_RATE_MS = 10L

internal val markerColor = Color(0xFF02FF00).copy(0.4f)