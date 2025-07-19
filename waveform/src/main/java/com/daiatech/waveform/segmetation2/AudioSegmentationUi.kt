package com.daiatech.waveform.segmetation2

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.models.WaveformColors
import com.daiatech.waveform.models.waveformColors
import com.daiatech.waveform.segmentation.TEXT_HEIGHT_PADDING
import com.daiatech.waveform.segmentation.end
import com.daiatech.waveform.segmentation.start
import com.daiatech.waveform.toSecsAndMs
import com.daiatech.waveform.touchTargetSize
import kotlinx.coroutines.launch

// TODO: 1. Compute zoomed-in amps only when the dragging has stopped
//       2. Add +- 50ms for window

/**
 * This component enables workers to pick a segment of audio
 */
@Composable
fun AudioSegmentPicker(
    modifier: Modifier = Modifier,
    state: SegmentPickerState,
    mainPlayerProgress: Long,
    segmentPlaybackProgress: Long,
    toggleSegmentPlayback: (ActiveWindow, Segment) -> Unit,
    colors: WaveformColors = waveformColors(),
    markersCount: Int = 10
) {
    val textMeasurer = rememberTextMeasurer()
    val coroutineScope = rememberCoroutineScope()
    val textStyle = remember { TextStyle(fontSize = 12.sp, color = colors.windowTextColor) }
    val markersTextStyle = remember { TextStyle(fontSize = 12.sp, color = colors.markerColor) }
    val textMeasure1 = remember { textMeasurer.measure("1", textStyle) }
    val textMeasure2 = remember { textMeasurer.measure("2", textStyle) }
    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    LaunchedEffect(canvasSize) { state.canvasSize = canvasSize }
    var spikes by remember { mutableIntStateOf(0) }
    var spikesMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(state, spikes, spikesMultiplier) {
        state.computeDrawableAmplitudes(spikes, spikesMultiplier)
    }
    LaunchedEffect(
        state,
        state.window.value,
        spikes,
        spikesMultiplier,
    ) {
        state.computeZoomedInDrawableAmplitudes(spikes, spikesMultiplier)
    }

    val (segmentXStart, segmentXEnd) = remember(
        state.segment.value,
        state.window.value
    ) {
        val x = state.durationToPx(
            time = state.segment.value.start,
            start = state.window.value.start,
            end = state.window.value.end
        )
        val xe = state.durationToPx(
            time = state.segment.value.end,
            start = state.window.value.start,
            end = state.window.value.end
        )
        x to xe
    }

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(state.graphHeight)
                .pointerInput(state.audioFilePath) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            if (state.activeWindow.value == ActiveWindow.WINDOW) {
                                coroutineScope.launch {
                                    state.onWindowDrag(change, dragAmount, touchTargetSize.toPx())
                                }
                            }
                        }
                    )
                }
                .pointerInput(state.audioFilePath) {
                    detectTapGestures { state.onSelect(ActiveWindow.WINDOW) }
                }
        ) {
            canvasSize = size
            spikes = (size.width / state.spikeTotalWidth.toPx()).toInt()

            // Main Waveform for entire audio
            state.spikeAmplitude.value.forEachIndexed { index, amplitude ->
                drawRoundRect(
                    brush = SolidColor(colors.waveformColor),
                    topLeft = Offset(
                        x = index * state.spikeTotalWidth.toPx(),
                        y = when (state.waveformAlignment) {
                            WaveformAlignment.Top -> 0F
                            WaveformAlignment.Bottom -> size.height - amplitude
                            WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
                        }
                    ),
                    size = Size(
                        width = state.spikeWidth.toPx(),
                        height = amplitude
                    ),
                    cornerRadius = CornerRadius(state.spikeRadius.toPx(), state.spikeRadius.toPx()),
                    style = state.style
                )
            }

            // Segment playback progress bar
            if (segmentPlaybackProgress != 0L) {
                val xCoordinate = state.durationToPx(segmentPlaybackProgress)
                drawLine(
                    brush = SolidColor(colors.secondaryProgressColor),
                    start = Offset(xCoordinate, size.height.times(0.3f)),
                    end = Offset(xCoordinate, size.height.times(0.7f)),
                    strokeWidth = state.spikeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Entire audio playback progress bar
            if (mainPlayerProgress != 0L) {
                val xCoordinate = state.durationToPx(mainPlayerProgress)
                drawLine(
                    brush = SolidColor(colors.primaryProgressColor),
                    start = Offset(xCoordinate, size.height.times(0.1f)),
                    end = Offset(xCoordinate, size.height.times(0.9f)),
                    strokeWidth = state.progressBarWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Show segment on the minimap
            state.segment.value.let { segment ->
                val xStart = size.width / state.durationMs.toFloat() * segment.start
                val xEnd = size.width / state.durationMs.toFloat() * segment.end
                // draw a window from xStart to xEnd
                drawRoundRect(
                    brush = SolidColor(colors.secondaryProgressColor.copy(0.4F)),
                    topLeft = Offset(xStart, size.height.times(0.3F)),
                    size = Size(xEnd - xStart, size.height.times(0.7F)),
                    style = Fill
                )
            }

            // Highlighted Segment boundary
            state.window.value.let { segment ->
                val xStart = size.width / state.durationMs.toFloat() * segment.start
                val xEnd = size.width / state.durationMs.toFloat() * segment.end

                // draw a window from xStart to xEnd
                drawRoundRect(
                    brush = SolidColor(colors.activeWindowColor.copy(0.6f)),
                    topLeft = Offset(xStart, 0F),
                    size = Size(xEnd - xStart, size.height),
                    style = Fill
                )

                if (state.activeWindow.value == ActiveWindow.WINDOW) {
                    drawRoundRect(
                        brush = SolidColor(colors.buttonColor),
                        topLeft = Offset(xStart, 0F),
                        size = Size(xEnd - xStart, size.height),
                        style = Stroke(width = state.spikeWidth.toPx())
                    )
                }

                // circle on the start-edge
                drawCircle(
                    center = Offset(xStart, size.height / 2),
                    radius = 8.dp.toPx(),
                    brush = SolidColor(colors.buttonColor)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    style = textStyle,
                    text = "1",
                    topLeft = Offset(
                        x = xStart - textMeasure1.size.width / 2,
                        y = size.height / 2 - textMeasure1.size.height / 2
                    ),
                    size = Size(
                        width = textMeasure1.size.width.toFloat(),
                        height = textMeasure1.size.height.toFloat()
                    )
                )

                // circle on the end-edge
                drawCircle(
                    center = Offset(xEnd, size.height / 2),
                    radius = 8.dp.toPx(),
                    brush = SolidColor(colors.buttonColor)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    style = textStyle,
                    text = "2",
                    topLeft = Offset(
                        x = xEnd - textMeasure2.size.width / 2,
                        y = size.height / 2 - textMeasure2.size.height / 2
                    ),
                    size = Size(
                        width = textMeasure2.size.width.toFloat(),
                        height = textMeasure2.size.height.toFloat()
                    )
                )
            }


        }
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(state.audioFilePath) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            if (state.activeWindow.value == ActiveWindow.SEGMENT) {
                                coroutineScope.launch {
                                    state.onWindowDrag(change, dragAmount, touchTargetSize.toPx())
                                }
                            }
                        }
                    )
                }
                .pointerInput(state.audioFilePath) {
                    detectTapGestures { state.onSelect(ActiveWindow.SEGMENT) }
                }
                .height(state.graphHeight + TEXT_HEIGHT_PADDING.times(2))
        ) {
            state.zoomedInAmplitudes.value.forEachIndexed { index, amplitude ->
                drawRoundRect(
                    brush = SolidColor(colors.waveformColor),
                    topLeft = Offset(
                        x = index * state.spikeTotalWidth.toPx(),
                        y = when (state.waveformAlignment) {
                            WaveformAlignment.Top -> 0F
                            WaveformAlignment.Bottom -> size.height - amplitude
                            WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
                        } + TEXT_HEIGHT_PADDING.toPx()
                    ),
                    size = Size(
                        width = state.spikeWidth.toPx(),
                        height = amplitude
                    ),
                    cornerRadius = CornerRadius(
                        state.spikeRadius.toPx(),
                        state.spikeRadius.toPx()
                    ),
                    style = state.style
                )
            }
            drawRoundRect(
                brush = SolidColor(colors.secondaryProgressColor.copy(0.6f)),
                topLeft = Offset(
                    segmentXStart,
                    (state.graphHeight * 0.3F + TEXT_HEIGHT_PADDING).toPx()
                ),
                size = Size(
                    segmentXEnd - segmentXStart,
                    (state.graphHeight * 0.7F + TEXT_HEIGHT_PADDING).toPx()
                ),
                style = Fill
            )
            if (state.activeWindow.value == ActiveWindow.SEGMENT) {
                drawRoundRect(
                    brush = SolidColor(colors.secondaryProgressColor),
                    topLeft = Offset(
                        segmentXStart,
                        (state.graphHeight * 0.3F + TEXT_HEIGHT_PADDING).toPx()
                    ),
                    size = Size(
                        segmentXEnd - segmentXStart,
                        (state.graphHeight * 0.7F + TEXT_HEIGHT_PADDING).toPx()
                    ),
                    style = Stroke(width = state.spikeWidth.toPx())
                )
            }

            // draw lines every 10ms
            state.window.value.let { seg ->
                val duration = seg.end - seg.start
                (0..markersCount).forEach { t ->
                    val time = seg.start + duration / markersCount * t
                    val timeString = "${toSecsAndMs(time)}s"
                    val xCoordinate = state.durationToPx(time, seg.start, seg.end)

                    drawLine(
                        brush = SolidColor(colors.markerColor),
                        start = Offset(xCoordinate, TEXT_HEIGHT_PADDING.toPx()),
                        end = Offset(xCoordinate, size.height),
                        strokeWidth = state.spikeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                    val tm = textMeasurer.measure(
                        text = timeString,
                        style = markersTextStyle
                    )

                    val x = when (t) {
                        0 -> xCoordinate
                        markersCount -> xCoordinate - tm.size.width.toFloat()
                        else -> xCoordinate - tm.size.width.toFloat() / 2
                    }
                    val y = if (t % 2 == 0) 0F else size.height

                    drawText(
                        textMeasurer = textMeasurer,
                        style = markersTextStyle,
                        text = timeString,
                        topLeft = Offset(x, y),
                        size = Size(
                            width = tm.size.width.toFloat(),
                            height = tm.size.height.toFloat()
                        )
                    )
                }
            }

            if (segmentPlaybackProgress != 0L) {
                val xCoordinate = state.durationToPx(
                    segmentPlaybackProgress,
                    state.window.value.start,
                    state.window.value.end
                )
                drawLine(
                    brush = SolidColor(colors.secondaryProgressColor),
                    start = Offset(xCoordinate, TEXT_HEIGHT_PADDING.toPx()),
                    end = Offset(xCoordinate, size.height),
                    strokeWidth = state.progressBarWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SegmentationActions(
            colors = colors,
            start = state.window.value.start,
            end = state.window.value.end,
            isPlaying = false,
            progressMs = 0L,
            togglePlayback = {
                val segment = when (state.activeWindow.value) {
                    ActiveWindow.WINDOW -> state.window.value
                    ActiveWindow.SEGMENT -> state.segment.value
                }
                toggleSegmentPlayback(state.activeWindow.value, segment)
            },
            addToStart = state::addToStart,
            addToEnd = state::addToEnd
        )
    }
}

@Preview
@Composable
private fun AudioSegmentationUi2Prev() {
    Surface {
        val state = rememberAudioSegmentPickerState(
            audioFilePath = "",
            durationMs = 500,
            amplitudes = listOf(200, 300, 500, 1000, 10, 20, 90, 100, 114, 23, 20, 18),
            segment = Segment(9, 200),
            window = Segment(40, 300)
        )
        AudioSegmentPicker(
            state = state,
            mainPlayerProgress = 10,
            segmentPlaybackProgress = 20,
            toggleSegmentPlayback = { _, _ -> }
        )
    }
}