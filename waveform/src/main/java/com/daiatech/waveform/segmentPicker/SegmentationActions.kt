package com.daiatech.waveform.segmentPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.R
import com.daiatech.waveform.models.WaveformColors
import com.daiatech.waveform.segmentation.WindowMarker
import com.daiatech.waveform.segmentation.changeSegmentPosition
import com.daiatech.waveform.toSecsAndMs

@Composable
fun SegmentationActions(
    modifier: Modifier = Modifier,
    colors: WaveformColors,
    start: Long,
    end: Long,
    isPlaying: Boolean,
    progressMs: Long,
    togglePlayback: () -> Unit,
    addToStart: (Int) -> Unit,
    addToEnd: (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier
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
                            addToStart(-it)
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
                            addToStart(it)
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
            Text(
                text = "${toSecsAndMs(start)}s",
                color = colors.buttonColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = togglePlayback,
            ) {
                val res =
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                Icon(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    tint = colors.buttonColor
                )
            }
            /*
            val progress =
                state.activeSegment.value?.let(state.segments::get)?.let { seg ->
                    val d = seg.end - seg.start
                    "${
                        toSecsAndMs(
                            (segmentPlaybackProgress - seg.start).coerceIn(0, d)
                        )
                    }s/${toSecsAndMs(d)}s"
                } ?: ""
             */
            Text(
                text = toSecsAndMs(progressMs),
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
                            addToEnd(-it)
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
                            addToEnd(it)
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
            Text(
                text = "${toSecsAndMs(end)}s",
                color = colors.buttonColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}