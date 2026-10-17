package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Values per step of the rotation loops; the butterflies and bit reversal work on [OCTET] values. */
private const val QUAD = 4
private const val OCTET = 8

/** `log2(OCTET)`: the butterfly stages stop at groups of [OCTET] values. */
private const val OCTET_BITS = 3

/** The client scales by `0.5f` in the input and output steps. */
private const val HALF = 0.5

/**
 * Inverse MDCT of blocksize [n] in place: [b] holds `n / 2` coefficients on entry and `n` samples on return.
 *
 * The steps are those of the Vorbis I specification, section 4.3.5, in the order and float
 * rounding of the game client, so that the output is bit-exact.
 */
internal fun inverseMdct(
    b: DoubleArray,
    n: Int,
    tables: TrigTables,
) {
    val n2 = n / 2
    for (i in 0 until n2) b[i] = fround(b[i] * HALF)
    for (i in n2 until n) b[i] = -b[n - i - 1]
    rotateInput(b, n, tables.a)
    butterflies(b, n, tables.a)
    reverseBits(b, n, tables.bitReverse)
    reorder(b, n)
    rotateOutput(b, n, tables.b, tables.c)
    unfold(b, n)
}

private fun f(x: Double): Double = fround(x)

private fun rotateInput(
    b: DoubleArray,
    n: Int,
    a: DoubleArray,
) {
    val n2 = n / 2
    for (k in 0 until n / QUAD) {
        val lo = QUAD * k
        val hi = n - 1 - lo
        val x = f(b[lo] - b[hi])
        val y = f(b[lo + 2] - b[hi - 2])
        val c = a[2 * k]
        val s = a[2 * k + 1]
        b[hi] = f(f(c * x) - f(s * y))
        b[hi - 2] = f(f(s * x) + f(y * c))
    }
    for (k in 0 until n / OCTET) {
        val lo = QUAD * k + 1
        val hi = n2 + lo
        val p = b[hi + 2]
        val q = b[hi]
        val r = b[lo + 2]
        val t = b[lo]
        b[hi + 2] = f(r + p)
        b[hi] = f(q + t)
        val w = n2 - QUAD * (k + 1)
        val w0 = a[w]
        val w1 = a[w + 1]
        b[lo + 2] = f(f(w0 * f(p - r)) - f(w1 * f(q - t)))
        b[lo] = f(f(w0 * f(q - t)) + f(f(p - r) * w1))
    }
}

private fun butterflies(
    b: DoubleArray,
    n: Int,
    a: DoubleArray,
) {
    val stages = ilog(n - 1) - OCTET_BITS
    for (l in 0 until stages) {
        val k0 = n shr (l + 2)
        val k1 = OCTET shl l
        for (r in 0 until (2 shl l)) {
            val i0 = n - r * 2 * k0
            val i1 = n - (r * 2 + 1) * k0
            for (j in 0 until k0 / QUAD) {
                val even = i0 - 1 - QUAD * j
                val odd = i1 - 1 - QUAD * j
                val e0 = b[even]
                val e2 = b[even - 2]
                val o0 = b[odd]
                val o2 = b[odd - 2]
                b[even] = f(o0 + e0)
                b[even - 2] = f(e2 + o2)
                val w0 = a[k1 * j]
                val w1 = a[1 + k1 * j]
                b[odd] = f(f(f(e0 - o0) * w0) - f(f(e2 - o2) * w1))
                b[odd - 2] = f(f(w0 * f(e2 - o2)) + f(f(e0 - o0) * w1))
            }
        }
    }
}

private fun reverseBits(
    b: DoubleArray,
    n: Int,
    bitReverse: IntArray,
) {
    for (i in 1 until n / OCTET - 1) {
        val j = bitReverse[i]
        if (i < j) {
            for (o in 1 until OCTET step 2) {
                val swap = b[OCTET * i + o]
                b[OCTET * i + o] = b[OCTET * j + o]
                b[OCTET * j + o] = swap
            }
        }
    }
}

private fun reorder(
    b: DoubleArray,
    n: Int,
) {
    val n4 = n / QUAD
    for (i in 0 until n / 2) b[i] = b[2 * i + 1]
    for (i in 0 until n / OCTET) {
        val first = QUAD * i
        val second = first + 2
        b[n - 1 - 2 * i] = b[first]
        b[n - 2 - 2 * i] = b[first + 1]
        b[n - n4 - 1 - 2 * i] = b[second]
        b[n - n4 - 2 - 2 * i] = b[second + 1]
    }
}

private fun rotateOutput(
    b: DoubleArray,
    n: Int,
    bTable: DoubleArray,
    cTable: DoubleArray,
) {
    val n2 = n / 2
    for (i in 0 until n / OCTET) {
        val c = cTable[2 * i]
        val s = cTable[2 * i + 1]
        val p = b[n2 + 2 * i]
        val q = b[n2 + 2 * i + 1]
        val r = b[n - 2 - 2 * i]
        val t = b[n - 1 - 2 * i]
        var u = f(f(c * f(t + q)) + f(f(p - r) * s))
        b[2 * i + n2] = f(HALF * f(u + f(p + r)))
        b[n - 2 - 2 * i] = f(HALF * f(f(r + p) - u))
        u = f(f(f(q + t) * s) - f(c * f(p - r)))
        b[1 + n2 + 2 * i] = f(HALF * f(u + f(q - t)))
        b[n - 1 - 2 * i] = f(f(f(t - q) + u) * HALF)
    }
    for (i in 0 until n / QUAD) {
        b[i] = f(f(bTable[2 * i + 1] * b[1 + 2 * i + n2]) + f(b[n2 + 2 * i] * bTable[2 * i]))
        b[n2 - 1 - i] =
            f(f(b[2 * i + n2] * bTable[1 + 2 * i]) - f(b[2 * i + 1 + n2] * bTable[2 * i]))
    }
}

private fun unfold(
    b: DoubleArray,
    n: Int,
) {
    val n2 = n / 2
    val n4 = n / QUAD
    for (i in 0 until n4) b[n - n4 + i] = -b[i]
    for (i in 0 until n4) b[i] = b[i + n4]
    for (i in 0 until n4) b[n4 + i] = -b[n4 - i - 1]
    for (i in 0 until n4) b[n2 + i] = b[n - i - 1]
}

/** The rotation and bit-reversal tables of the IMDCT of blocksize [n]. */
internal class TrigTables(
    n: Int,
) {
    val a = DoubleArray(n / 2)
    val b = DoubleArray(n / 2)
    val c = DoubleArray(n / QUAD)
    val bitReverse = IntArray(n / OCTET)

    init {
        for (k in 0 until n / QUAD) {
            a[2 * k] = fround(cos(PI * (k * QUAD) / n))
            a[2 * k + 1] = -fround(sin(PI * (k * QUAD) / n))
            b[2 * k] = fround(cos(PI * (2 * k + 1) / (n * 2)))
            b[2 * k + 1] = fround(sin((2 * k + 1) * PI / (2 * n)))
        }
        for (k in 0 until n / OCTET) {
            c[2 * k] = fround(cos(PI * (QUAD * k + 2) / n))
            c[2 * k + 1] = -fround(sin((QUAD * k + 2) * PI / n))
        }
        val bits = ilog(n / OCTET - 1)
        for (k in bitReverse.indices) {
            var reversed = 0
            for (bit in 0 until bits) {
                if (k and (1 shl bit) != 0) reversed = reversed or (1 shl (bits - 1 - bit))
            }
            bitReverse[k] = reversed
        }
    }
}
