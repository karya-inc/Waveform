package com.daiatech.waveform.graphs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun AmplitudeGraph(
    modifier: Modifier = Modifier,
    currentAmp: Float,
    isPaused: Boolean,
    noOfPoints: Int,
    maxAmplitude: Float,
    type: GraphType
) {
    var _maxAmplitude by remember { mutableFloatStateOf(maxAmplitude) }
    LaunchedEffect(key1 = currentAmp) {
        if (currentAmp > _maxAmplitude) {
            _maxAmplitude = currentAmp
        }
    }

    var amplitudes by remember { mutableStateOf(List(noOfPoints) { 1f }) }
    val animationSpec = remember { tween<Float>(durationMillis = 200) }
    val animateFloat by animateFloatAsState(
        targetValue = currentAmp,
        animationSpec = animationSpec,
        label = "amplitudes"
    )
    LaunchedEffect(isPaused, animateFloat) {
        if (!isPaused) {
            amplitudes = amplitudes.takeLast(noOfPoints - 1) + animateFloat
        }
    }
    Graph(
        modifier = modifier,
        amplitudes = amplitudes,
        maxAmplitude = _maxAmplitude,
        type = type
    )
}