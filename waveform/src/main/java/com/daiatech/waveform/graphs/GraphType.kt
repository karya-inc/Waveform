package com.daiatech.waveform.graphs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

sealed interface GraphType {
    class Bar(
        val barGap: Dp,
        val barColor: Color
    ) : GraphType

    class Line(
        val strokeWidth: Dp,
        val strokeColor: Color
    ) : GraphType
}
