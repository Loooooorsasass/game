package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.engine.EAST
import com.example.core.engine.Maze
import com.example.core.engine.NORTH
import com.example.core.engine.Point
import com.example.core.engine.SOUTH
import com.example.core.engine.WEST
import com.example.data.shop.MazeTheme
import com.example.data.shop.PlayerSkin
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun MazeCanvas(
    maze: Maze,
    player: Point,
    visitedCells: Set<Int>,
    theme: MazeTheme,
    skin: PlayerSkin,
    vision: Int?,
    hintPath: List<Point>? = null,
    replayTrail: List<Point>? = null,
    onMove: (dx: Int, dy: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth animated player position
    val animPlayerX by animateFloatAsState(
        targetValue = player.x.toFloat(),
        animationSpec = tween(durationMillis = 80),
        label = "animPlayerX"
    )
    val animPlayerY by animateFloatAsState(
        targetValue = player.y.toFloat(),
        animationSpec = tween(durationMillis = 80),
        label = "animPlayerY"
    )

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 36f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(theme.panelColor)
            .pointerInput(maze, player) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (abs(totalDragX) > dragThreshold || abs(totalDragY) > dragThreshold) {
                            if (abs(totalDragX) > abs(totalDragY)) {
                                if (totalDragX > 0) onMove(1, 0) else onMove(-1, 0)
                            } else {
                                // In coordinate system: dy = 1 is NORTH (up on screen), dy = -1 is SOUTH (down on screen)
                                if (totalDragY < 0) onMove(0, 1) else onMove(0, -1)
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val viewW = if (vision != null) min(vision, maze.w) else maze.w
            val viewH = if (vision != null) min(vision, maze.h) else maze.h

            val originX = if (vision != null) {
                min(max(0, player.x - viewW / 2), maze.w - viewW)
            } else 0

            val originY = if (vision != null) {
                min(max(0, player.y - viewH / 2), maze.h - viewH)
            } else 0

            val cellSize = min(canvasW / viewW, canvasH / viewH)
            val offX = (canvasW - viewW * cellSize) / 2f
            val offY = (canvasH - viewH * cellSize) / 2f

            // 1. Draw visited cells
            for (ly in 0 until viewH) {
                for (lx in 0 until viewW) {
                    val gx = originX + lx
                    val gy = originY + ly
                    val cellIndex = gy * maze.w + gx
                    if (visitedCells.contains(cellIndex)) {
                        val sx = offX + lx * cellSize
                        val sy = offY + (viewH - 1 - ly) * cellSize
                        drawRect(
                            color = theme.pathVisitedColor,
                            topLeft = Offset(sx + 1f, sy + 1f),
                            size = Size(cellSize - 2f, cellSize - 2f)
                        )
                    }
                }
            }

            // 2. Draw hint / replay path trail if available
            hintPath?.forEach { p ->
                if (p.x in originX until originX + viewW && p.y in originY until originY + viewH) {
                    val lx = p.x - originX
                    val ly = p.y - originY
                    val sx = offX + lx * cellSize + cellSize / 2f
                    val sy = offY + (viewH - 1 - ly) * cellSize + cellSize / 2f
                    drawCircle(
                        color = Color(0x66F59E0B),
                        radius = cellSize * 0.22f,
                        center = Offset(sx, sy)
                    )
                }
            }

            replayTrail?.forEachIndexed { idx, p ->
                if (p.x in originX until originX + viewW && p.y in originY until originY + viewH) {
                    val lx = p.x - originX
                    val ly = p.y - originY
                    val sx = offX + lx * cellSize + cellSize / 2f
                    val sy = offY + (viewH - 1 - ly) * cellSize + cellSize / 2f
                    drawCircle(
                        color = theme.accentColor.copy(alpha = 0.5f),
                        radius = cellSize * 0.18f,
                        center = Offset(sx, sy)
                    )
                }
            }

            // 3. Draw maze walls
            val strokeWidth = max(1.5f, cellSize * 0.08f)
            for (ly in 0 until viewH) {
                for (lx in 0 until viewW) {
                    val gx = originX + lx
                    val gy = originY + ly
                    val mask = maze.cellAt(gx, gy)
                    val sx = offX + lx * cellSize
                    val sy = offY + (viewH - 1 - ly) * cellSize

                    // NORTH wall
                    if ((mask and NORTH) == 0) {
                        drawLine(
                            color = theme.wallColor,
                            start = Offset(sx, sy),
                            end = Offset(sx + cellSize, sy),
                            strokeWidth = strokeWidth
                        )
                    }
                    // SOUTH wall
                    if ((mask and SOUTH) == 0) {
                        drawLine(
                            color = theme.wallColor,
                            start = Offset(sx, sy + cellSize),
                            end = Offset(sx + cellSize, sy + cellSize),
                            strokeWidth = strokeWidth
                        )
                    }
                    // WEST wall
                    if ((mask and WEST) == 0) {
                        drawLine(
                            color = theme.wallColor,
                            start = Offset(sx, sy),
                            end = Offset(sx, sy + cellSize),
                            strokeWidth = strokeWidth
                        )
                    }
                    // EAST wall
                    if ((mask and EAST) == 0) {
                        drawLine(
                            color = theme.wallColor,
                            start = Offset(sx + cellSize, sy),
                            end = Offset(sx + cellSize, sy + cellSize),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }

            // 4. Draw Goal
            if (maze.goal.x in originX until originX + viewW && maze.goal.y in originY until originY + viewH) {
                val lx = maze.goal.x - originX
                val ly = maze.goal.y - originY
                val gxCenter = offX + lx * cellSize + cellSize / 2f
                val gyCenter = offY + (viewH - 1 - ly) * cellSize + cellSize / 2f

                // Outer beacon glow
                drawCircle(
                    color = theme.goalColor.copy(alpha = 0.25f),
                    radius = cellSize * 0.45f,
                    center = Offset(gxCenter, gyCenter)
                )

                // Goal flag symbol text
                val fontSizePx = max(14f, cellSize * 0.72f)
                val paint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = fontSizePx
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "🚩",
                    gxCenter,
                    gyCenter + fontSizePx * 0.35f,
                    paint
                )
            }

            // 5. Draw Player
            val pX = animPlayerX
            val pY = animPlayerY
            if (pX >= originX - 1 && pX <= originX + viewW && pY >= originY - 1 && pY <= originY + viewH) {
                val lx = pX - originX
                val ly = pY - originY
                val pxCenter = offX + lx * cellSize + cellSize / 2f
                val pyCenter = offY + (viewH - 1 - ly) * cellSize + cellSize / 2f

                // Player outer glow ring
                drawCircle(
                    color = skin.glowColor.copy(alpha = 0.35f),
                    radius = cellSize * 0.38f,
                    center = Offset(pxCenter, pyCenter)
                )

                // Player body or icon
                if (skin.id == "classic_blue") {
                    drawCircle(
                        color = skin.primaryColor,
                        radius = cellSize * 0.28f,
                        center = Offset(pxCenter, pyCenter)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = cellSize * 0.12f,
                        center = Offset(pxCenter - cellSize * 0.08f, pyCenter - cellSize * 0.08f)
                    )
                } else {
                    val fontSizePx = max(14f, cellSize * 0.75f)
                    val paint = android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = fontSizePx
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        skin.iconEmoji,
                        pxCenter,
                        pyCenter + fontSizePx * 0.35f,
                        paint
                    )
                }
            }
        }
    }
}
