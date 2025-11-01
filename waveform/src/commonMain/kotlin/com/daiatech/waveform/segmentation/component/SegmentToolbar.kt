package com.daiatech.waveform.segmentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.ON_LONG_TAP_ADJUSTMENT_MS
import com.daiatech.waveform.ON_TAP_ADJUSTMENT_MS
import com.daiatech.waveform.Res
import com.daiatech.waveform.mdarrowbackwardios
import com.daiatech.waveform.mdarrowforwardios
import com.daiatech.waveform.models.Segment
import com.daiatech.waveform.segmentation.EnableSegmentButton
import com.daiatech.waveform.segmentation.SegmentationColors
import com.daiatech.waveform.segmentation.segmentationColors
import com.daiatech.waveform.toMinSecMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Toolbar for selected segment controls
 *
 * Displays segment start/end times with adjustment buttons,
 * playback control, and duration in three-column layout.
 *
 * @param modifier modifier for toolbar container
 * @param segment selected segment with start and end times
 * @param isPlaying whether segment is currently playing
 * @param togglePlayback toggles segment playback
 * @param moveStart adjusts segment start by milliseconds
 * @param moveEnd adjusts segment end by milliseconds
 * @param colors color scheme for UI elements
 */
@Composable
fun SegmentToolbar(
    modifier: Modifier = Modifier,
    segment: Segment,
    isPlaying: Boolean,
    togglePlayback: () -> Unit,
    moveStart: (by: Int) -> Unit,
    moveEnd: (by: Int) -> Unit,
    enabled: EnableSegmentButton,
    colors: SegmentationColors
) {

    Row(
        modifier = modifier.background(colors.segmentAddedBackground).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("START", color = colors.contentPrimary)
            Spacer(Modifier.height(8.dp))
            MoveHandleButtons(
                containerColor = colors.trimHandleStart,
                outlineColor = colors.moveStartColor,
                contentColor = colors.contentPrimary,
                move = moveStart,
                enableLeft = enabled.startLeft,
                enableRight = enabled.startRight
            )
            Spacer(Modifier.height(2.dp))
            Text(text = toMinSecMs(segment.start), color = colors.contentPrimary)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("1 Segment Added", color = colors.contentPrimary)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(colors.selectionOutline)
                        .clickable { togglePlayback() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colors.background
                    )

                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = toMinSecMs(segment.end - segment.start),
                    color = colors.contentPrimary
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("End", color = colors.contentPrimary)
            Spacer(Modifier.height(8.dp))
            MoveHandleButtons(
                containerColor = colors.trimHandleEnd,
                outlineColor = colors.moveEndColor,
                contentColor = colors.contentPrimary,
                move = moveEnd,
                enableLeft = enabled.endLeft,
                enableRight = enabled.endRight
            )
            Spacer(Modifier.height(2.dp))
            Text(text = toMinSecMs(segment.end), color = colors.contentPrimary)
        }
    }
}

/**
 * Dual-button control for adjusting segment position
 *
 * Displays backward/forward buttons with divider.
 * Supports single tap and long press for continuous adjustment.
 *
 * @param containerColor background color of buttons
 * @param outlineColor border and divider color
 * @param contentColor icon tint color
 * @param move invoked with milliseconds to adjust (positive or negative)
 */
@Composable
private fun MoveHandleButtons(
    containerColor: Color,
    outlineColor: Color,
    contentColor: Color,
    enableLeft: Boolean,
    enableRight: Boolean,
    move: (by: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Row {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp, 0.dp, 0.dp, 8.dp))
                .background(containerColor.withState(enableLeft))
                .changeSegmentPosition(coroutineScope, Unit) { move(-it) }
                .border(
                    2.dp,
                    outlineColor.withState(enableLeft),
                    RoundedCornerShape(8.dp, 0.dp, 0.dp, 8.dp)
                )
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.mdarrowbackwardios),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
        Box(
            modifier = Modifier
                .offset(x = -(2.dp)) // overlap the borders
                .clip(RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp))
                .background(containerColor.withState(enableRight))
                .changeSegmentPosition(coroutineScope, Unit) { move(it) }
                .border(
                    2.dp,
                    outlineColor.withState(enableLeft),
                    RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp)
                )
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.mdarrowforwardios),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
        }
    }
}

private fun Color.withState(enabled: Boolean) = if (enabled) this else this.copy(0.5f)

/**
 * Modifier for segment position adjustment with tap and long press
 *
 * Single tap invokes onChange once with singleTap value.
 * Long press invokes onChange continuously every 10ms with longTap value
 * until pointer is released.
 *
 * @param coroutineScope scope for launching continuous update job
 * @param key key for pointerInput stability
 * @param longTap milliseconds per update during long press
 * @param singleTap milliseconds for single tap
 * @param onChange invoked with adjustment value
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

/**
 * Preview of segment toolbar
 *
 * Demonstrates toolbar with segment from 1000ms to 1500ms.
 *
 * @param colors color scheme for UI elements
 */
@Composable
fun SegmentToolbarPreview(
    colors: SegmentationColors = segmentationColors()
) {
    SegmentToolbar(
        modifier = Modifier.fillMaxWidth(),
        segment = Segment(1000, 1500),
        isPlaying = false,
        togglePlayback = {},
        moveStart = {},
        moveEnd = {},
        colors = colors,
        enabled = EnableSegmentButton.all
    )
}