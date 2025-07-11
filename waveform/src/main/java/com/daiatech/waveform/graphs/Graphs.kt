package com.daiatech.waveform.graphs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
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

/**
 * A polymorphic composable function that renders a graph based on the specified [GraphType].
 *
 * It supports rendering either a bar graph or a line graph using the given list of amplitudes.
 *
 * @param modifier Modifier to be applied to the graph.
 * @param amplitudes A list of amplitude values representing the data points.
 * @param maxAmplitude The maximum amplitude expected in the graph (used for normalization).
 * @param type The graph type to render, either [GraphType.Bar] or [GraphType.Line].
 */
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
            amplitudes = amplitudes,
            maxAmplitude = maxAmplitude,
            strokeWidth = type.strokeWidth,
            strokeColor = type.strokeColor
        )
    }
}


/**
 * Draws a vertical bar graph using amplitude values.
 *
 * Each amplitude is rendered as a vertical line (bar) centered on the Y-axis.
 * The bar height is scaled based on the [maxAmplitude] and the canvas height.
 *
 * @param modifier Modifier to be applied to the canvas.
 * @param amplitudes List of amplitude values to render.
 * @param maxAmplitude The maximum expected amplitude used to normalize bar height.
 * @param barGap The space between consecutive bars.
 * @param barColor The color of the bars.
 */
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

/**
 * Draws a smooth line graph from the given amplitude values.
 *
 * The line is rendered using a [Path] that connects all data points horizontally across the canvas.
 * Amplitudes are scaled based on the [maxAmplitude] and graph height.
 *
 * @param modifier Modifier to be applied to the canvas.
 * @param amplitudes List of amplitude values to plot.
 * @param maxAmplitude The maximum expected amplitude used to normalize the Y-axis.
 * @param strokeWidth The thickness of the graph line.
 * @param strokeColor The color of the graph line.
 */
@Composable
private fun LineGraph(
    modifier: Modifier = Modifier,
    amplitudes: List<Float>,
    maxAmplitude: Float,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val graphHeight = size.height - 4.dp.toPx()
        val graphWidth = size.width - 4.dp.toPx()

        val xStep = graphWidth / amplitudes.size

        val path = Path()

        amplitudes.normalized(size.height, 0f, maxAmplitude)
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
    Surface {
        LineGraph(
            amplitudes = listOf(200f, 30f, 45f, 5f, 16f, 20f, 25f, 23f),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxAmplitude = 300f,
            strokeColor = Color.Black
        )
    }

}


@Preview
@Composable
fun GraphPrev() {
    BarGraph(
        amplitudes = listOf(200f, 30f, 45f, 5f, 16f, 20f, 25f, 23f),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        maxAmplitude = 300f
    )
}
