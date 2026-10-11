package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VitalOxygen

@Composable
fun LiveEcgWaveform(
    wavePoints: List<Float>,
    heartRate: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF07121E))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw medical grid lines
            val gridStepX = 30.dp.toPx()
            val gridStepY = 24.dp.toPx()
            val gridColor = Color(0x1A00E5FF)

            var x = 0f
            while (x <= width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridStepX
            }

            var y = 0f
            while (y <= height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridStepY
            }

            // 2. Draw rolling ECG waveform
            if (wavePoints.size > 1) {
                val path = Path()
                val stepX = width / (wavePoints.size - 1).toFloat()

                wavePoints.forEachIndexed { index, normalizedY ->
                    val pointX = index * stepX
                    // Invert normalized 0..1 to canvas coordinates (1.0 = top, 0.0 = bottom)
                    val pointY = height * (1f - normalizedY)
                    if (index == 0) {
                        path.moveTo(pointX, pointY)
                    } else {
                        path.lineTo(pointX, pointY)
                    }
                }

                // Glow shadow
                drawPath(
                    path = path,
                    color = VitalOxygen.copy(alpha = 0.35f),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Crisp foreground ECG trace
                drawPath(
                    path = path,
                    color = Color(0xFF00FFC2),
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Current head lead pulse dot
                val lastX = (wavePoints.size - 1) * stepX
                val lastY = height * (1f - wavePoints.last())
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(lastX, lastY)
                )
            }
        }

        // Overlay status header
        Text(
            text = "LEAD II • CONTINUOUS ECG • ${heartRate} BPM",
            color = Color(0xFF00FFC2),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopStart)
        )

        Text(
            text = "LIVE TELEMETRY STREAM",
            color = Color(0x99FFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
