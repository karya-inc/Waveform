package com.daiatech.waveform.segmetation2

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class SegmentationColors(
    val activeWindowColor: Color,
    val inactiveWindowColor: Color,
    val windowTextColor: Color,
    val buttonColor: Color,
    val containerColor: Color,
    val waveformColor: Color,
    val markerColor: Color,
    val primaryProgressColor: Color,
    val secondaryProgressColor: Color,
)


@Composable
fun segmentationColors(
    activeWindowColor: Color = MaterialTheme.colorScheme.inversePrimary,
    inactiveWindowColor: Color = MaterialTheme.colorScheme.inversePrimary.copy(0.5f),
    windowTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.background,
    waveformColor: Color = (MaterialTheme.colorScheme.primary),
    markerColor: Color = MaterialTheme.colorScheme.secondary,
    primaryProgressColor: Color = (MaterialTheme.colorScheme.secondary),
    secondaryProgressColor: Color = (MaterialTheme.colorScheme.tertiary),
): SegmentationColors {
    return SegmentationColors(
        activeWindowColor = activeWindowColor,
        inactiveWindowColor = inactiveWindowColor,
        windowTextColor = windowTextColor,
        buttonColor = buttonColor,
        containerColor = containerColor,
        waveformColor = waveformColor,
        markerColor = markerColor,
        primaryProgressColor = primaryProgressColor,
        secondaryProgressColor = secondaryProgressColor
    )
}