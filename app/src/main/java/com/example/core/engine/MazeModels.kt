package com.example.core.engine

const val NORTH = 1
const val EAST = 2
const val SOUTH = 4
const val WEST = 8

data class Maze(
    val w: Int,
    val h: Int,
    val masks: IntArray,
    val start: Point,
    val goal: Point,
    val spine: List<Point>,
    val target: Int,
    val seedStr: String
) {
    fun cellAt(x: Int, y: Int): Int {
        if (x !in 0 until w || y !in 0 until h) return 0
        return masks[y * w + x]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Maze
        return w == other.w && h == other.h && seedStr == other.seedStr
    }

    override fun hashCode(): Int {
        var result = w
        result = 31 * result + h
        result = 31 * result + seedStr.hashCode()
        return result
    }
}

data class LevelDef(
    val id: String,
    val numericLevel: Int?,
    val tier: String? = null,
    val w: Int,
    val h: Int,
    val target: Int,
    val moveCap: Int? = null,
    val vision: Int? = null,
    val isFinal: Boolean = false,
    val noTimer: Boolean = false
)

object MazeConfig {
    const val MAX_LEVEL = 295
    const val PASSES_TO_UNLOCK_IMPOSSIBLE = 50
    const val FULL_VISION_MAX_SIZE = 10
    const val VISION_WINDOW = 10
    const val NO_STAR_GATE_UNTIL = 7

    fun difficultyRatio(i: Int): Double = (0.72 + i * 0.0006).coerceAtMost(0.92)

    fun generateLevelDef(levelNumber: Int): LevelDef {
        val size = 4 + levelNumber
        val n = size * size
        val ratio = difficultyRatio(levelNumber)
        val target = ((n - 1) * ratio).toInt().coerceIn(1, n - 1)
        val vision = if (size > FULL_VISION_MAX_SIZE) VISION_WINDOW else null
        return LevelDef(
            id = levelNumber.toString(),
            numericLevel = levelNumber,
            w = size,
            h = size,
            target = target,
            moveCap = null,
            vision = vision,
            isFinal = false,
            noTimer = false
        )
    }

    val IMPOSSIBLE_TIER_SIZES = listOf(300, 400, 500)
    const val SUPER_SIZE = 1000

    fun impossibleDefForTier(tier: Int): LevelDef {
        val size = IMPOSSIBLE_TIER_SIZES[tier - 1]
        val target = (size * size) / 100
        return LevelDef(
            id = "IMPOSSIBLE_$tier",
            numericLevel = null,
            tier = tier.toString(),
            w = size,
            h = size,
            target = target,
            moveCap = target,
            vision = VISION_WINDOW,
            isFinal = true,
            noTimer = true
        )
    }

    fun superDef(): LevelDef {
        val target = (SUPER_SIZE * SUPER_SIZE) / 100
        return LevelDef(
            id = "IMPOSSIBLE_SUPER",
            numericLevel = null,
            tier = "SUPER",
            w = SUPER_SIZE,
            h = SUPER_SIZE,
            target = target,
            moveCap = target,
            vision = VISION_WINDOW,
            isFinal = true,
            noTimer = true
        )
    }

    fun starReqFor(levelNumber: Int): Int {
        if (levelNumber <= NO_STAR_GATE_UNTIL) return 0
        if (levelNumber % 5 == 0) {
            return 3 * levelNumber - 5
        }
        return 0
    }

    fun starsFromPace(secPerCell: Double): Int = when {
        secPerCell <= 0.7 -> 3
        secPerCell <= 1.0 -> 2
        secPerCell <= 2.0 -> 1
        else -> 0
    }

    fun encodeCoord(p: Point): String {
        var n = p.x
        val out = StringBuilder()
        do {
            out.append(('A'.code + (n % 26)).toChar())
            n /= 26
        } while (n > 0)
        while (out.length < 2) out.append('A')
        val xStr = out.reverse().toString()
        return "$xStr${p.y.toString().padStart(2, '0')}"
    }
}
