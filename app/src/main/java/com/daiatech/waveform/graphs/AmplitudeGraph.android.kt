package com.daiatech.waveform.graphs

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AmplitudeBarGraphPrev() {
    Surface {
        AmplitudeBarGraph(
            amplitudes = listOf(100, 200, 300, 500, 100, 20),
            onProgressChange = {}
        )
    }
}