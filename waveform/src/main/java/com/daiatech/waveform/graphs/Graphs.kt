package com.daiatech.waveform.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daiatech.waveform.normalized

@Composable
fun Graph(
    modifier: Modifier = Modifier,
    amplitudes: List<Float>,
    maxAmplitude: Float,
    type: GraphType
) {
    when (type) {
        is GraphType.Bar -> BarGraph(
            modifier = modifier,
            amplitudes = amplitudes,
            maxAmplitude = maxAmplitude,
            barGap = type.barGap,
            barColor = type.barColor
        )

        is GraphType.Line -> LineGraph(
            modifier = modifier,
            amplitudeValues = amplitudes,
            maxAmplitude = maxAmplitude,
            strokeWidth = type.strokeWidth,
            strokeColor = type.strokeColor
        )
    }
}

/** Amplitudes should be normalized between 0 and 1 */
@Composable
private fun BarGraph(
    modifier: Modifier = Modifier,
    amplitudes: List<Float>,
    maxAmplitude: Float = 18000f,
    barGap: Dp = 4.dp,
    barColor: Color = Color.Red,
) {
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val xStep = size.width / amplitudes.size
        val w = (size.width - with(density) { barGap.toPx() }
            .times(amplitudes.size)) / amplitudes.size

        amplitudes.normalized(size.height, 0f, maxAmplitude).forEachIndexed { idx, y ->
            val x = (idx + 0.5f) * xStep
            drawLine(
                start = Offset(x, (size.height / 2) - (y / 3)),
                end = Offset(x, ((size.height / 2)) + y / 3),
                brush = SolidColor(barColor),
                strokeWidth = w,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Amplitudes should be normalized between 0 and 1 */
@Composable
private fun LineGraph(
    modifier: Modifier = Modifier,
    amplitudeValues: List<Float>,
    maxAmplitude: Float,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = Color.White
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
    ) {
        val graphHeight = size.height - 4.dp.toPx()
        val graphWidth = size.width - 4.dp.toPx()

        val xStep = graphWidth / amplitudeValues.size

        val path = Path()

        amplitudeValues.normalized(size.height, 0f, maxAmplitude)
            .forEachIndexed { idx, yCoordinate ->
                val x = 10.dp.toPx() + idx * xStep
                val y = graphHeight - yCoordinate
                if (idx == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth.toPx())
        )
    }
}

@Preview
@Composable
private fun LineGraphPrev() {
    LineGraph(
        amplitudeValues = listOf(200f, 30f, 45f, 5f, 16f),
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp),
        maxAmplitude = 1800f
    )
}


@Preview
@Composable
fun GraphPrev() {
    BarGraph(
        amplitudes = listOf(200f, 30f, 45f, 5f, 16f),
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp),
        maxAmplitude = 1800f
    )
}
