package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Marker1: ImageVector
    get() {
        if (_Marker1 != null) {
            return _Marker1!!
        }
        _Marker1 = ImageVector.Builder(
            name = "Marker1",
            defaultWidth = 25.dp,
            defaultHeight = 66.dp,
            viewportWidth = 25f,
            viewportHeight = 66f
        ).apply {
            path(fill = SolidColor(Color(0xFF02FF00))) {
                moveTo(11f, 0f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(66f)
                horizontalLineToRelative(-3f)
                close()
            }
            path(fill = SolidColor(Color(0xFF02FF00))) {
                moveTo(12.5f, 34.5f)
                moveToRelative(-12.5f, 0f)
                arcToRelative(12.5f, 12.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 25f, 0f)
                arcToRelative(12.5f, 12.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -25f, 0f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.295f, 28.364f)
                verticalLineTo(40f)
                horizontalLineTo(12.886f)
                verticalLineTo(29.841f)
                horizontalLineTo(12.818f)
                lineTo(9.977f, 31.727f)
                verticalLineTo(30.295f)
                lineTo(12.886f, 28.364f)
                horizontalLineTo(14.295f)
                close()
            }
        }.build()

        return _Marker1!!
    }

@Suppress("ObjectPropertyName")
private var _Marker1: ImageVector? = null
