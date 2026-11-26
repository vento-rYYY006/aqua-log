package com.atomu.aqualog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

/** points は日付（epochDay）の昇順 */
@Composable
fun LineChart(points: List<Pair<Long, Double>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()

    if (points.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("この項目の記録はまだありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Canvas(modifier) {
        val left = 52.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 22.dp.toPx()

        val minV = points.minOf { it.second }
        val maxV = points.maxOf { it.second }
        val span = if (maxV - minV > 0) maxV - minV else maxOf(abs(maxV) * 0.01, 0.001)
        val lo = minV - span * 0.15
        val hi = maxV + span * 0.15
        val minD = points.first().first
        val maxD = points.last().first
        val dSpan = maxOf(maxD - minD, 1L).toFloat()

        fun x(d: Long): Float =
            if (minD == maxD) (left + right) / 2f else left + (right - left) * ((d - minD).toFloat() / dSpan)
        fun y(v: Double): Float = (bottom - (bottom - top) * ((v - lo) / (hi - lo))).toFloat()

        val decimals = when {
            hi - lo < 0.05 -> 4
            hi - lo < 1 -> 2
            hi - lo < 20 -> 1
            else -> 0
        }
        for (i in 0..3) {
            val v = lo + (hi - lo) * i / 3.0
            val yy = y(v)
            drawLine(gridColor, Offset(left, yy), Offset(right, yy), strokeWidth = 1f)
            val txt = String.format(Locale.US, "%.${decimals}f", v)
            val r = measurer.measure(txt, labelStyle)
            drawText(r, topLeft = Offset(left - r.size.width - 6.dp.toPx(), yy - r.size.height / 2f))
        }

        val path = Path()
        points.forEachIndexed { i, (d, v) ->
            if (i == 0) path.moveTo(x(d), y(v)) else path.lineTo(x(d), y(v))
        }
        drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { (d, v) -> drawCircle(lineColor, radius = 3.5.dp.toPx(), center = Offset(x(d), y(v))) }

        val first = measurer.measure(minD.fmtShortY(), labelStyle)
        drawText(first, topLeft = Offset(left, bottom + 4.dp.toPx()))
        if (maxD != minD) {
            val last = measurer.measure(maxD.fmtShortY(), labelStyle)
            drawText(last, topLeft = Offset(right - last.size.width, bottom + 4.dp.toPx()))
        }
    }
}
