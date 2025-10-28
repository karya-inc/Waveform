package com.daiatech.waveform.segmentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.daiatech.karya.ui.buttons.ButtonVariation
import com.daiatech.karya.ui.buttons.KButton
import com.daiatech.karya.ui.buttons.KIconButton
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.models.Segment
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.safeDiv
import com.daiatech.waveform.segmentation.component.PlaybackToolbar
import com.daiatech.waveform.segmentation.component.SegmentToolbar
import com.daiatech.waveform.segmentation.speed.PlaybackSpeed
import com.daiatech.waveform.times
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *
 * @param colors Custom colors for waveform elements.
 * @param waveformAlignment Alignment of spikes relative to the graph height.
 * @param spikeAnimationSpec Animation specification for spike height transitions.
 * @param progressMs Current time progress in milliseconds.
 */
@Composable
fun AudioSegmentPicker(
    state: SegmentPickerState,
    colors: SegmentationColors = segmentationColors(),
    progressMs: Long,
    isPlaying: Boolean,
    togglePlayback: () -> Unit,
    isSegmentPlaying: Boolean,
    toggleSegmentPlayback: () -> Unit,
    seek: (Long) -> Unit,
    speed: PlaybackSpeed,
    updateSpeed: (PlaybackSpeed) -> Unit,
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val markersTextStyle = remember {
        TextStyle(fontSize = markerFontSize, color = colors.markerColor)
    }

    var screenWidth by remember { mutableIntStateOf(0) }
    val screenWidthDp by derivedStateOf { with(density) { screenWidth.toDp() } }

    val durationMs = state.durationMs
    val canvasWidthDp = state.canvasWidthDp.value
    val spikeWidthPx = state.spikeWidthPx
    val spikeTotalWidthPx = state.spikeTotalWidthPx
    val layout = state.layout
    val spikeCornerRadius = state.spikeCornerRadius
    val windowCornerRadius = state.windowCornerRadius
    val enableZoomIn = state.enableZoomIn.value
    val enableZoomOut = state.enableZoomOut.value
    val timestampMs = state.timestampMs.value
    val drawableAmplitudes = state.drawableAmplitudes.value
    val window = state.window.value
    val inactiveSegment = state.inactive
    val segment = state.segment.value
    val spikeCountPerTimestampMs = state.spikeCountPerTimestampMs

    val canvasOffset by derivedStateOf {
        screenWidthDp / 2 - ((progressMs.toFloat() safeDiv durationMs.toFloat())
            .coerceIn(0f, 1f)) * canvasWidthDp
    }

    val centerMarkerPath = remember(screenWidth) {
        if (screenWidth == 0) return@remember Path()

        with(density) {
            val centerX = screenWidth / 2
            val vSpacing = verticalItemSpacing.toPx()
            val spikeW = spikeWidthPx / 2
            val totalHeight =
                (vSpacing * 7 + MIN_GRAPH_HEIGHT.toPx() + 2 * markerFontSize.toPx())

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
        Row(
            modifier = Modifier
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
            Canvas(
                modifier = Modifier
                    .width(canvasWidthDp)
                    .height(layout.canvasHeight)
                    .offset(canvasOffset)
            ) {
                drawableAmplitudes.forEachIndexed { index, amplitude ->
                    drawRoundRect(
                        brush = SolidColor(colors.waveformColor),
                        topLeft = amplitude.first,
                        size = Size(spikeWidthPx, amplitude.second),
                        cornerRadius = spikeCornerRadius,
                        style = Fill
                    )

                    // Draw timestamp markers
                    if (index % spikeCountPerTimestampMs == 0) {
                        drawRoundRect(
                            brush = SolidColor(colors.markerColor),
                            topLeft = Offset(amplitude.first.x, layout.markerY),
                            size = Size(spikeWidthPx, layout.markerHeight),
                            cornerRadius = spikeCornerRadius,
                            style = Fill
                        )

                        val timeInSeconds =
                            (index * timestampMs) / (spikeCountPerTimestampMs * 1000f)
                        val timeText = "${timeInSeconds}s"
                        val tm = textMeasurer.measure(timeText, markersTextStyle)
                        val y = if (index % (2 * spikeCountPerTimestampMs) == 0) {
                            layout.evenMarkerY
                        } else {
                            layout.oddMarkerY
                        }

                        drawText(
                            textMeasurer = textMeasurer,
                            style = markersTextStyle,
                            text = timeText,
                            topLeft = Offset(amplitude.first.x - (tm.size.width.toFloat() / 2), y),
                            size = Size(
                                width = tm.size.width.toFloat(),
                                height = tm.size.height.toFloat()
                            )
                        )
                    }
                }

                window?.let { window ->
                    drawSegmentWindow(
                        cornerRadius = windowCornerRadius,
                        window = window,
                        colors = colors,
                        style = Stroke(spikeWidthPx)
                    )
                }

                inactiveSegment.forEach { segment ->
                    val startPx = state.durationToPx(segment.start)
                    val endPx = state.durationToPx(segment.end)
                    val width = endPx - startPx
                    drawRoundRect(
                        brush = SolidColor(colors.inactiveSelectionOutline),
                        topLeft = Offset(x = startPx, y = layout.spikesOffset),
                        size = Size(width, state.graphHeightPx),
                        cornerRadius = windowCornerRadius,
                        style = Stroke(spikeWidthPx)
                    )
                    drawRoundRect(
                        color = colors.inactiveSelectionOutline.copy(0.2f),
                        topLeft = Offset(x = startPx, y = layout.spikesOffset),
                        size = Size(width, state.graphHeightPx),
                        cornerRadius = windowCornerRadius,
                    )
                }
            }
        }

        PlaybackToolbar(
            colors = colors,
            progressMs = progressMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            enableZoomIn = enableZoomIn,
            enableZoomOut = enableZoomOut,
            speed = speed,
            togglePlayback = togglePlayback,
            onZoomIn = state::zoomIn,
            onZoomOut = state::zoomOut,
            updateSpeed = updateSpeed,
        )

        segment?.let { segment ->
            SegmentToolbar(
                modifier = Modifier.fillMaxWidth(),
                segment = segment,
                isPlaying = isSegmentPlaying,
                togglePlayback = toggleSegmentPlayback,
                moveStart = { by -> state.addToStart(by) },
                moveEnd = { by -> state.addToEnd(by) },
                colors = colors
            )
        }
    }
}

fun DrawScope.drawSegmentWindow(
    cornerRadius: CornerRadius,
    window: Pair<Offset, Size>,
    colors: SegmentationColors,
    style: Stroke
) {
    drawRoundRect(
        color = colors.selectionOutline.copy(0.2f),
        topLeft = window.first,
        size = window.second,
        cornerRadius = cornerRadius
    )
    drawRoundRect(
        brush = SolidColor(colors.selectionOutline),
        topLeft = window.first,
        size = window.second,
        cornerRadius = cornerRadius,
        style = style
    )
    drawCircle(
        color = colors.contentPrimary,
        radius = 8.dp.toPx(),
        center = Offset(window.first.x, window.first.y + window.second.height / 2)
    )
    drawCircle(
        color = colors.trimHandleStart,
        radius = 6.dp.toPx(),
        center = Offset(window.first.x, window.first.y + window.second.height / 2)
    )
    drawCircle(
        color = colors.contentPrimary,
        radius = 8.dp.toPx(),
        center = Offset(
            x = window.first.x + window.second.width,
            y = window.first.y + window.second.height / 2
        )
    )
    drawCircle(
        color = colors.trimHandleEnd,
        radius = 6.dp.toPx(),
        center = Offset(
            x = window.first.x + window.second.width,
            y = window.first.y + window.second.height / 2
        )
    )
}

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
