package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Data class to represent the full color scheme for the audio editing component.
 */
data class SegmentationColors(
    val background: Color,                // Main background (Black)
    val fadeColor: Color,                 // Overlay gradient start
    val contentPrimary: Color,            // White/Light Grey text and active waveform
    val markerColor: Color,               // Timeline numbers and markers
    val waveformColor: Color,             // Amplitude spikes
    val playheadIndicator: Color,         // The vertical red line
    val selectionOutline: Color,          // The yellow-green selection box outline
    val inactiveSelectionOutline: Color,  // The gray-green selection box outline for inactive segment
    val trimHandleStart: Color,           // The blue start handle
    val trimHandleEnd: Color,             // The olive green end handle
    val buttonBackgroundPrimary: Color,   // Dark Grey button background
    val buttonBackgroundPlay: Color,      // White play button circle
    val iconSecondary: Color,             // White icons inside the dark grey buttons
    val segmentAddedBackground: Color     // Olive green background for the segment play button
)

/**
 * Provides a default set of ComponentColors, optionally overriding them.
 * The defaults are chosen to visually match the provided image.
 */
@Composable
fun segmentationColors(
    background: Color = Color(0xFF001F24), // Solid Black
    fadeColor: Color = Color(0xFF00363D), // Solid Black
    contentPrimary: Color = Color(0xFFFFFFFF), // White
    markerColor: Color = Color(0xFF008857), // Medium Grey
    waveformColor: Color = Color(0xFFD5D5D5), // Amplitude spikes
    playheadIndicator: Color = Color(0xFFDE3730), // Bright Red
    selectionOutline: Color = Color(0xFFC8E56E), // Yellow-Green
    inactiveSelectionOutline: Color = Color(0xFF959595), // Yellow-Green
    trimHandleStart: Color = Color(0xFF007BCB), // Bright Blue
    trimHandleEnd: Color = Color(0xFF688010), // Olive Green
    buttonBackgroundPrimary: Color = Color(0xFF222222), // Dark Grey
    buttonBackgroundPlay: Color = Color(0xFFFFFFFF), // White
    iconSecondary: Color = Color(0xFFFFFFFF), // White
    segmentAddedBackground: Color = Color(0xFF00363D), // Olive Green
): SegmentationColors {
    return SegmentationColors(
        background = background,
        fadeColor = fadeColor,
        contentPrimary = contentPrimary,
        markerColor = markerColor,
        waveformColor = waveformColor,
        playheadIndicator = playheadIndicator,
        selectionOutline = selectionOutline,
        inactiveSelectionOutline = inactiveSelectionOutline,
        trimHandleStart = trimHandleStart,
        trimHandleEnd = trimHandleEnd,
        buttonBackgroundPrimary = buttonBackgroundPrimary,
        buttonBackgroundPlay = buttonBackgroundPlay,
        iconSecondary = iconSecondary,
        segmentAddedBackground = segmentAddedBackground
    )
}