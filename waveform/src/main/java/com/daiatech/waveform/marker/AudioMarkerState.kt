package com.daiatech.waveform.marker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.MIN_GRAPH_HEIGHT
import com.daiatech.waveform.MIN_SPIKE_HEIGHT
import com.daiatech.waveform.maxSpikePaddingDp
import com.daiatech.waveform.maxSpikeRadiusDp
import com.daiatech.waveform.maxSpikeWidthDp
import com.daiatech.waveform.minSpikePaddingDp
import com.daiatech.waveform.minSpikeRadiusDp
import com.daiatech.waveform.minSpikeWidthDp
import com.daiatech.waveform.models.AmplitudeType
import com.daiatech.waveform.models.WaveformAlignment
import com.daiatech.waveform.segmentation.Segment
import com.daiatech.waveform.segmentation.end
import com.daiatech.waveform.segmentation.start
import com.daiatech.waveform.toDrawableAmplitudes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioMarkerState(
    val audioFilePath: String,
    val durationMs: Long,
    val amplitudes: List<Int>,
    val graphHeight: Dp,
    val style: DrawStyle,
    val waveformAlignment: WaveformAlignment,
    val amplitudeType: AmplitudeType,
    spikeWidth: Dp,
    spikeRadius: Dp,
    spikePadding: Dp,
    initialMarkers: List<Long>
) {
    var canvasSize: Size = Size.Zero
    val spikeWidth = spikeWidth.coerceIn(minSpikeWidthDp, maxSpikeWidthDp)
    val progressBarWidth = spikeWidth.times(2f)
    val spikeRadius = spikeRadius.coerceIn(minSpikeRadiusDp, maxSpikeRadiusDp)
    val spikeTotalWidth =
        this.spikeWidth + spikePadding.coerceIn(minSpikePaddingDp, maxSpikePaddingDp)

    /***************************  UI States   *****************************/

    private val _markers = mutableStateListOf(*(initialMarkers).toTypedArray())
    val markers: SnapshotStateList<Long> = _markers

    private val _spikeAmplitudes = mutableStateOf(listOf<Float>())
    val spikeAmplitude: State<List<Float>> = _spikeAmplitudes

    private val _zoomedInAmplitudes = mutableStateOf(listOf<Float>())
    val zoomedInAmplitudes: State<List<Float>> = _zoomedInAmplitudes

    private val _activeSegment = mutableStateOf<Segment>(Segment(0L, durationMs / 4))
    val activeSegment: State<Segment> = _activeSegment

    suspend fun computeDrawableAmplitudes(
        spikes: Int,
        spikesMultiplier: Float
    ) = withContext(Dispatchers.Default) {
        val maxHeight = canvasSize.height.coerceAtLeast(MIN_SPIKE_HEIGHT)
        val drawableAmps = amplitudes.toDrawableAmplitudes(
            amplitudeType = amplitudeType,
            spikes = spikes,
            maxHeight = maxHeight,
            minHeight = MIN_SPIKE_HEIGHT,
            multiplier = spikesMultiplier
        )
        _spikeAmplitudes.value = drawableAmps
    }

    suspend fun computeZoomedInDrawableAmplitudes(
        spikes: Int,
        spikesMultiplier: Float
    ) = withContext(Dispatchers.Default) {
        val maxHeight = canvasSize.height.coerceAtLeast(MIN_SPIKE_HEIGHT)
        val zoomedInAmps = activeSegment.value?.let { segment ->
            val viewStartMs = (segment.start).coerceAtLeast(0)
            val viewEndMs = (segment.end).coerceAtMost(durationMs)
            val startIdx =
                (amplitudes.size.toFloat() / durationMs.toFloat() * viewStartMs.toFloat()).toInt()
                    .coerceIn(0, amplitudes.size)
            val endIdx =
                (amplitudes.size.toFloat() / durationMs.toFloat() * viewEndMs.toFloat()).toInt()
                    .coerceIn(0, amplitudes.size)
            amplitudes.subList(startIdx, endIdx).toDrawableAmplitudes(
                amplitudeType = amplitudeType,
                spikes = spikes,
                minHeight = MIN_SPIKE_HEIGHT,
                maxHeight = maxHeight,
                multiplier = spikesMultiplier
            )
        } ?: listOf()
        _zoomedInAmplitudes.value = zoomedInAmps
    }

    private val _undoList = mutableStateListOf<List<Long>>()
    val undoList: SnapshotStateList<List<Long>> = _undoList

    private val _redoList = mutableStateListOf<List<Long>>()
    val redoList: SnapshotStateList<List<Long>> = _redoList

    /***************************  UI States   *****************************/

    /***************************  UI Interactions   *****************************/
    fun addMarker(idx: Int) {

    }

    fun removeMarker(idx: Int) {

    }

    fun onTap(offset: Offset) {

    }

    fun clearAll() {

    }

    fun undo() {

    }

    fun redo() {

    }
    /***************************  UI Interactions   *****************************/


    /***************************  UTILITIES   *****************************/

    /**
     * Returns the position of pixel which lies on the [time]
     *
     * Known values (x,y) = (s,0) and (e,w)
     *
     * required pixel value (y)
     *
     * total width (w) = canvas.width
     *
     * current time (x)
     *
     * start (s) = { [start] - if start!=null ; 0 - otherwise }
     *
     * end (e) = { [end] - if end!=null ; durationMS - otherwise}
     *
     *  y = mx + c
     *
     *  y = (y2 - y1)x/(x2-x1) + c
     *
     *  y = (w-0)x/(e-s) + c ....(1)
     *
     *  Putting (s,0) in (1)
     *
     *  0 = ws/(e-s) + c
     *
     *  c = -ws/(e-s)
     *
     *  Substituting c in (1)
     *
     *  y = wx/(e-s) - ws/(e-s)
     *
     *  y = w(x-s)/(e-s)
     */
    fun durationToPx(time: Long, start: Long? = null, end: Long? = null): Float {
        val s = start ?: 0
        val e = end ?: durationMs
        val w = canvasSize.width
        return w * (time - s) / (e - s)
    }

    /**
     * Returns the corresponding duration which a pixel represents.
     * This function is inverse of [durationToPx].
     *
     * For underlying calculations see [durationToPx]
     * @See durationToPx
     */
    fun pxToDuration(px: Float, start: Long? = null, end: Long? = null): Long {
        val s = start ?: 0L
        val e = end ?: durationMs
        val w = canvasSize.width

        return (px * (e - s) / w).toLong() + s

    }

    /***************************  UTILITIES   *****************************/
}

@Composable
fun rememberAudioMarkerState(
    key: Any? = null,
    audioFilePath: String,
    durationMs: Long,
    amplitudes: List<Int>,
    graphHeight: Dp = MIN_GRAPH_HEIGHT,
    style: DrawStyle = Fill,
    waveformAlignment: WaveformAlignment = WaveformAlignment.Center,
    amplitudeType: AmplitudeType = AmplitudeType.MAX,
    spikeWidth: Dp = 2.dp,
    spikeRadius: Dp = 2.dp,
    spikePadding: Dp = 1.dp,
    initialMarkers: List<Long> = listOf()
): AudioMarkerState {
    return remember(key) {
        AudioMarkerState(
            audioFilePath = audioFilePath,
            durationMs = durationMs,
            amplitudes = amplitudes,
            graphHeight = graphHeight,
            style = style,
            waveformAlignment = waveformAlignment,
            amplitudeType = amplitudeType,
            spikeWidth = spikeWidth,
            spikeRadius = spikeRadius,
            spikePadding = spikePadding,
            initialMarkers = initialMarkers
        )
    }
}