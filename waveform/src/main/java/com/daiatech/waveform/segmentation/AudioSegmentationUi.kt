package com.daiatech.waveform.segmentation

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.waveform.AUDIO_PLAYER_REFRESH_RATE_MS
import com.daiatech.waveform.AudioPlayer
import com.daiatech.waveform.ON_LONG_TAP_ADJUSTMENT_MS
import com.daiatech.waveform.ON_TAP_ADJUSTMENT_MS
import com.daiatech.waveform.R
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.toSecsAndMs
import com.daiatech.waveform.touchTargetSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.ranges.coerceIn

internal val TEXT_HEIGHT_PADDING = 12.dp

@Composable
fun AudioSegmentationUi(
    modifier: Modifier = Modifier,
    state: SegmentationState,
    colors: SegmentationColors = segmentationColors(),
    markersCount: Int = 10,
    addSegmentText: String = "+ Add",
    removeSegmentText: String = "- Remove",
    undoText: String = "Undo",
    redoText: String = "Redo",
    mergeText: String = "Merge",
    clearAllText: String = "Clear All"
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

    val durationMs = state.durationMs
    val activeSegment = state.activeSegment.value?.let(state.segments::get)

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
        state.activeSegment.value?.let(state.segments::get)
    ) {
        state.computeZoomedInDrawableAmplitudes(spikes, spikesMultiplier)
    }

    val exoPlayer = remember(state.audioFilePath) { ExoPlayer.Builder(context).build() }
    var isPlaying by remember(state.audioFilePath) { mutableStateOf(false) }
    var isPaused by remember(state.audioFilePath) { mutableStateOf(false) }
    var topPlayerProgress by remember(state.audioFilePath) { mutableLongStateOf(0L) }
    var segmentPlaybackProgress by remember(state.audioFilePath) { mutableLongStateOf(0L) }

    DisposableEffect(key1 = state.audioFilePath) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                segmentPlaybackProgress = exoPlayer.currentPosition
                state.activeSegment.value?.let(state.segments::get)?.end?.let {
                    if (exoPlayer.currentPosition >= it) exoPlayer.stop()
                }
                handler.postDelayed(this, AUDIO_PLAYER_REFRESH_RATE_MS)
            }
        }

        val listener = object : Player.Listener {
            init {
                handler.postDelayed(runnable, 0)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    ExoPlayer.STATE_ENDED, ExoPlayer.STATE_IDLE -> {
                        handler.removeCallbacks(runnable)
                        segmentPlaybackProgress = 0
                    }

                    ExoPlayer.STATE_READY -> {
                        segmentPlaybackProgress = exoPlayer.currentPosition
                    }

                    else -> {}
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                super.onIsPlayingChanged(playing)
                isPlaying = playing
                if (playing) {
                    isPaused = false
                    handler.postDelayed(runnable, AUDIO_PLAYER_REFRESH_RATE_MS)
                } else {
                    handler.removeCallbacks(runnable)
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            handler.removeCallbacks(runnable)
            exoPlayer.release()
        }
    }

    Column(
        modifier
            .background(colors.containerColor)
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Player
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioPlayer(
                audioFilePath = state.audioFilePath,
                durationMs = durationMs,
                onProgressUpdate = { topPlayerProgress = it }
            )
        }

        // Main Waveform
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(state.graphHeight)
                .pointerInput(state.audioFilePath) {
                    detectTapGestures(
                        onLongPress = state::onLongPress,
                        onTap = {
                            if (exoPlayer.isPlaying) exoPlayer.stop()
                            state.onTap(it)
                        }
                    )
                }
                .pointerInput(state.audioFilePath) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            state.onHorizontalDrag(change, dragAmount, touchTargetSize.toPx())
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

            // Each Segment Boundaries
            state.segments.forEachIndexed { idx, segment ->
                if (state.activeSegment.value != idx) {
                    val xStart = size.width / durationMs.toFloat() * segment.start
                    val xEnd = size.width / durationMs.toFloat() * segment.end
                    // draw a window from xStart to xEnd
                    drawRoundRect(
                        brush = SolidColor(colors.inactiveWindowColor),
                        topLeft = Offset(xStart, 0F),
                        size = Size(xEnd - xStart, size.height),
                        style = Stroke(width = state.spikeWidth.toPx())
                    )
                }
            }

            // Highlighted Segment boundary
            state.activeSegment.value?.let {
                val segment = state.segments[it]
                val xStart = size.width / durationMs.toFloat() * segment.start
                val xEnd = size.width / durationMs.toFloat() * segment.end

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

            // Highlighted borders for grouped segments
            state.groupedSegments.forEach {
                state.segments.getOrNull(it)?.let { segment ->
                    val xStart = size.width / durationMs.toFloat() * segment.start
                    val xEnd = size.width / durationMs.toFloat() * segment.end

                    // draw a window from xStart to xEnd
                    drawRoundRect(
                        brush = SolidColor(Color.Cyan),
                        topLeft = Offset(xStart, 0F),
                        size = Size(xEnd - xStart, size.height),
                        style = Stroke(width = state.spikeWidth.toPx())
                    )
                }
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
        AnimatedVisibility(visible = state.activeSegment.value != null && state.enableAdjustment) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
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
                                            activeSegment?.start,
                                            activeSegment?.end
                                        )
                                        exoPlayer.seekTo(seekPosition)
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
                    state.activeSegment.value?.let { segIdx ->
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
                        state.segments.getOrNull(segIdx)?.let { seg ->
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
                    }

                    if (segmentPlaybackProgress != 0L) {
                        val xCoordinate = state.durationToPx(
                            segmentPlaybackProgress,
                            activeSegment?.start,
                            activeSegment?.end
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(0.1F))
                                    .changeSegmentPosition(coroutineScope, Unit) {
                                        state.addToStart(-it)
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_arrow_left
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = colors.buttonColor
                                )
                            }
                            WindowMarker(
                                text = "1",
                                containerColor = colors.activeWindowColor,
                                contentColor = colors.windowTextColor
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(0.1F))
                                    .changeSegmentPosition(coroutineScope, Unit) {
                                        state.addToStart(it)
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_arrow_right
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = colors.buttonColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val start =
                            state.activeSegment.value?.let(state.segments::get)
                                ?.start?.let(::toSecsAndMs)
                        Text(
                            text = "${start}s",
                            color = colors.buttonColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPaused = true
                                } else {
                                    val start = activeSegment?.start
                                    val end = activeSegment?.end
                                    if (!isPaused) {
                                        val mediaItem = MediaItem.fromUri(state.audioFilePath)
                                        exoPlayer.setMediaItem(mediaItem)
                                        exoPlayer.prepare()
                                        if (start != null) {
                                            exoPlayer.seekTo(start)
                                        }
                                    }
                                    start?.let {
                                        if (exoPlayer.currentPosition < it) {
                                            exoPlayer.seekTo(
                                                it
                                            )
                                        }
                                    }
                                    end?.let {
                                        if (exoPlayer.currentPosition > it) {
                                            exoPlayer.seekTo(start!!)
                                        }
                                    }
                                    exoPlayer.play()
                                }
                            },
                            enabled = state.activeSegment.value != null
                        ) {
                            val res =
                                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            Icon(
                                painter = painterResource(id = res),
                                contentDescription = null,
                                tint = colors.buttonColor
                            )
                        }
                        val progress =
                            state.activeSegment.value?.let(state.segments::get)?.let { seg ->
                                val d = seg.end - seg.start
                                "${
                                    toSecsAndMs(
                                        (segmentPlaybackProgress - seg.start).coerceIn(0, d)
                                    )
                                }s/${toSecsAndMs(d)}s"
                            } ?: ""
                        Text(
                            text = progress,
                            color = colors.buttonColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(0.1F))
                                    .changeSegmentPosition(coroutineScope, Unit) {
                                        state.addToEnd(-it)
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_arrow_left
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = colors.buttonColor
                                )
                            }
                            WindowMarker(
                                text = "2",
                                containerColor = colors.activeWindowColor,
                                contentColor = colors.windowTextColor
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(0.1F))
                                    .changeSegmentPosition(coroutineScope, Unit) {
                                        state.addToEnd(it)
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_arrow_right
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = colors.buttonColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val end = state.activeSegment.value?.let(state.segments::get)
                            ?.end?.let(::toSecsAndMs)
                        Text(
                            text = "${end}s",
                            color = colors.buttonColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(visible = enableSpikeAmplification) {
            Slider(
                value = spikesMultiplier,
                onValueChange = { spikesMultiplier = it },
                valueRange = 1f..10f
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            val zoomButtonTint = if (enableSpikeAmplification) {
                colors.buttonColor
            } else {
                colors.buttonColor.copy(0.4F)
            }

            OutlinedButton(
                onClick = { enableSpikeAmplification = !enableSpikeAmplification },
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder().copy(
                    brush = SolidColor(zoomButtonTint)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_zoom_in),
                    contentDescription = "zoom",
                    tint = zoomButtonTint
                )
            }
            // disable adding segments when grouping
            if (state.groupedSegments.isEmpty() && state.enableAdjustment) {
                OutlinedButton(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        }
                        state.addSegment()
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                    ),
                    enabled = state.groupedSegments.isEmpty()
                ) {
                    Text(text = addSegmentText)
                }
            }

            if (state.enableAdjustment) {
                state.activeSegment.value?.let {
                    OutlinedButton(
                        onClick = { state.removeActiveSegment() },
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder().copy(
                            brush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    ) {
                        Text(text = removeSegmentText)
                    }
                }
            }

            if (state.groupedSegments.size > 1) {
                OutlinedButton(
                    onClick = { state.mergeSegments() },
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                ) {
                    Text(text = mergeText)
                }
            }

            if (state.undoList.isNotEmpty()) {
                OutlinedButton(
                    onClick = { state.undoMerge() },
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                ) {
                    Text(text = undoText)
                }
            }

            if (state.redoList.isNotEmpty()) {
                OutlinedButton(
                    onClick = { state.redoMerge() },
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                ) {
                    Text(text = redoText)
                }
            }

            if (state.segments.isNotEmpty()) {
                OutlinedButton(
                    onClick = { state.removeAllSegments() },
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                ) {
                    Text(text = clearAllText)
                }
            }
        }
    }
}

