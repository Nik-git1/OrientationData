package com.example.myapplication
import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import androidx.compose.ui.Modifier
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity

@Composable
fun LineChartActivity(data: List<Float>, lineColor: Color) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val maxValue = data.maxOrNull() ?: 1f // Maximum value for x-axis
    val minValue = data.minOrNull() ?: 0f // Minimum value for x-axis
    var graphWidth by remember { mutableStateOf(0) }
    var graphHeight by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Canvas(
            modifier = Modifier
                .padding(1.dp)
                .fillMaxWidth()
                .height(150.dp)
        ) {
            graphWidth = size.width.toInt()
            graphHeight = size.height.toInt()

            val stepX = graphWidth.toFloat() / (maxValue - minValue)
            val stepY = graphHeight.toFloat() / (180 - (-180))

            val path = Path()
            path.moveTo(0f, graphHeight.toFloat())

            data.forEachIndexed { index, value ->
                val x = (index * stepX).coerceIn(0f, graphWidth.toFloat()) // Limit x within graph bounds
                val y = graphHeight.toFloat() - ((value - (-180f)) * stepY) // Adjust y based on the value and scale
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path, color = lineColor, alpha = 1f, style = Stroke(width = 3.dp.toPx())
            )

            val xLabels = (0..10).map { it.toString() }
            xLabels.forEachIndexed { index, label ->
                val x = (index * (graphWidth / (xLabels.size - 1))).toFloat()
                drawLine(
                    start = Offset(x, graphHeight.toFloat()),
                    end = Offset(x, graphHeight + 8.dp.toPx()),
                    color = Color.Black
                )
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        label,
                        x - 10.dp.toPx(),
                        graphHeight + 20.dp.toPx(),
                        Paint().apply {
                            color = Color.Black.toArgb()
                            textSize = 16.sp.toPx() / density
                        }
                    )
                }
            }

            val yLabels = (minValue.toInt()..maxValue.toInt() step 20).map { it.toString() }
            yLabels.forEachIndexed { index, label ->
                val y = graphHeight.toFloat() - ((index * stepY * 20) + (180 * stepY)) // Adjusted y position
                drawLine(start = Offset(0f, y), end = Offset(-8.dp.toPx(), y), color = Color.Black)
                drawIntoCanvas {
                    it.nativeCanvas.drawText(
                        label,
                        -30.dp.toPx(),
                        y + 10.dp.toPx(),
                        Paint().apply {
                            color = Color.Black.toArgb()
                            textSize = 12.sp.toPx() / density
                        }
                    )
                }
            }

        }
    }
}
