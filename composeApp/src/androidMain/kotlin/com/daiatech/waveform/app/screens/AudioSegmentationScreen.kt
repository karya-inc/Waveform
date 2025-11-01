package com.daiatech.waveform.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.daiatech.waveform.app.model.AudioMeta
import com.daiatech.waveform.app.utils.LocalAudioManager
import com.daiatech.waveform.segmentation.AudioSegmentationUi
import com.daiatech.waveform.segmentation.rememberAudioSegmentationState

@Composable
fun AudioSegmentationScreen(audioFilePath: String) {
    var meta by remember { mutableStateOf<AudioMeta?>(null) }
    val audioManager = LocalAudioManager.current
    LaunchedEffect(Unit) {
        val amplitudes = audioManager.getAmplitudes(path = audioFilePath)
        val duration = audioManager.getDuration(path = audioFilePath)
        meta = AudioMeta(amplitudes, duration)
    }

    meta?.run {
        val segmentationState = rememberAudioSegmentationState(
            audioFilePath = audioFilePath,
            amplitudes = amplitudes,
            durationMs = duration,
            enableAdjustment = true
        )
        AudioSegmentationUi(state = segmentationState)
    }
}