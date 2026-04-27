package com.daiatech.waveform.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun WaveformPulse(
    modifier: Modifier = Modifier,
    spikeWidth: Dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 2.dp,
    minSpikeHeight: Dp = 1.dp,
    color: Color = Color.Gray,
    shimmerColor: Color = Color.White,
    animationDuration: Int = 500
) {
    val density = LocalDensity.current
    val spikeWidthPx = remember { with(density) { spikeWidth.toPx() } }
    val spikeRadiusPx = remember { with(density) { spikeRadius.toPx() } }
    val spikePaddingPx = remember { with(density) { spikePadding.toPx() } }
    val minSpikeHeightPx = remember { with(density) { minSpikeHeight.toPx() } }

    WaveformPulse(
        modifier = modifier,
        spikeWidthPx = spikeWidthPx,
        spikeRadiusPx = spikeRadiusPx,
        spikePaddingPx = spikePaddingPx,
        minSpikeHeightPx = minSpikeHeightPx,
        color = color,
        shimmerColor = shimmerColor,
        animationDuration = animationDuration
    )
}

@Composable
fun WaveformPulse(
    modifier: Modifier = Modifier,
    spikeWidthPx: Float,
    spikeRadiusPx: Float = 6f,
    spikePaddingPx: Float = 12f,
    minSpikeHeightPx: Float = 3f,
    color: Color = Color.Gray,
    shimmerColor: Color = Color.White,
    animationDuration: Int = 500
) {
    val spikes = remember { mutableStateListOf<Float>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val spikeCornerRadius = remember { CornerRadius(spikeRadiusPx) }

    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Calculate spikes when canvas size changes
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val noOfSpikes = (canvasSize.width / (spikeWidthPx + spikePaddingPx)).toInt()

            if (noOfSpikes > 0) {
                spikes.clear()
                val maxSpikeHeight = canvasSize.height
                val minHeight = minSpikeHeightPx.roundToInt()
                repeat(noOfSpikes) {
                    spikes.add((minHeight..maxSpikeHeight).random().toFloat())
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                canvasSize = coordinates.size
            }
    ) {
        val graphWidth = size.width
        val graphHeight = size.height

        // Draw spikes
        spikes.forEachIndexed { idx, spikeHeight ->
            val x = idx * (spikeWidthPx + spikePaddingPx)
            val y = (graphHeight - spikeHeight) / 2f

            // Calculate shimmer effect for this spike
            val spikeCenter = (x + spikeWidthPx / 2) / graphWidth
            val distanceFromShimmer = abs(spikeCenter - shimmerOffset)
            val shimmerWidth = 0.3f // Width of the shimmer wave
            val shimmerIntensity = (1f - (distanceFromShimmer / shimmerWidth).coerceIn(0f, 1f))
                .coerceAtLeast(0f)

            // Interpolate between base color and shimmer color
            val spikeColor = lerp(color, shimmerColor, shimmerIntensity)

            drawRoundRect(
                brush = SolidColor(spikeColor),
                topLeft = Offset(x, y),
                size = Size(spikeWidthPx, spikeHeight),
                cornerRadius = spikeCornerRadius,
                style = Fill
            )
        }
    }
}

@Preview
@Composable
fun WaveformPulsePreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Using Dp version
        WaveformPulse(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            color = Color(0xFF6200EE),
            shimmerColor = Color(0xFFBB86FC),
            spikeWidth = 2.dp
        )

        // Using Px version
        WaveformPulse(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            spikeWidthPx = 8f,
            spikeRadiusPx = 4f,
            spikePaddingPx = 8f,
            minSpikeHeightPx = 5f,
            color = Color(0xFF018786),
            shimmerColor = Color(0xFF03DAC5)
        )

        // Thicker spikes with Dp
        WaveformPulse(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            spikeWidth = 6.dp,
            spikePadding = 6.dp,
            color = Color.DarkGray,
            shimmerColor = Color.White,
            animationDuration = 1500
        )

        // Px version with custom dimensions
        WaveformPulse(
            modifier = Modifier
                .width(200.dp)
                .height(48.dp),
            spikeWidthPx = 10f,
            spikeRadiusPx = 5f,
            spikePaddingPx = 10f,
            color = Color(0xFFFF6B6B),
            shimmerColor = Color(0xFFFFE66D)
        )
    }
}