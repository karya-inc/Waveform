package com.daiatech.waveform.app.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.waveform.AUDIO_PLAYER_REFRESH_RATE_MS
import com.daiatech.waveform.app.model.AudioMeta
import com.daiatech.waveform.app.utils.LocalAudioManager
import com.daiatech.waveform.segmentPicker.AudioSegmentPicker
import com.daiatech.waveform.segmentPicker.rememberAudioSegmentPickerState

@Composable
fun AudioSegmentPickerScreen(audioFilePath: String) {
    val activity = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(activity).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var progressMs by remember { mutableLongStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                progressMs = exoPlayer.currentPosition
                handler.postDelayed(this, AUDIO_PLAYER_REFRESH_RATE_MS)
            }
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_ENDED, ExoPlayer.STATE_IDLE -> {
                        handler.removeCallbacks(runnable)
                        progressMs = 0
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

        // Cleanup when component is destroyed
        onDispose {
            exoPlayer.removeListener(listener)
            handler.removeCallbacks(runnable)
            exoPlayer.release()
        }
    }

    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    val audioManager = LocalAudioManager.current
    LaunchedEffect(Unit) {
        val amplitudes = audioManager.getAmplitudes(path = audioFilePath)
        val duration = audioManager.getDuration(path = audioFilePath)
        meta = AudioMeta(amplitudes, duration)
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        meta?.run {
            val pickerState = rememberAudioSegmentPickerState(
                audioFilePath = audioFilePath,
                amplitudes = amplitudes,
                durationMs = duration,
                segment = Pair(0, duration / 4),
                window = Pair(0, duration / 8)
            )
            AudioSegmentPicker(
                state = pickerState,
                mainPlayerProgress = pickerState.activeSegment.first + progressMs,
                segmentPlaybackProgress = pickerState.activeSegment.first + progressMs,
                toggleSegmentPlayback = {
                    val segment = pickerState.activeSegment
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.playWhenReady = true
                        val mediaItem = MediaItem.Builder()
                            .setUri(audioFilePath)
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .apply {
                                        setStartPositionMs(segment.first)
                                        setEndPositionMs(segment.second)
                                    }
                                    .build()
                            )
                            .build()
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                    }
                },
                isPlaying = isPlaying
            )
        }
    }
}