@Composable
fun WindowMarker(
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember { TextStyle(fontSize = 8.sp, color = contentColor) }
    val tm = remember { textMeasurer.measure(text, textStyle) }
    val tmSizeDp by derivedStateOf {
        with(density) {
            DpSize(
                width = tm.size.width.toDp(),
                height = tm.size.height.toDp()
            )
        }
    }
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.marker_2),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = containerColor
        )
        Canvas(Modifier.size(tmSizeDp)) {
            drawText(
                textMeasurer = textMeasurer,
                style = textStyle,
                text = text,
                size = Size(
                    width = tm.size.width.toFloat(),
                    height = tm.size.height.toFloat()
                )
            )
        }
    }
}

/**
 * Captures pointer input.
 * On Long press and hold, calls onChange continuously with a delay of 10ms until click is released
 * On Single Tap, onChange is called on time.
 */
fun Modifier.changeSegmentPosition(
    coroutineScope: CoroutineScope,
    key: Any?,
    longTap: Int = ON_LONG_TAP_ADJUSTMENT_MS,
    singleTap: Int = ON_TAP_ADJUSTMENT_MS,
    onChange: (change: Int) -> Unit
) = this.pointerInput(key) {
    var moveStartJob: Job? = null
    detectTapGestures(
        onLongPress = {
            moveStartJob = coroutineScope.launch(Dispatchers.Default) {
                while (isActive) {
                    delay(10)
                    onChange(longTap)
                }
            }
        },
        onPress = {
            if (moveStartJob == null) {
                onChange(singleTap)
            }
            tryAwaitRelease()
            moveStartJob?.cancel()
            moveStartJob = null
        }
    )
}


@Preview
@Composable
private fun WindowMarkerPrev() {
    WindowMarker(
        text = "2",
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}