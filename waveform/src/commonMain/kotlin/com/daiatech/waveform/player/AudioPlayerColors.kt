package com.daiatech.waveform.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AudioPlayerColors(
    val background: Color,
    val fadeColor: Color,
    val contentPrimary: Color,
    val markerColor: Color,
    val waveformColor: Color,
    val playheadIndicator: Color,
)

@Composable
fun audioPlayerColors(
    background: Color = Color(0xFF001F24),
    fadeColor: Color = Color(0xFF00363D),
    contentPrimary: Color = Color(0xFFFFFFFF),
    markerColor: Color = Color(0xFF008857),
    waveformColor: Color = Color(0xFFD5D5D5),
    playheadIndicator: Color = Color(0xFFDE3730),
): AudioPlayerColors = AudioPlayerColors(
    background = background,
    fadeColor = fadeColor,
    contentPrimary = contentPrimary,
    markerColor = markerColor,
    waveformColor = waveformColor,
    playheadIndicator = playheadIndicator,
)
