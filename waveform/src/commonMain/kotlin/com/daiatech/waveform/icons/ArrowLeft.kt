package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArrowLeft: ImageVector
    get() {
        if (_ArrowLeft != null) {
            return _ArrowLeft!!
        }
        _ArrowLeft = ImageVector.Builder(
            name = "ArrowLeft",
            defaultWidth = 32.dp,
            defaultHeight = 26.dp,
            viewportWidth = 32f,
            viewportHeight = 26f
        ).apply {
            path(fill = SolidColor(Color(0xFF52BE8D))) {
                moveTo(29.71f, 10.96f)
                curveTo(29.71f, 10.96f, 29.47f, 10.96f, 29.32f, 10.96f)
                horizontalLineTo(7.3f)
                curveTo(7.3f, 10.96f, 7.39f, 10.84f, 7.42f, 10.81f)
                curveTo(9.61f, 8.62f, 11.8f, 6.43f, 13.99f, 4.24f)
                curveTo(15.01f, 3.22f, 14.8f, 1.6f, 13.54f, 0.91f)
                curveTo(12.76f, 0.49f, 11.8f, 0.61f, 11.17f, 1.24f)
                curveTo(9.82f, 2.59f, 8.47f, 3.94f, 7.12f, 5.29f)
                curveTo(5.05f, 7.36f, 3.01f, 9.4f, 0.94f, 11.47f)
                curveTo(0.19f, 12.22f, 0.01f, 13.21f, 0.52f, 14.05f)
                curveTo(0.64f, 14.23f, 0.79f, 14.41f, 0.94f, 14.56f)
                curveTo(4.3f, 17.92f, 7.69f, 21.31f, 11.05f, 24.67f)
                curveTo(11.98f, 25.6f, 13.33f, 25.57f, 14.14f, 24.61f)
                curveTo(14.83f, 23.8f, 14.8f, 22.63f, 14.05f, 21.88f)
                curveTo(13.18f, 20.98f, 12.31f, 20.11f, 11.41f, 19.24f)
                curveTo(10.06f, 17.89f, 8.71f, 16.57f, 7.39f, 15.22f)
                curveTo(7.36f, 15.19f, 7.33f, 15.16f, 7.27f, 15.07f)
                horizontalLineTo(7.51f)
                curveTo(13.99f, 15.07f, 23.05f, 15.07f, 29.53f, 15.07f)
                curveTo(30.43f, 15.07f, 31.12f, 14.65f, 31.48f, 13.81f)
                curveTo(32.08f, 12.49f, 31.09f, 10.99f, 29.65f, 10.96f)
                horizontalLineTo(29.71f)
                close()
            }
        }.build()

        return _ArrowLeft!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowLeft: ImageVector? = null
