package com.daiatech.waveform

import androidx.compose.ui.geometry.Offset
import com.daiatech.waveform.models.AmplitudeType
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal fun <T> Iterable<T>.fillToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val capacity = ceil(size.safeDiv(count())).roundToInt()
    return map { data -> List(capacity) { data } }.flatten().chunkToSize(size, transform)
}

internal fun <T> Iterable<T>.chunkToSize(size: Int, transform: (List<T>) -> T): List<T> {
    val chunkSize = count() / size
    val remainder = count() % size
    val remainderIndex = ceil(count().safeDiv(remainder)).roundToInt()
    val chunkIteration = filterIndexed { index, _ ->
        remainderIndex == 0 || index % remainderIndex != 0
    }.chunked(chunkSize, transform)
    return when (size) {
        chunkIteration.count() -> chunkIteration
        else -> chunkIteration.chunkToSize(size, transform)
    }
}

internal fun Iterable<Float>.normalize(min: Float, max: Float): List<Float> {
    return map { (max - min) * ((it - min()) safeDiv (max() - min())) + min }
}

internal fun Int.safeDiv(value: Int): Float {
    return if (value == 0) return 0F else this / value.toFloat()
}

internal fun Long.safeDiv(value: Long): Float {
    return if (value == 0L) return 0F else this / value.toFloat()
}

internal infix fun Float.safeDiv(value: Float): Float {
    return if (value == 0f) return 0F else this / value
}

internal fun millisecondsToMmSs(milliseconds: Long): String {
    // Calculate seconds and minutes from milliseconds
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60

    // Format minutes and seconds as strings in "mm:ss" format
    val formattedSeconds = seconds.toString().padStart(2, '0')

    // Combine minutes and seconds in the "mm:ss" format
    return "$minutes:$formattedSeconds"
}

internal fun Offset.distanceFrom(point: Offset): Float {
    val xDiff = this.x - point.x
    val yDiff = this.y - point.y
    return sqrt(xDiff.pow(2) + yDiff.pow(2))
}

internal fun List<Int>.toDrawableAmplitudes(
    amplitudeType: AmplitudeType,
    spikes: Int,
    minHeight: Float,
    maxHeight: Float,
    multiplier: Float = 1f
): List<Float> {
    val amplitudes = map(Int::toFloat)
    if (amplitudes.isEmpty() || spikes == 0) {
        return List(spikes) { minHeight }
    }
    val transform = { data: List<Float> ->
        when (amplitudeType) {
            AmplitudeType.AVG -> data.average()
            AmplitudeType.MAX -> data.max()
            AmplitudeType.MIN -> data.min()
        }.toFloat()
    }
    return when {
        spikes > amplitudes.count() -> amplitudes.fillToSize(spikes, transform)
        else -> amplitudes.chunkToSize(spikes, transform)
    }.normalize(minHeight, maxHeight)
        .map { it.times(multiplier).coerceIn(minHeight, maxHeight) }
}



internal fun List<Float>.normalized(max: Float, min: Float, lMax: Float): List<Float> {
    val lMin = this.min()

    // If the list min == max, then return as it is
    if (lMax == lMin) return this

    /*
     * y = mx + c
     * m = (y2-y1) / (x2 - x1)
     */
    val slope = (max - min) / (lMax - lMin)
    val yIntercept = max - (slope * lMax)

    val y: (Float) -> Float = { x ->
        slope * x + yIntercept + 1f
    }

    return map(y)
}
