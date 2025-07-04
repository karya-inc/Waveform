package com.daiatech.waveform.marker

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.segmentation.SegmentationColors
import com.daiatech.waveform.segmentation.TEXT_HEIGHT_PADDING
import com.daiatech.waveform.segmentation.end
import com.daiatech.waveform.segmentation.segmentationColors
import com.daiatech.waveform.segmentation.start
import com.daiatech.waveform.toSecsAndMs

// TODO: Complete this
@Composable
fun AudioMarkerUi(
    modifier: Modifier = Modifier,
    state: AudioMarkerState,
    topPlayerProgress: Long,
    segmentPlaybackProgress: Long,
    markersCount: Int = 10,
    colors: SegmentationColors = segmentationColors()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember { TextStyle(fontSize = 12.sp, color = colors.windowTextColor) }
    val markersTextStyle = remember { TextStyle(fontSize = 12.sp, color = colors.markerColor) }
    val textMeasure1 = remember { textMeasurer.measure("1", textStyle) }
    val textMeasure2 = remember { textMeasurer.measure("2", textStyle) }

    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    LaunchedEffect(canvasSize) { state.canvasSize = canvasSize }

    var enableSpikeAmplification by remember { mutableStateOf(false) }
    var spikes by remember { mutableIntStateOf(0) }
    var spikesMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(state, spikes, spikesMultiplier) {
        state.computeDrawableAmplitudes(spikes, spikesMultiplier)
    }
    LaunchedEffect(
        state,
        state.activeSegment.value,
        spikes,
        spikesMultiplier,
        state.activeSegment.value
    ) {
        state.computeZoomedInDrawableAmplitudes(spikes, spikesMultiplier)
    }

    Column(modifier) {
        // Main Waveform
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(state.graphHeight)
                .pointerInput(state.audioFilePath) {
                    detectTapGestures(
                        // onLongPress = state::onLongPress,
                        onTap = { // FIXME
                            // if (exoPlayer.isPlaying) exoPlayer.stop()
                            // state.onTap(it)
                        }
                    )
                }
                .pointerInput(state.audioFilePath) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            // FIXME
                            // state.onHorizontalDrag(change, dragAmount, touchTargetSize.toPx())
                        }
                    )
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

            // Each Marker
            state.markers.forEachIndexed { idx, marker ->
                val xCoordinate = (size.width / state.durationMs.toFloat()) * marker
                drawLine(
                    brush = SolidColor(colors.inactiveWindowColor),
                    start = Offset(xCoordinate, 0F),
                    end = Offset(xCoordinate, size.height),
                    strokeWidth = state.spikeWidth.toPx(),
                )
            }

            // Highlighted Segment boundary
            state.activeSegment.value?.let { segment ->
                val xStart = size.width / state.durationMs.toFloat() * segment.start
                val xEnd = size.width / state.durationMs.toFloat() * segment.end

                // draw a window from xStart to xEnd
                drawRoundRect(
                    brush = SolidColor(colors.activeWindowColor),
                    topLeft = Offset(xStart, 0F),
                    size = Size(xEnd - xStart, size.height),
                    style = Stroke(width = state.spikeWidth.toPx())
                )

                // circle on the start-edge
                drawCircle(
                    center = Offset(xStart, size.height / 2),
                    radius = 8.dp.toPx(),
                    brush = SolidColor(colors.activeWindowColor)
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
                    brush = SolidColor(colors.activeWindowColor)
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


            // Segment playback progress bar
            if (segmentPlaybackProgress != 0L) {
                val xCoordinate = state.durationToPx(segmentPlaybackProgress)
                drawLine(
                    brush = SolidColor(colors.secondaryProgressColor),
                    start = Offset(xCoordinate, size.height.times(0.2f)),
                    end = Offset(xCoordinate, size.height.times(0.8f)),
                    strokeWidth = state.spikeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Entire audio playback progress bar
            if (topPlayerProgress != 0L) {
                val xCoordinate = state.durationToPx(topPlayerProgress)
                drawLine(
                    brush = SolidColor(colors.primaryProgressColor),
                    start = Offset(xCoordinate, 0F),
                    end = Offset(xCoordinate, size.height),
                    strokeWidth = state.progressBarWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInteropFilter {
                    return@pointerInteropFilter when (it.action) {
                        MotionEvent.ACTION_DOWN,
                        MotionEvent.ACTION_MOVE -> {
                            if (it.x in 0F..canvasSize.width) {
                                val seekPosition = state.pxToDuration(
                                    it.x,
                                    state.activeSegment.value.start,
                                    state.activeSegment.value.end
                                )
                                // exoPlayer.seekTo(seekPosition)
                                true
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
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

            // draw lines every 10ms
            state.activeSegment.value.let { seg ->
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
                    state.activeSegment.value.start,
                    state.activeSegment.value.end
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
        Spacer(Modifier.height(TEXT_HEIGHT_PADDING))
    }
}

@Preview
@Composable
private fun AudioMarkerUiPrev() {
    Surface {
        AudioMarkerUi(
            state = rememberAudioMarkerState(
                audioFilePath = "",
                durationMs = 5000L,
                amplitudes = listOf(100, 200, 300, 5000, 100, 20),
            ),
            topPlayerProgress = 0L,
            segmentPlaybackProgress = 0L
        )
    }
}