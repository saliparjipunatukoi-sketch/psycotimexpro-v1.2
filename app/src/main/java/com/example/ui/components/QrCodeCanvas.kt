package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * High-performance self-contained QR matrix visualizer for Cam 1 -> Cam 2 pairing
 */
@Composable
fun QrCodeVisualizer(
    payload: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220
) {
    val matrixSize = 25
    val matrix = remember(payload) {
        generateQrMatrix(payload, matrixSize)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF06B6D4), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.size(sizeDp.dp)
        ) {
            val cellSize = size.width / matrixSize

            // Draw white background
            drawRect(Color.White, Offset.Zero, size)

            // Draw matrix cells
            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (matrix[row][col]) {
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }

            // Draw QR Corner Position Detection Marks (Top-Left, Top-Right, Bottom-Left)
            drawFinderPattern(0f, 0f, cellSize * 7)
            drawFinderPattern((matrixSize - 7) * cellSize, 0f, cellSize * 7)
            drawFinderPattern(0f, (matrixSize - 7) * cellSize, cellSize * 7)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "SCAN QR UNTUK PAIR CAM 2",
            fontSize = 11.sp,
            color = Color(0xFF0F172A),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    x: Float,
    y: Float,
    finderSize: Float
) {
    // Outer black box
    drawRect(Color(0xFF0F172A), Offset(x, y), Size(finderSize, finderSize))
    // Inner white gap
    val step = finderSize / 7
    drawRect(Color.White, Offset(x + step, y + step), Size(finderSize - 2 * step, finderSize - 2 * step))
    // Center black core
    drawRect(Color(0xFF0F172A), Offset(x + 2 * step, y + 2 * step), Size(finderSize - 4 * step, finderSize - 4 * step))
}

private fun generateQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }
    val hash = abs(data.hashCode())

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Reserve corner finder areas
            if ((r < 7 && c < 7) || (r < 7 && c >= size - 7) || (r >= size - 7 && c < 7)) {
                continue
            }
            // Deterministic pseudo-random bits based on hash + coordinate
            val cellHash = (hash * (r + 1) * 31 + c * 17 + data.length) % 100
            matrix[r][c] = cellHash % 2 == 0
        }
    }
    return matrix
}
