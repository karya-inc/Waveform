package com.daiatech.waveform.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Marker2: ImageVector
    get() {
        if (_Marker2 != null) {
            return _Marker2!!
        }
        _Marker2 = ImageVector.Builder(
            name = "Marker2",
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
                moveTo(9.205f, 40f)
                verticalLineTo(38.977f)
                lineTo(13.045f, 34.773f)
                curveTo(13.496f, 34.28f, 13.867f, 33.852f, 14.159f, 33.489f)
                curveTo(14.451f, 33.121f, 14.667f, 32.777f, 14.807f, 32.454f)
                curveTo(14.951f, 32.129f, 15.023f, 31.788f, 15.023f, 31.432f)
                curveTo(15.023f, 31.023f, 14.924f, 30.669f, 14.727f, 30.369f)
                curveTo(14.534f, 30.07f, 14.269f, 29.839f, 13.932f, 29.676f)
                curveTo(13.595f, 29.513f, 13.216f, 29.432f, 12.795f, 29.432f)
                curveTo(12.349f, 29.432f, 11.958f, 29.525f, 11.625f, 29.71f)
                curveTo(11.295f, 29.892f, 11.04f, 30.148f, 10.858f, 30.477f)
                curveTo(10.68f, 30.807f, 10.591f, 31.193f, 10.591f, 31.636f)
                horizontalLineTo(9.25f)
                curveTo(9.25f, 30.955f, 9.407f, 30.356f, 9.722f, 29.841f)
                curveTo(10.036f, 29.326f, 10.464f, 28.924f, 11.006f, 28.636f)
                curveTo(11.551f, 28.348f, 12.163f, 28.205f, 12.841f, 28.205f)
                curveTo(13.523f, 28.205f, 14.127f, 28.348f, 14.653f, 28.636f)
                curveTo(15.18f, 28.924f, 15.593f, 29.313f, 15.892f, 29.801f)
                curveTo(16.191f, 30.29f, 16.341f, 30.833f, 16.341f, 31.432f)
                curveTo(16.341f, 31.86f, 16.263f, 32.278f, 16.108f, 32.688f)
                curveTo(15.956f, 33.093f, 15.691f, 33.546f, 15.313f, 34.046f)
                curveTo(14.938f, 34.542f, 14.417f, 35.148f, 13.75f, 35.864f)
                lineTo(11.136f, 38.659f)
                verticalLineTo(38.75f)
                horizontalLineTo(16.545f)
                verticalLineTo(40f)
                horizontalLineTo(9.205f)
                close()
            }
        }.build()

        return _Marker2!!
    }

@Suppress("ObjectPropertyName")
private var _Marker2: ImageVector? = null
