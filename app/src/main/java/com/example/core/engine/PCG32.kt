package com.example.core.engine

/**
 * 64-bit PCG32 Random Number Generator
 * Exact port of the proven algorithm from the core engine.
 */
class PCG32(seedBig: ULong) {
    private val inc: ULong = ((seedBig shl 1) or 1UL)
    private var state: ULong = 0UL

    init {
        nextU32()
        state = state + seedBig + 0x9E3779B97F4A7C15UL
        nextU32()
    }

    fun nextU32(): UInt {
        val old = state
        state = old * 6364136223846793005UL + inc
        val xorShifted = (((old shr 18) xor old) shr 27).toUInt()
        val rot = (old shr 59).toInt()
        val left = xorShifted shr rot
        val right = if (rot == 0) 0u else (xorShifted shl (32 - rot))
        return left or right
    }

    fun nextInt(lo: Int, hi: Int): Int {
        val range = (hi - lo).toUInt()
        if (range <= 1u) return lo
        val limit = (0xFFFFFFFFu / range) * range
        var v: UInt
        do {
            v = nextU32()
        } while (v >= limit)
        return lo + (v % range).toInt()
    }

    fun nextBool(): Boolean = (nextU32() and 1u) != 0u
}
