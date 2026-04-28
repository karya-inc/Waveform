package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Mdarrowbackwardios: ImageVector
    get() {
        if (_Mdarrowbackwardios != null) {
            return _Mdarrowbackwardios!!
        }
        _Mdarrowbackwardios = ImageVector.Builder(
            name = "Mdarrowbackwardios",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(11.847f, 2.513f)
                lineTo(10.667f, 1.333f)
                lineTo(4f, 8f)
                lineTo(10.667f, 14.667f)
                lineTo(11.847f, 13.487f)
                lineTo(6.36f, 8f)
                lineTo(11.847f, 2.513f)
                close()
            }
        }.build()

        return _Mdarrowbackwardios!!
    }

@Suppress("ObjectPropertyName")
private var _Mdarrowbackwardios: ImageVector? = null
