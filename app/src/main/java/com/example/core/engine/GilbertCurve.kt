package com.example.core.engine

import kotlin.math.abs

data class Point(val x: Int, val y: Int)

object GilbertCurve {
    private fun sign(v: Int): Int = when {
        v > 0 -> 1
        v < 0 -> -1
        else -> 0
    }

    private fun generate(
        x: Int, y: Int,
        ax: Int, ay: Int,
        bx: Int, by: Int,
        out: MutableList<Point>
    ) {
        val cw = abs(ax + ay)
        val ch = abs(bx + by)
        val dax = sign(ax)
        val day = sign(ay)
        val dbx = sign(bx)
        val dby = sign(by)

        if (ch == 1) {
            var cx = x
            var cy = y
            for (i in 0 until cw) {
                out.add(Point(cx, cy))
                cx += dax
                cy += day
            }
            return
        }

        if (cw == 1) {
            var cx = x
            var cy = y
            for (i in 0 until ch) {
                out.add(Point(cx, cy))
                cx += dbx
                cy += dby
            }
            return
        }

        var ax2 = ax / 2
        var ay2 = ay / 2
        var bx2 = bx / 2
        var by2 = by / 2

        val w2 = abs(ax2 + ay2)
        val h2 = abs(bx2 + by2)

        if (2 * cw > 3 * ch) {
            if (w2 % 2 != 0 && cw > 2) {
                ax2 += dax
                ay2 += day
            }
            generate(x, y, ax2, ay2, bx, by, out)
            generate(x + ax2, y + ay2, ax - ax2, ay - ay2, bx, by, out)
        } else {
            if (h2 % 2 != 0 && ch > 2) {
                bx2 += dbx
                by2 += dby
            }
            generate(x, y, bx2, by2, ax2, ay2, out)
            generate(x + bx2, y + by2, ax, ay, bx - bx2, by - by2, out)
            generate(
                x + (ax - dax) + (bx2 - dbx),
                y + (ay - day) + (by2 - dby),
                -bx2, -by2,
                -(ax - ax2), -(ay - ay2),
                out
            )
        }
    }

    fun traversal(w: Int, h: Int): List<Point> {
        val out = ArrayList<Point>(w * h)
        if (w >= h) {
            generate(0, 0, w, 0, 0, h, out)
        } else {
            generate(0, 0, 0, h, w, 0, out)
        }
        return out
    }
}
