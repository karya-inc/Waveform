package com.daiatech.waveform.segmentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.daiatech.karya.ui.buttons.ButtonVariation
import com.daiatech.karya.ui.buttons.KButton
import com.daiatech.karya.ui.buttons.KIconButton
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.common.WaveformPulse
import com.daiatech.waveform.models.Segment
import com.daiatech.waveform.safeDiv
import com.daiatech.waveform.segmentation.component.PlaybackToolbar
import com.daiatech.waveform.segmentation.component.SegmentToolbar
import com.daiatech.waveform.segmentation.speed.PlaybackSpeed
import com.daiatech.waveform.segmentation.zoom.Zoom
import com.daiatech.waveform.times
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Audio waveform segment picker with playback controls
 *
 * Displays scrollable waveform with zoom, playback, and segment selection.
 * Shows timestamp markers, inactive segments, and center playhead indicator.
 *
 * @param state segment picker state managing waveform and selection
 * @param progressMs current playback position in milliseconds
 * @param isPlaying whether full audio is playing
 * @param togglePlayback toggles full audio playback
 * @param isSegmentPlaying whether selected segment is playing
 * @param toggleSegmentPlayback toggles segment playback
 * @param seek adjusts playback position by milliseconds
 * @param speed current playback speed
 * @param updateSpeed updates playback speed
 * @param colors color scheme for UI elements
 */
