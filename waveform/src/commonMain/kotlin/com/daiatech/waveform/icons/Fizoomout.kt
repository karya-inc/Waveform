package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Fizoomout: ImageVector
    get() {
        if (_Fizoomout != null) {
            return _Fizoomout!!
        }
        _Fizoomout = ImageVector.Builder(
            name = "Fizoomout",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(7.333f, 2.667f)
                curveTo(4.756f, 2.667f, 2.667f, 4.756f, 2.667f, 7.333f)
                curveTo(2.667f, 9.911f, 4.756f, 12f, 7.333f, 12f)
                curveTo(9.911f, 12f, 12f, 9.911f, 12f, 7.333f)
                curveTo(12f, 4.756f, 9.911f, 2.667f, 7.333f, 2.667f)
                close()
                moveTo(1.333f, 7.333f)
                curveTo(1.333f, 4.02f, 4.02f, 1.333f, 7.333f, 1.333f)
                curveTo(10.647f, 1.333f, 13.333f, 4.02f, 13.333f, 7.333f)
                curveTo(13.333f, 10.647f, 10.647f, 13.333f, 7.333f, 13.333f)
                curveTo(4.02f, 13.333f, 1.333f, 10.647f, 1.333f, 7.333f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(10.629f, 10.629f)
                curveTo(10.889f, 10.368f, 11.311f, 10.368f, 11.571f, 10.629f)
                lineTo(14.471f, 13.529f)
                curveTo(14.732f, 13.789f, 14.732f, 14.211f, 14.471f, 14.471f)
                curveTo(14.211f, 14.732f, 13.789f, 14.732f, 13.529f, 14.471f)
                lineTo(10.629f, 11.571f)
                curveTo(10.368f, 11.311f, 10.368f, 10.889f, 10.629f, 10.629f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(4.667f, 7.333f)
                curveTo(4.667f, 6.965f, 4.965f, 6.667f, 5.333f, 6.667f)
                horizontalLineTo(9.333f)
                curveTo(9.702f, 6.667f, 10f, 6.965f, 10f, 7.333f)
                curveTo(10f, 7.702f, 9.702f, 8f, 9.333f, 8f)
                horizontalLineTo(5.333f)
                curveTo(4.965f, 8f, 4.667f, 7.702f, 4.667f, 7.333f)
                close()
            }
        }.build()

        return _Fizoomout!!
    }

@Suppress("ObjectPropertyName")
private var _Fizoomout: ImageVector? = null
