package com.daiatech.waveform.segmentation.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import com.daiatech.waveform.models.WaveformAlignment
import kotlinx.coroutines.launch

@Composable
fun AudioSegmentationUi(
    modifier: Modifier = Modifier,
    state: AudioSegmentationUiState,
    color: Colors = Colors()
) {
    val coroutineScope = rememberCoroutineScope()
    Surface(color = color.containerColor) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.ZoomOut,
                        contentDescription = null,
                        tint = color.primaryContentColor
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(state.spikeMaxHeight)
                        .onGloballyPositioned {
                            coroutineScope.launch {
                                state.computeDrawableAmplitudes(it.size)
                            }
                        }
                        .drawBehind {
                            state.spikeAmplitude.value.forEachIndexed { index, amplitude ->
                                drawRoundRect(
                                    brush = SolidColor(Color.White),
                                    topLeft = Offset(
                                        x = index * state.spikeTotalWidth.toPx(),
                                        y = when (state.alignment) {
                                            WaveformAlignment.Top -> 0F
                                            WaveformAlignment.Bottom -> size.height - amplitude
                                            WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
                                        }
                                    ),
                                    size = Size(
                                        width = state.spikeWidth.toPx(),
                                        height = amplitude
                                    ),
                                    cornerRadius = CornerRadius(
                                        state.spikeRadius.toPx(),
                                        state.spikeRadius.toPx()
                                    ),
                                    style = Fill
                                )
                            }
                        }
                )

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = null,
                        tint = color.primaryContentColor
                    )
                }
            }

            Row {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = color.primaryContentColor
                    )
                }
                if (state.isPlaying) {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = color.primaryContentColor
                        )
                    }
                } else {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = color.primaryContentColor
                        )
                    }
                }

                IconButton(onClick = {

                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = color.primaryContentColor
                    )
                }
            }

            when (state.marker.value) {
                Marker.START -> {
                    TextButton(
                        onClick = {},
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = color.actionsColor
                        )
                    ) {
                        Text("+ Add start marker")
                    }
                }

                Marker.END -> {
                    TextButton(
                        onClick = {},
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = color.actionsColor
                        )
                    ) {
                        Text("+ Add end marker")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AudioSegmentationUiPrev() {
    Surface {
        val density = LocalDensity.current
        AudioSegmentationUi(
            modifier = Modifier.fillMaxWidth(),
            state = AudioSegmentationUiState(
                amplitudes = listOf(100, 10, 2, 111, 12, 222, 345, 11, 58, 36, 32, 12, 98, 47),
                density = density
            )
        )
    }
}