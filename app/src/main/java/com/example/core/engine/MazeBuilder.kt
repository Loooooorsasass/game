package com.example.core.engine

import java.util.ArrayDeque
import kotlin.random.Random

object MazeBuilder {
    private val DIRS = listOf(
        Triple(0, 1, Pair(NORTH, SOUTH)),
        Triple(1, 0, Pair(EAST, WEST)),
        Triple(0, -1, Pair(SOUTH, NORTH)),
        Triple(-1, 0, Pair(WEST, EAST))
    )

    fun randomSeed(): ULong {
        val a = Random.nextLong().toULong()
        return a
    }

    fun buildMaze(w: Int, h: Int, target: Int, seedBig: ULong): Maze {
        val rng = PCG32(seedBig)
        var path = GilbertCurve.traversal(w, h)

        if (rng.nextBool()) {
            path = path.map { Point(w - 1 - it.x, it.y) }
        }
        if (rng.nextBool()) {
            path = path.map { Point(it.x, h - 1 - it.y) }
        }
        if (rng.nextBool()) {
            path = path.reversed()
        }

        val n = w * h
        val maxStart = (n - 1 - target).coerceAtLeast(0)
        val startIndex = if (maxStart > 0) rng.nextInt(0, maxStart + 1) else 0
        val spine = path.subList(startIndex, (startIndex + target + 1).coerceAtMost(path.size))

        val start = spine.first()
        val goal = spine.last()

        val masks = IntArray(n) { 0 }
        val inTree = BooleanArray(n) { false }

        for (i in spine.indices) {
            val (cx, cy) = spine[i]
            inTree[cy * w + cx] = true
            if (i > 0) {
                val (px, py) = spine[i - 1]
                val dx = cx - px
                val dy = cy - py
                val (d, od) = when {
                    dx == 1 && dy == 0 -> Pair(EAST, WEST)
                    dx == -1 && dy == 0 -> Pair(WEST, EAST)
                    dx == 0 && dy == 1 -> Pair(NORTH, SOUTH)
                    else -> Pair(SOUTH, NORTH)
                }
                masks[py * w + px] = masks[py * w + px] or d
                masks[cy * w + cx] = masks[cy * w + cx] or od
            }
        }

        data class Frontier(val parent: Int, val child: Int, val d: Int)
        val frontier = ArrayList<Frontier>()

        fun addFrontier(p: Point) {
            val pi = p.y * w + p.x
            for ((dx, dy, dirPair) in DIRS) {
                val nx = p.x + dx
                val ny = p.y + dy
                if (nx in 0 until w && ny in 0 until h && !inTree[ny * w + nx]) {
                    frontier.add(Frontier(pi, ny * w + nx, dirPair.first))
                }
            }
        }

        for (c in spine) {
            addFrontier(c)
        }

        var count = spine.size
        while (count < n && frontier.isNotEmpty()) {
            val pick = rng.nextInt(0, frontier.size)
            val item = frontier.removeAt(pick)
            if (inTree[item.child]) continue

            val opp = when (item.d) {
                NORTH -> SOUTH
                EAST -> WEST
                SOUTH -> NORTH
                WEST -> EAST
                else -> 0
            }

            masks[item.parent] = masks[item.parent] or item.d
            masks[item.child] = masks[item.child] or opp
            inTree[item.child] = true
            count++

            addFrontier(Point(item.child % w, item.child / w))
        }

        return Maze(
            w = w,
            h = h,
            masks = masks,
            start = start,
            goal = goal,
            spine = spine,
            target = target,
            seedStr = seedBig.toString()
        )
    }
}

object MazeSolver {
    fun computeDistances(maze: Maze): IntArray {
        val n = maze.w * maze.h
        val dist = IntArray(n) { -1 }
        val startIdx = maze.start.y * maze.w + maze.start.x
        dist[startIdx] = 0

        val queue = ArrayDeque<Int>()
        queue.add(startIdx)

        val dirs = listOf(
            Triple(0, 1, NORTH),
            Triple(1, 0, EAST),
            Triple(0, -1, SOUTH),
            Triple(-1, 0, WEST)
        )

        while (queue.isNotEmpty()) {
            val curr = queue.poll()
            val cx = curr % maze.w
            val cy = curr / maze.w
            val m = maze.masks[curr]

            for ((dx, dy, dir) in dirs) {
                if ((m and dir) != 0) {
                    val nx = cx + dx
                    val ny = cy + dy
                    val ni = ny * maze.w + nx
                    if (dist[ni] == -1) {
                        dist[ni] = dist[curr] + 1
                        queue.add(ni)
                    }
                }
            }
        }
        return dist
    }

    fun findPathToGoal(maze: Maze): List<Point> {
        return maze.spine
    }
}
