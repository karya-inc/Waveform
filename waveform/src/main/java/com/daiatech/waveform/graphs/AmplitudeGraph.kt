package com.daiatech.waveform.graphs

import android.view.MotionEvent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.MAX_PROGRESS
import com.daiatech.waveform.MIN_PROGRESS
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.minSpikePaddingDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.toDrawableAmplitudes

/**
 * A dynamic, real-time graph composable that visualizes audio amplitudes over time.
 *
 * This composable listens to the current amplitude input ([currentAmp]) and continuously updates
 * a list of amplitude points to simulate waveform or signal movement. It supports both paused and
 * active states, as well as automatic amplitude scaling.
 *
 * Internally, it renders either a [BarGraph] or [LineGraph] based on the provided [GraphType].
 *
 * @param modifier Modifier to be applied to the graph layout.
 * @param currentAmp The current amplitude value to visualize.
 * @param isPaused Whether the graph should pause updating amplitudes. If true, the graph freezes.
 * @param noOfPoints The number of amplitude points to keep and display at once.
 * @param maxAmplitude The initial max amplitude used for scaling; updates dynamically if larger values are seen.
 * @param type The graph rendering style: bar or line (see [GraphType]).
 */
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

/**
 * Displays a visual representation of audio amplitude as a waveform using vertical bars ("spikes").
 * Supports touch interaction to indicate and update progress on the waveform.
 *
 * @param modifier Modifier applied to the Canvas layout.
 * @param style The [DrawStyle] used for drawing spikes (e.g., [Fill]).
 * @param waveformBrush Brush used to draw the waveform spikes.
 * @param progressBrush Brush used to draw the progress indicator.
 * @param waveformAlignment Vertical alignment of spikes in the canvas.
 * @param amplitudeType Type of amplitude processing (e.g., average, max) defined in [AmplitudeType].
 * @param spikeAnimationSpec Animation used for animating the amplitude spikes.
 * @param spikeWidth Width of each spike (bar) in the waveform.
 * @param spikeRadius Corner radius applied to each spike for rounded ends.
 * @param spikePadding Space between adjacent spikes.
 * @param progress Current progress of the waveform (value between 0 and 1).
 * @param amplitudes List of raw amplitude values to render as waveform spikes.
 * @param onProgressChange Lambda that provides updated progress when user interacts with the waveform.
 * @param onProgressChangeFinished Optional lambda triggered when user interaction ends.
 *
 * ### Behavior:
 * - Spikes are animated using [animateFloatAsState] to create smooth transitions.
 * - Tapping or dragging across the waveform triggers [onProgressChange] with a value between 0 and 1.
 * - When user lifts their finger, [onProgressChangeFinished] is called if defined.
 *
 * ### Visual Customization:
 * - Supports full customization for spike appearance (width, spacing, radius, alignment).
 * - The progress indicator is a thin vertical bar drawn using [progressBrush].
 *
 * ### Usage Example:
 * ```
 * AmplitudeBarGraph(
 *     amplitudes = audioData,
 *     progress = playbackProgress,
 *     onProgressChange = { newProgress -> viewModel.updateProgress(newProgress) },
 *     onProgressChangeFinished = { viewModel.onSeekFinished() }
 * )
 * ```
 */
@Suppress("LocalVariableName")
@Composable
fun AmplitudeBarGraph(
    modifier: Modifier = Modifier,
    style: DrawStyle = Fill,
    waveformBrush: Brush = SolidColor(Color.White),
    progressBrush: Brush = SolidColor(Color.Blue),
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.AVG,
    spikeAnimationSpec: AnimationSpec<Float> = tween(500),
    spikeWidth: Dp = 4.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    progress: Float = 0F,
    amplitudes: List<Int>,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: (() -> Unit)? = null
) {
    val _progress = remember(progress) { progress.coerceIn(MIN_PROGRESS, MAX_PROGRESS) }
    val _spikeWidth = remember(spikeWidth) { spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp) }
    val _spikePadding = remember(spikePadding) { spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp) }
    val _spikeRadius = remember(spikeRadius) { spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp) }
    val _spikeTotalWidth = remember(spikeWidth, spikePadding) { _spikeWidth + _spikePadding }
    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
    var spikes by remember { mutableFloatStateOf(0F) }
    val spikesAmplitudes = remember(amplitudes, spikes, amplitudeType) {
        amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes.toInt(),
            minHeight = MIN_SPIKE_HEIGHT,
            maxHeight = canvasSize.height.coerceAtLeast(MIN_SPIKE_HEIGHT)
        )
    }.map { animateFloatAsState(it, spikeAnimationSpec, label = "spike amplitudes").value }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(48.dp)
            .pointerInteropFilter {
                return@pointerInteropFilter when (it.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> {
                        if (it.x in 0F..canvasSize.width) {
                            onProgressChange(it.x / canvasSize.width)
                            true
                        } else {
                            false
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        onProgressChangeFinished?.invoke()
                        true
                    }

                    else -> false
                }
            }
            .then(modifier)
    ) {
        canvasSize = size
        spikes = size.width / _spikeTotalWidth.toPx()
        spikesAmplitudes.forEachIndexed { index, amplitude ->
            drawRoundRect(
                brush = waveformBrush,
                topLeft = Offset(
                    x = index * _spikeTotalWidth.toPx(),
                    y = when (waveformAlignment) {
                        WaveformAlignment.Top -> 0F
                        WaveformAlignment.Bottom -> size.height - amplitude
                        WaveformAlignment.Center -> size.height / 2F - amplitude / 2F
                    }
                ),
                size = Size(
                    width = _spikeWidth.toPx(),
                    height = amplitude
                ),
                cornerRadius = CornerRadius(_spikeRadius.toPx(), _spikeRadius.toPx()),
                style = style
            )

            if (_progress != 0F) {
                drawRoundRect(
                    brush = progressBrush,
                    size = Size(
                        width = 2.dp.toPx(),
                        height = size.height
                    ),
                    topLeft = Offset(
                        x = _progress * size.width,
                        y = 0f
                    )
                )
            }
        }
    }
}


@Preview
@Composable
private fun AmplitudeBarGraphPrev() {
    AmplitudeBarGraph(
        amplitudes = listOf(100, 200, 300, 500, 100, 20),
        onProgressChange = {}
    )
}