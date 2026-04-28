package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Mdarrowforwardios: ImageVector
    get() {
        if (_Mdarrowforwardios != null) {
            return _Mdarrowforwardios!!
        }
        _Mdarrowforwardios = ImageVector.Builder(
            name = "Mdarrowforwardios",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4.153f, 13.487f)
                lineTo(5.333f, 14.667f)
                lineTo(12f, 8f)
                lineTo(5.333f, 1.333f)
                lineTo(4.153f, 2.513f)
                lineTo(9.64f, 8f)
                lineTo(4.153f, 13.487f)
                close()
            }
        }.build()

        return _Mdarrowforwardios!!
    }

@Suppress("ObjectPropertyName")
private var _Mdarrowforwardios: ImageVector? = null
