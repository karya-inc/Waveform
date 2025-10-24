package com.daiatech.waveform.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import com.daiatech.waveform.app.model.AudioMeta
import com.daiatech.waveform.app.utils.LocalAudioManager
import com.daiatech.waveform.segmentation.AudioSegmentPicker
import com.daiatech.waveform.segmentation.rememberSegmentPickerState

@Composable
fun AudioSegmentationScreen2(audioFilePath: String) {
    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    val audioManager = LocalAudioManager.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val amplitudes = audioManager.getAmplitudes(path = audioFilePath)
        val duration = audioManager.getDuration(path = audioFilePath)
        meta = AudioMeta(amplitudes, duration)
    }

    meta?.run {
        val exoPlayer = remember { ExoPlayer.Builder(context).build() }
        val segmentPickerState = rememberSegmentPickerState(
            amplitudes = amplitudes,
            duration,
            minimumSegmentMs = 1000
        )
    }
}