@Composable
fun AudioSegmentPicker(
    state: SegmentPickerState,
    progressMs: Long,
    isPlaying: Boolean,
    togglePlayback: () -> Unit,
    isSegmentPlaying: Boolean,
    toggleSegmentPlayback: () -> Unit,
    seek: (Long) -> Unit,
    speed: PlaybackSpeed,
    updateSpeed: (PlaybackSpeed) -> Unit,
    colors: SegmentationColors = segmentationColors()
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { state.calculateDrawableAmplitudes() }

    val layout = remember { state.layout }
    val spikeWidthPx = remember { layout.spikeWidthPx }
    val spikePaddingPx = remember { layout.spikePaddingPx }
    val graphHeightPx = remember { layout.graphHeightPx }
    val spikeTotalWidthPx = remember { layout.spikeTotalWidthPx }
    val spikeCornerRadius = remember { layout.spikeCornerRadius }
    val windowCornerRadius = remember { layout.windowCornerRadius }
    val durationMs = remember { state.durationMs }
    val inactiveSegments = remember { state.inactive }
    val processing by state.processing
    val segment by state.segment
    val zoom by state.zoom
    val drawableAmplitudes by state.drawableAmplitudes

    var screenWidth by remember { mutableIntStateOf(0) }

    val markersTextStyle = remember {
        TextStyle(fontSize = markerFontSize, color = colors.markerColor)
    }

    val spikesCountBetweenTwoTimestampMarkers = remember(zoom) {
        noOfSpikesInTwoTimestamps(zoom)
    }

    val noOfSpikes = remember(
        durationMs,
        spikesCountBetweenTwoTimestampMarkers
    ) {
        ((spikesCountBetweenTwoTimestampMarkers * durationMs) / DURATION_MS_BETWEEN_TIMESTAMP).toInt()
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

    val centerMarkerPath = remember(screenWidth, density, spikeWidthPx) {
        if (screenWidth == 0) return@remember Path()
        with(density) {
            val centerX = screenWidth / 2
            val vSpacing = verticalItemSpacing.toPx()
            val spikeW = spikeWidthPx / 2
            val totalHeight = (vSpacing * 7 + MIN_GRAPH_HEIGHT.toPx() + 2 * markerFontSize.toPx())

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Box(
            modifier = Modifier
                .height(canvasHeightDp)
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    screenWidth = layoutCoordinates.size.width
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            val seekBy = state.pxToDuration(dragAmount)
                            seek(-seekBy) // left swipe means seek forward and conversely
                            change.consume()
                        }
                    )
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(colors.fadeColor, Color.Transparent)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width / 2, size.height)
                    )
                    drawPath(
                        path = centerMarkerPath,
                        brush = SolidColor(colors.playheadIndicator)
                    )
                }
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
                    animationDuration = 1000
                )
            }

            if (!processing) {
                Canvas(
                    modifier = Modifier
                        .height(canvasHeightDp)
                        .fillMaxWidth()
                        .graphicsLayer { translationX = canvasOffset }
                ) {

                    // visible range based on screen width and offset
                    // when canvas is scrolled, we need to find which spikes are visible on screen
                    val screenWidthPx = screenWidth.toFloat()
                    val buffer = screenWidthPx * 0.5f // 50% buffer on each side
                    // visible canvas range: [-canvasOffset - buffer, -canvasOffset + screenWidth + buffer]
                    val visibleCanvasStart = -canvasOffset - buffer
                    val visibleCanvasEnd = visibleCanvasStart + screenWidthPx + (buffer * 2)

                    // Convert canvas coordinates to spike indices
                    val visibleStart = (visibleCanvasStart / spikeTotalWidthPx).toInt()
                        .coerceAtLeast(0)
                    val visibleEnd = ((visibleCanvasEnd / spikeTotalWidthPx).toInt() + 1)
                        .coerceAtMost(drawableAmplitudes.size)

                    // Draw only visible amplitudes
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
                            style = Fill
                        )

                        val timeInSeconds = state.pxToDuration(x).toFloat() / 1000
                        // draw timestamp each 0.5s: 0, 0.5, 1.0, 1.5, 2.0 ...
                        if (((timeInSeconds * 100).toInt() % 50) == 0) {
                            drawRoundRect(
                                brush = SolidColor(colors.markerColor),
                                topLeft = Offset(x, layout.markerY),
                                size = Size(spikeWidthPx, layout.markerHeight),
                                cornerRadius = spikeCornerRadius,
                                style = Fill
                            )

                            val timeText = "${timeInSeconds}s"
                            val tm = textMeasurer.measure(timeText, markersTextStyle)
                            val y = if ((2 * timeInSeconds).toInt() % 2 == 0) {
                                layout.evenMarkerY
                            } else {
                                layout.oddMarkerY
                            }

                            drawText(
                                textMeasurer = textMeasurer,
                                style = markersTextStyle,
                                text = timeText,
                                topLeft = Offset(x - (tm.size.width.toFloat() / 2), y),
                                size = Size(
                                    width = tm.size.width.toFloat(),
                                    height = tm.size.height.toFloat()
                                )
                            )
                        }
                    }

                    segment?.let { segment ->
                        val startPx = state.durationToPx(segment.start)
                        val endPx = state.durationToPx(segment.end)
                        val width = endPx - startPx
                        println("SegmentPickerState:: Window: $startPx, $endPx")
                        drawSegmentWindow(
                            cornerRadius = windowCornerRadius,
                            topLeft = Offset(startPx, layout.graphY),
                            size = Size(width, graphHeightPx),
                            colors = colors,
                            style = Stroke(spikeWidthPx)
                        )
                    }

                    inactiveSegments.forEach { segment ->
                        val startPx = state.durationToPx(segment.start)
                        val endPx = state.durationToPx(segment.end)
                        val width = endPx - startPx
                        drawRoundRect(
                            brush = SolidColor(colors.inactiveSelectionOutline),
                            topLeft = Offset(x = startPx, y = layout.graphY),
                            size = Size(width, graphHeightPx),
                            cornerRadius = windowCornerRadius,
                            style = Stroke(spikeWidthPx)
                        )
                        drawRoundRect(
                            color = colors.inactiveSelectionOutline.copy(0.2f),
                            topLeft = Offset(x = startPx, y = layout.graphY),
                            size = Size(width, layout.graphHeightPx),
                            cornerRadius = windowCornerRadius,
                        )
                    }
                }
            }
        }

        PlaybackToolbar(
            colors = colors,
            progressMs = progressMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            enableZoomIn = zoom != Zoom.max,
            enableZoomOut = zoom != Zoom.min,
            speed = speed,
            togglePlayback = togglePlayback,
            onZoomIn = { coroutineScope.launch { state.zoomIn() } },
            onZoomOut = { coroutineScope.launch { state.zoomOut() } },
            updateSpeed = updateSpeed,
        )

        segment?.let { segment ->
            SegmentToolbar(
                modifier = Modifier.fillMaxWidth(),
                segment = segment,
                isPlaying = isSegmentPlaying,
                togglePlayback = toggleSegmentPlayback,
                moveStart = { by -> coroutineScope.launch { state.addToStart(by) } },
                moveEnd = { by -> coroutineScope.launch { state.addToEnd(by) } },
                colors = colors
            )
        }
    }
}

/**
 * Draws segment selection window with trim handles
 *
 * Renders semi-transparent overlay, outline, and circular trim handles
 * at start and end positions.
 *
 * @param cornerRadius corner radius for rounded rectangle
 * @param topLeft top-left position of window
 * @param size dimensions of window
 * @param colors color scheme for window elements
 * @param style stroke style for outline
 */
