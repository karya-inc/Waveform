package com.daiatech.waveform.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.daiatech.waveform.common.WaveformPulse
import com.daiatech.waveform.safeDiv
import com.daiatech.waveform.segmentation.DURATION_MS_BETWEEN_TIMESTAMP
import com.daiatech.waveform.segmentation.markerFontSize
import com.daiatech.waveform.segmentation.noOfSpikesInTwoTimestamps
import com.daiatech.waveform.segmentation.verticalItemSpacing
import com.daiatech.waveform.times

@Composable
fun PlayerWaveform(
    state: AudioPlayerState,
    progressMs: Long,
    seek: (Long) -> Unit,
    colors: AudioPlayerColors = audioPlayerColors(),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    LaunchedEffect(Unit) { state.calculateDrawableAmplitudes() }

    val layout = remember { state.layout }
    val spikeWidthPx = remember { layout.spikeWidthPx }
    val spikePaddingPx = remember { layout.spikePaddingPx }
    val graphHeightPx = remember { layout.graphHeightPx }
    val spikeTotalWidthPx = remember { layout.spikeTotalWidthPx }
    val spikeCornerRadius = remember { layout.spikeCornerRadius }
    val durationMs = remember { state.durationMs }
    val processing by state.processing
    val zoom by state.zoom
    val drawableAmplitudes by state.drawableAmplitudes

    var screenWidth by remember { mutableIntStateOf(0) }

    val markersTextStyle = remember {
        TextStyle(fontSize = markerFontSize, color = colors.markerColor)
    }

    val noOfSpikes = remember(zoom, durationMs) {
        ((noOfSpikesInTwoTimestamps(zoom) * durationMs) / DURATION_MS_BETWEEN_TIMESTAMP).toInt()
    }

    val canvasWidthPx = remember(spikeTotalWidthPx, noOfSpikes) {
        spikeTotalWidthPx * noOfSpikes
    }

    var canvasOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progressMs, screenWidth, canvasWidthPx, durationMs) {
        if (screenWidth > 0) {
            canvasOffset = screenWidth / 2 - canvasWidthPx *
                ((progressMs.toFloat() safeDiv durationMs.toFloat()).coerceIn(0f, 1f))
        }
    }

    val canvasHeightDp = remember { with(density) { layout.canvasHeightPx.toDp() } }

    val centerMarkerPath = remember(screenWidth, graphHeightPx, spikeWidthPx) {
        if (screenWidth == 0) return@remember Path()
        with(density) {
            val centerX = screenWidth / 2
            val vSpacing = verticalItemSpacing.toPx()
            val spikeW = spikeWidthPx / 2
            val totalHeight = vSpacing * 7 + graphHeightPx + 2 * markerFontSize.toPx()

            Path().apply {
                moveTo(centerX - vSpacing, 0f)
                lineTo(centerX + vSpacing, 0f)
                lineTo(centerX + spikeW, vSpacing)
                lineTo(centerX + spikeW, totalHeight)
                lineTo(centerX + vSpacing, totalHeight + vSpacing)
                lineTo(centerX - vSpacing, totalHeight + vSpacing)
                lineTo(centerX - spikeW, totalHeight)
                lineTo(centerX - spikeW, vSpacing)
                close()
            }
        }
    }

    Box(
        modifier = modifier
            .height(canvasHeightDp)
            .fillMaxWidth()
            .background(colors.background)
            .onGloballyPositioned { screenWidth = it.size.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (!processing) {
                        seek(-state.pxToDuration(dragAmount))
                        change.consume()
                    }
                }
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.linearGradient(listOf(colors.fadeColor, Color.Transparent)),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width / 2, size.height),
                )
                drawPath(path = centerMarkerPath, brush = SolidColor(colors.playheadIndicator))
            },
    ) {
        if (processing) {
            WaveformPulse(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { graphHeightPx.toDp() })
                    .graphicsLayer {
                        translationY = layout.graphY
                        translationX = canvasOffset
                    },
                color = colors.waveformColor.copy(0.2f),
                spikeWidthPx = spikeWidthPx.times(2),
                spikePaddingPx = spikePaddingPx,
                animationDuration = 1000,
            )
        }

        if (!processing) {
            Canvas(
                modifier = Modifier
                    .height(canvasHeightDp)
                    .fillMaxWidth()
                    .graphicsLayer { translationX = canvasOffset },
            ) {
                val screenWidthPx = screenWidth.toFloat()
                val buffer = screenWidthPx * 0.5f
                val visibleCanvasStart = -canvasOffset - buffer
                val visibleCanvasEnd = visibleCanvasStart + screenWidthPx + (buffer * 2)

                val visibleStart = (visibleCanvasStart / spikeTotalWidthPx).toInt()
                    .coerceAtLeast(0)
                val visibleEnd = ((visibleCanvasEnd / spikeTotalWidthPx).toInt() + 1)
                    .coerceAtMost(drawableAmplitudes.size)

                for (index in visibleStart until visibleEnd) {
                    if (index >= drawableAmplitudes.size) break

                    val amplitude = drawableAmplitudes[index]
                    val x = index * spikeTotalWidthPx
                    val y = (graphHeightPx - amplitude) / 2f + layout.graphY

                    drawRoundRect(
                        brush = SolidColor(colors.waveformColor),
                        topLeft = Offset(x, y),
                        size = Size(spikeWidthPx, amplitude),
                        cornerRadius = spikeCornerRadius,
                        style = Fill,
                    )

                    val timeInSeconds = state.pxToDuration(x).toFloat() / 1000
                    if (((timeInSeconds * 100).toInt() % 50) == 0) {
                        drawRoundRect(
                            brush = SolidColor(colors.markerColor),
                            topLeft = Offset(x, layout.markerY),
                            size = Size(spikeWidthPx, layout.markerHeight),
                            cornerRadius = spikeCornerRadius,
                            style = Fill,
                        )

                        val timeText = "${timeInSeconds}s"
                        val tm = textMeasurer.measure(timeText, markersTextStyle)
                        val textY = if ((2 * timeInSeconds).toInt() % 2 == 0) {
                            layout.evenMarkerY
                        } else {
                            layout.oddMarkerY
                        }
                        drawText(
                            textMeasurer = textMeasurer,
                            style = markersTextStyle,
                            text = timeText,
                            topLeft = Offset(x - (tm.size.width.toFloat() / 2), textY),
                            size = Size(
                                width = tm.size.width.toFloat(),
                                height = tm.size.height.toFloat(),
                            ),
                        )
                    }
                }
            }
        }
    }
}
