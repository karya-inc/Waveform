package com.daiatech.waveform.segmentation.zoom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.Res
import com.daiatech.waveform.fizoomin
import com.daiatech.waveform.fizoomout
import org.jetbrains.compose.resources.painterResource

@Composable
fun ZoomButton(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    enableZoomIn: Boolean = true,
    enableZoomOut: Boolean = true
) {
    Row(modifier = modifier.clip(CircleShape), verticalAlignment = Alignment.CenterVertically) {
        ZoomButtonItem(
            modifier = Modifier.weight(1f),
            onClick = onZoomIn,
            enabled = enableZoomIn,
            icon = painterResource(Res.drawable.fizoomin),
            tint = Color.White,
            contentDescription = "Zoom In"
        )
        ZoomButtonItem(
            modifier = Modifier.weight(1f),
            onClick = onZoomOut,
            enabled = enableZoomOut,
            icon = painterResource(Res.drawable.fizoomout),
            tint = Color.White,
            contentDescription = "Zoom out"
        )
    }
}

@Composable
fun ZoomButtonItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    icon: Painter,
    tint: Color,
    contentDescription: String?
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(Color.White.copy(alpha = if (enabled) 0.10f else 0.15f))
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ZoomButtonPreview() {
    var zoomLevel by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001F24)),
        contentAlignment = Alignment.Center
    ) {
        ZoomButton(
            onZoomIn = { zoomLevel += 0.1f },
            onZoomOut = { zoomLevel = (zoomLevel - 0.1f).coerceAtLeast(0.1f) },
            enableZoomIn = true,
            enableZoomOut = false
        )
    }
}