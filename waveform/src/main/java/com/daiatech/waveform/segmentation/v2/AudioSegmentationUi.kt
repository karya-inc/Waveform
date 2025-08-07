package com.daiatech.waveform.segmentation.v2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun AudioSegmentationUi(
    modifier: Modifier = Modifier,
    state: AudioSegmentationUiState,
    properties: SegmentationUiProperties
) {
    Surface(color = properties.color.containerColor) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = properties.color.primaryContentColor
                    )
                }

                Canvas(Modifier.weight(1f)) {

                }

                IconButton(onClick = {}) {
                    androidx.compose.material.icons.Icons
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = properties.color.primaryContentColor
                    )
                }
            }

            Row {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = properties.color.primaryContentColor
                    )
                }
                if (state.isPlaying) {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = properties.color.primaryContentColor
                        )
                    }
                } else {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = properties.color.primaryContentColor
                        )
                    }
                }

                IconButton(onClick = {

                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = properties.color.primaryContentColor
                    )
                }
            }

            when (state.marker.value) {
                Marker.START -> {
                    TextButton(
                        onClick = {},
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = properties.color.actionsColor
                        )
                    ) {
                        Text("+ Add start marker")
                    }
                }

                Marker.END -> {
                    TextButton(
                        onClick = {},
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = properties.color.actionsColor
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
        AudioSegmentationUi(
            modifier = Modifier.fillMaxWidth(),
            state = AudioSegmentationUiState(
                amplitudes = listOf()
            ),
            properties = SegmentationUiProperties()
        )
    }
}