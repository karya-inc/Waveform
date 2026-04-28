package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArrowRight: ImageVector
    get() {
        if (_ArrowRight != null) {
            return _ArrowRight!!
        }
        _ArrowRight = ImageVector.Builder(
            name = "ArrowRight",
            defaultWidth = 32.dp,
            defaultHeight = 26.dp,
            viewportWidth = 32f,
            viewportHeight = 26f
        ).apply {
            path(fill = SolidColor(Color(0xFF52BE8D))) {
                moveTo(2.29f, 15.04f)
                curveTo(2.29f, 15.04f, 2.53f, 15.04f, 2.68f, 15.04f)
                lineTo(24.7f, 15.04f)
                curveTo(24.7f, 15.04f, 24.61f, 15.16f, 24.58f, 15.19f)
                curveTo(22.39f, 17.38f, 20.2f, 19.57f, 18.01f, 21.76f)
                curveTo(16.99f, 22.78f, 17.2f, 24.4f, 18.46f, 25.09f)
                curveTo(19.24f, 25.51f, 20.2f, 25.39f, 20.83f, 24.76f)
                curveTo(22.18f, 23.41f, 23.53f, 22.06f, 24.88f, 20.71f)
                curveTo(26.95f, 18.64f, 28.99f, 16.6f, 31.06f, 14.53f)
                curveTo(31.81f, 13.78f, 31.99f, 12.79f, 31.48f, 11.95f)
                curveTo(31.36f, 11.77f, 31.21f, 11.59f, 31.06f, 11.44f)
                curveTo(27.7f, 8.08f, 24.31f, 4.69f, 20.95f, 1.33f)
                curveTo(20.02f, 0.4f, 18.67f, 0.43f, 17.86f, 1.39f)
                curveTo(17.17f, 2.2f, 17.2f, 3.37f, 17.95f, 4.12f)
                curveTo(18.82f, 5.02f, 19.69f, 5.89f, 20.59f, 6.76f)
                curveTo(21.94f, 8.11f, 23.29f, 9.43f, 24.61f, 10.78f)
                curveTo(24.64f, 10.81f, 24.67f, 10.84f, 24.73f, 10.93f)
                lineTo(24.49f, 10.93f)
                curveTo(18.01f, 10.93f, 8.95f, 10.93f, 2.47f, 10.93f)
                curveTo(1.57f, 10.93f, 0.88f, 11.35f, 0.52f, 12.19f)
                curveTo(-0.08f, 13.51f, 0.91f, 15.01f, 2.35f, 15.04f)
                lineTo(2.29f, 15.04f)
                close()
            }
        }.build()

        return _ArrowRight!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowRight: ImageVector? = null
