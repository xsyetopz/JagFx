package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.math.pow

private const val SYNC_BITS = 24
private const val DIMENSIONS_BITS = 16
private const val ENTRIES_BITS = 24
private const val LENGTH_BITS = 5
private const val LOOKUP_TYPE_BITS = 4
private const val VALUE_BITS_BITS = 4
private const val LOOKUP_IMPLICIT = 1
private const val INITIAL_TREE_SIZE = 8

/**
 * A Vorbis codebook.
 *
 * The Huffman tree is an [IntArray]: a node holds the index of its 1 child, its 0 child follows it,
 * and a leaf holds the bitwise NOT of its entry.
 */
internal class Codebook(
    val dimensions: Int,
    private val tree: IntArray,
    private val vectors: Array<DoubleArray>?,
) {
    fun decodeScalar(reader: BitReader): Int {
        var node = 0
        while (tree[node] >= 0) node = if (reader.bit() != 0) tree[node] else node + 1
        return tree[node].inv()
    }

    fun decodeVector(reader: BitReader): DoubleArray {
        val table = checkNotNull(vectors) { "codebook has no value lookup" }
        return table[decodeScalar(reader)]
    }

    companion object {
        fun parse(reader: BitReader): Codebook {
            reader.bits(SYNC_BITS)
            val dimensions = reader.bits(DIMENSIONS_BITS)
            val entries = reader.bits(ENTRIES_BITS)
            val lengths = readLengths(reader, entries)
            val tree = buildTree(lengths)
            val lookupType = reader.bits(LOOKUP_TYPE_BITS)
            val vectors =
                if (lookupType >
                    0
                ) {
                    readVectors(reader, lookupType, entries, dimensions)
                } else {
                    null
                }
            return Codebook(dimensions, tree, vectors)
        }

        private fun readLengths(
            reader: BitReader,
            entries: Int,
        ): IntArray {
            val lengths = IntArray(entries)
            if (reader.bit() != 0) {
                var entry = 0
                var length = reader.bits(LENGTH_BITS) + 1
                while (entry < entries) {
                    repeat(reader.bits(ilog(entries - entry))) { lengths[entry++] = length }
                    length++
                }
            } else {
                val sparse = reader.bit() != 0
                for (entry in 0 until entries) {
                    lengths[entry] =
                        if (sparse && reader.bit() == 0) 0 else reader.bits(LENGTH_BITS) + 1
                }
            }
            return lengths
        }

        /** Assigns canonical codewords, left-justified in 32 bits, in entry order. */
        private fun codewords(lengths: IntArray): IntArray {
            val codes = IntArray(lengths.size)
            val next = IntArray(Int.SIZE_BITS + 1)
            for (entry in lengths.indices) {
                val length = lengths[entry]
                if (length == 0) continue
                val bit = 1 shl (Int.SIZE_BITS - length)
                val code = next[length]
                codes[entry] = code
                val following =
                    if (code and bit != 0) {
                        next[length - 1]
                    } else {
                        advanceShorter(next, length, code)
                        code or bit
                    }
                next[length] = following
                for (longer in length + 1..Int.SIZE_BITS) {
                    if (next[longer] == code) next[longer] = following
                }
            }
            return codes
        }

        private fun advanceShorter(
            next: IntArray,
            length: Int,
            code: Int,
        ) {
            for (shorter in length - 1 downTo 1) {
                val candidate = next[shorter]
                if (candidate != code) return
                val bit = 1 shl (Int.SIZE_BITS - shorter)
                if (candidate and bit != 0) {
                    next[shorter] = next[shorter - 1]
                    return
                }
                next[shorter] = candidate or bit
            }
        }

        private fun buildTree(lengths: IntArray): IntArray {
            val codes = codewords(lengths)
            var tree = IntArray(INITIAL_TREE_SIZE)
            var nextFree = 0
            for (entry in lengths.indices) {
                val length = lengths[entry]
                if (length == 0) continue
                var node = 0
                for (i in 0 until length) {
                    val one = codes[entry] and (Int.MIN_VALUE ushr i) != 0
                    node = if (one) oneChild(tree, node, nextFree) else node + 1
                    if (node >= tree.size) tree = tree.copyOf(tree.size * 2)
                }
                tree[node] = entry.inv()
                if (node >= nextFree) nextFree = node + 1
            }
            return tree
        }

        /** The 1 child of [node], which becomes [nextFree] when the node has none yet. */
        private fun oneChild(
            tree: IntArray,
            node: Int,
            nextFree: Int,
        ): Int {
            if (tree[node] == 0) tree[node] = nextFree
            return tree[node]
        }

        private fun readVectors(
            reader: BitReader,
            lookupType: Int,
            entries: Int,
            dimensions: Int,
        ): Array<DoubleArray> {
            val minimum = float32Unpack(reader.bits(Int.SIZE_BITS))
            val delta = float32Unpack(reader.bits(Int.SIZE_BITS))
            val valueBits = reader.bits(VALUE_BITS_BITS) + 1
            val sequence = reader.bit() != 0
            val valueCount =
                if (lookupType ==
                    LOOKUP_IMPLICIT
                ) {
                    lookup1Values(entries, dimensions)
                } else {
                    entries * dimensions
                }
            val multiplicands = IntArray(valueCount) { reader.bits(valueBits) }
            return Array(entries) { entry ->
                var last = 0.0
                var divisor = 1
                DoubleArray(dimensions) { d ->
                    val index =
                        if (lookupType == LOOKUP_IMPLICIT) {
                            entry / divisor % valueCount
                        } else {
                            entry * dimensions + d
                        }
                    val value =
                        fround(fround(fround(multiplicands[index] * delta) + minimum) + last)
                    if (sequence) last = value
                    divisor *= valueCount
                    value
                }
            }
        }

        /** The largest `v` with `v` to the power [dimensions] at most [entries]. */
        private fun lookup1Values(
            entries: Int,
            dimensions: Int,
        ): Int {
            var values = entries.toDouble().pow(1.0 / dimensions).toInt() + 1
            while (intPow(values, dimensions) > entries) values--
            return values
        }

        /** [base] to the power [exponent] with [Int] overflow. */
        private fun intPow(
            base: Int,
            exponent: Int,
        ): Int {
            var result = 1
            var square = base
            var remaining = exponent
            while (remaining > 1) {
                if (remaining and 1 != 0) result *= square
                square *= square
                remaining = remaining shr 1
            }
            return if (remaining == 1) square * result else result
        }
    }
}
