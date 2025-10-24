package com.daiatech.waveform.segmentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.daiatech.waveform.models.waveformColors

@Preview
@Composable
private fun AudioSegmentPickerPrev1Android() {
    AudioSegmentPickerPreview()
}

@Preview
@Composable
private fun AudioSegmentPickerPreview2Android() {
    AudioSegmentPickerPreview(
        colors = waveformColors(
            containerColor = Color(0xFF001F24),
            markerColor = Color(0xFF008857),
            waveformColor = Color(0xFFD5D5D5),
            primaryProgressColor = Color(0xFFDE3730),
            fadeColor = Color(0xFF00363D),
            buttonColor = Color(0xFF008857),
            activeWindowColor = Color(0xFFC8E56E)
        )
    )
}
