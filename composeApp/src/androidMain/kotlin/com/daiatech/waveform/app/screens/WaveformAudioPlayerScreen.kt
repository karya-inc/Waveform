package com.daiatech.waveform.app.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import com.daiatech.waveform.player.WaveformAudioPlayerUi
import com.daiatech.waveform.player.rememberAudioPlayerState
import com.daiatech.waveform.segmentation.speed.PlaybackSpeed

@Composable
fun WaveformAudioPlayerScreen(audioFilePath: String) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var progressMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableStateOf(PlaybackSpeed.X1_00) }

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
                        progressMs = 0L
                    }
                    else -> {}
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
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
        verticalArrangement = Arrangement.Bottom,
    ) {
        meta?.let { (amplitudes, duration) ->
            val playerState = rememberAudioPlayerState(
                amplitudes = amplitudes,
                durationMs = duration,
            )

            WaveformAudioPlayerUi(
                state = playerState,
                progressMs = progressMs,
                isPlaying = isPlaying,
                togglePlayback = {
                    if (isPlaying) {
                        exoPlayer.pause()
                        isPaused = true
                    } else {
                        if (!isPaused) {
                            val mediaItem = MediaItem.fromUri(audioFilePath)
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                        }
                        exoPlayer.play()
                    }
                },
                seek = { deltaMs ->
                    val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, duration)
                    exoPlayer.seekTo(target)
                    progressMs = target
                },
                speed = speed,
                updateSpeed = { newSpeed ->
                    Log.d("WaveformAudioPlayer", "WaveformAudioPlayerScreen: Updated speed, $newSpeed")
                    speed = newSpeed
                    exoPlayer.setPlaybackSpeed(newSpeed.float)
                },
            )
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