fun DrawScope.drawSegmentWindow(
    cornerRadius: CornerRadius,
    topLeft: Offset,
    size: Size,
    colors: SegmentationColors,
    style: Stroke
) {
    drawRoundRect(
        color = colors.selectionOutline.copy(0.2f),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        brush = SolidColor(colors.selectionOutline),
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
        style = style
    )
    drawCircle(
        color = colors.contentPrimary,
        radius = 8.dp.toPx(),
        center = Offset(topLeft.x, topLeft.y + size.height / 2)
    )
    drawCircle(
        color = colors.trimHandleStart,
        radius = 6.dp.toPx(),
        center = Offset(topLeft.x, topLeft.y + size.height / 2)
    )
    drawCircle(
        color = colors.contentPrimary,
        radius = 8.dp.toPx(),
        center = Offset(
            x = topLeft.x + size.width,
            y = topLeft.y + size.height / 2
        )
    )
    drawCircle(
        color = colors.trimHandleEnd,
        radius = 6.dp.toPx(),
        center = Offset(
            x = topLeft.x + size.width,
            y = topLeft.y + size.height / 2
        )
    )
}

/**
 * Preview of audio segment picker with mock data
 *
 * Demonstrates full functionality with simulated playback,
 * segment selection, and controls.
 *
 * @param colors color scheme for UI elements
 */
@Composable
fun AudioSegmentPickerPreview(
    colors: SegmentationColors = segmentationColors()
) {
    val coroutineScope = rememberCoroutineScope()
    val progressJobRef = remember { mutableStateOf<Job?>(null) }

    // Keep playback related variables/methods out of state
    var progressMs by remember { mutableLongStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isSegmentPlaying by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(PlaybackSpeed.X1_00) }

    val state = rememberSegmentPickerState(
        amplitudes = listOf(100, 200, 300, 500, 100, 20).times(20),
        durationMs = 8000,
        minimumSegmentMs = 500,
        inactive = listOf(Segment(0, 1000))
    )

    Surface {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            AudioSegmentPicker(
                state = state,
                colors = colors,
                progressMs = progressMs,
                isPlaying = isPlaying,
                togglePlayback = {
                    if (isSegmentPlaying) {
                        progressJobRef.value?.cancel()
                        isSegmentPlaying = false
                    }

                    if (isPlaying) {
                        progressJobRef.value?.cancel()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        progressJobRef.value = coroutineScope.launch {
                            while (progressMs < 8000L && isPlaying) {
                                progressMs += (50.times(speed.float)).toLong()
                                delay(50)
                            }
                            if (progressMs >= 8000L) {
                                progressMs = 0L
                            }
                            isPlaying = false
                        }
                    }
                },
                seek = { it ->
                    progressJobRef.value?.cancel()
                    progressMs = (progressMs + it).coerceIn(0, 8000)
                },
                updateSpeed = { speed = it },
                speed = speed,
                isSegmentPlaying = isSegmentPlaying,
                toggleSegmentPlayback = {
                    if (isPlaying) {
                        progressJobRef.value?.cancel()
                        isPlaying = false
                    }

                    if (isSegmentPlaying) {
                        progressJobRef.value?.cancel()
                        isSegmentPlaying = false
                    } else {
                        val segment = state.segment.value
                        if (segment != null) {
                            println("Segment snapshot is not null")
                            isSegmentPlaying = true
                            progressJobRef.value?.cancel()
                            progressMs = segment.start
                            progressJobRef.value = coroutineScope.launch {
                                while (progressMs < segment.end && isSegmentPlaying) {
                                    progressMs += (50.times(speed.float)).toLong()
                                    delay(50)
                                }
                                if (progressMs >= segment.end) {
                                    progressMs = segment.start
                                }
                                isSegmentPlaying = false
                            }
                        }
                    }
                }
            )

            Row(Modifier.fillMaxWidth().background(colors.background).padding(8.dp)) {
                AnimatedVisibility(visible = state.segment.value == null) {
                    KButton(
                        content = "Add Segment",
                        buttonVariation = ButtonVariation.PrimaryButtonRegular,
                        onClick = { state.addSegment(progressMs) }
                    )
                }

                AnimatedVisibility(visible = state.segment.value != null) {
                    Row(Modifier.fillMaxWidth()) {
                        KIconButton(
                            onClick = { state.removeSegment() },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            },
                            buttonVariation = ButtonVariation.IconSecondaryButtonRegular
                        )
                        Spacer(Modifier.width(8.dp))
                        KButton(
                            modifier = Modifier.weight(1f),
                            content = "Submit Segment",
                            buttonVariation = ButtonVariation.PrimaryButtonRegular,
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}
