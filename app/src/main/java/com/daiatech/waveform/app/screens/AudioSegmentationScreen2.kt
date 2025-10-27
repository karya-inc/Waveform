package com.daiatech.waveform.app.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.karya.ui.buttons.ButtonVariation
import com.daiatech.karya.ui.buttons.KButton
import com.daiatech.karya.ui.buttons.KIconButton
import com.daiatech.waveform.AUDIO_PLAYER_REFRESH_RATE_MS
import com.daiatech.waveform.app.model.AudioMeta
import com.daiatech.waveform.app.utils.LocalAudioManager
import com.daiatech.waveform.segmentation.AudioSegmentPicker
import com.daiatech.waveform.segmentation.SegmentationColors
import com.daiatech.waveform.segmentation.rememberSegmentPickerState
import com.daiatech.waveform.segmentation.segmentationColors
import kotlinx.coroutines.Job

@Composable
fun AudioSegmentationScreen2(
    audioFilePath: String,
    colors: SegmentationColors = segmentationColors()
) {
    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    val audioManager = LocalAudioManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val amplitudes = audioManager.getAmplitudes(path = audioFilePath)
        val duration = audioManager.getDuration(path = audioFilePath)
        meta = AudioMeta(amplitudes, duration)
    }

    meta?.run {
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(audioFilePath)
                setMediaItem(mediaItem)
                prepare()
            }
        }

        var isPlaying by remember { mutableStateOf(false) }
        var isSegmentPlaying by remember { mutableStateOf(false) }
        var progressMs by remember { mutableLongStateOf(0) }
        var speed by remember { mutableFloatStateOf(1f) }
        val segmentPickerState = rememberSegmentPickerState(
            amplitudes = amplitudes,
            durationMs = duration,
            minimumSegmentMs = 1000
        )
        val segment = segmentPickerState.segment

        DisposableEffect(Unit) {
            val handler = Handler(Looper.getMainLooper())

            val runnable = object : Runnable {
                override fun run() {
                    progressMs = exoPlayer.currentPosition
                    if (isSegmentPlaying) {
                        segment.value?.let { segment ->
                            if (progressMs >= segment.end) {
                                isSegmentPlaying = false
                                exoPlayer.pause()
                                exoPlayer.seekTo(segment.start)
                            }
                        }
                    }
                    handler.postDelayed(this, AUDIO_PLAYER_REFRESH_RATE_MS)
                }
            }

            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        ExoPlayer.STATE_ENDED -> {
                            handler.removeCallbacks(runnable)
                            if (!isSegmentPlaying) {
                                // Full playback ended
                                exoPlayer.seekTo(0)
                                progressMs = 0
                                isPlaying = false
                            }
                        }

                        ExoPlayer.STATE_IDLE -> {
                            handler.removeCallbacks(runnable)
                        }

                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    super.onIsPlayingChanged(playing)
                    if (playing) {
                        handler.postDelayed(runnable, AUDIO_PLAYER_REFRESH_RATE_MS)
                    } else {
                        isSegmentPlaying = false
                        isPlaying = false
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

        // Update ExoPlayer speed when speed changes
        LaunchedEffect(speed) {
            exoPlayer.setPlaybackSpeed(speed)
        }

        Column {
            AudioSegmentPicker(
                state = segmentPickerState,
                progressMs = progressMs,
                isPlaying = isPlaying,
                togglePlayback = {
                    // handle playback only if segment is not playing
                    if (!isSegmentPlaying) {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    }
                },
                isSegmentPlaying = isSegmentPlaying,
                toggleSegmentPlayback = {
                    // play segment only if main player is not playing
                    if (!isPlaying) {
                        if (isSegmentPlaying) {
                            exoPlayer.pause()
                        } else {
                            segment.value?.let { (start, end) ->
                                exoPlayer.seekTo(start)
                                exoPlayer.play()
                                isSegmentPlaying = true
                            }
                        }
                    }
                },
                seek = { deltaMs ->
                    if (exoPlayer.isPlaying) exoPlayer.stop()
                    val newPosition = (progressMs + deltaMs).coerceIn(0, duration)
                    exoPlayer.seekTo(newPosition)
                    progressMs = newPosition
                },
                speed = speed,
                updateSpeed = { newSpeed ->
                    speed = newSpeed
                }
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(8.dp)
            ) {
                AnimatedVisibility(visible = segmentPickerState.segment.value == null) {
                    KButton(
                        content = "Add Segment",
                        buttonVariation = ButtonVariation.PrimaryButtonRegular,
                        onClick = { segmentPickerState.addSegment(progressMs) }
                    )
                }

                AnimatedVisibility(visible = segmentPickerState.segment.value != null) {
                    Row(Modifier.fillMaxWidth()) {
                        KIconButton(
                            onClick = { segmentPickerState.removeSegment() },
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
