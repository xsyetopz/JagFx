package io.github.xsyetopz.jagfx.core.vorbis

private const val TYPE_BITS = 16
private const val RANGE_BITS = 24
private const val CLASSIFICATIONS_BITS = 6
private const val BOOK_BITS = 8
private const val CASCADE_LOW_BITS = 3
private const val CASCADE_HIGH_BITS = 5
private const val PASSES = 8
private const val INTERLEAVED_TYPE = 0

/** Vorbis residue types 0, 1, and 2, for one channel. */
internal class Residue(
    private val type: Int,
    private val begin: Int,
    private val end: Int,
    private val partitionSize: Int,
    private val classbook: Int,
    /** Book of class `c` in pass `p` at `c * 8 + p`, or `-1`. */
    private val books: IntArray,
) {
    private val classifications = books.size / PASSES

    /** Replaces `buffer[0 until n2]` with the residue vector of one packet, or with zeros. */
    fun decode(
        reader: BitReader,
        buffer: DoubleArray,
        n2: Int,
        doNotDecode: Boolean,
        codebooks: List<Codebook>,
    ) {
        buffer.fill(0.0, 0, n2)
        if (doNotDecode) return
        val classifier = codebooks[classbook]
        val partitions = (end - begin) / partitionSize
        val classes = IntArray(partitions)
        for (pass in 0 until PASSES) {
            val passBooks = List(classifications) { codebooks.getOrNull(books[it * PASSES + pass]) }
            var i = 0
            while (i < partitions) {
                if (pass == 0) {
                    val value = classifier.decodeScalar(reader)
                    readClasses(value, classifier.dimensions, classes, i)
                }
                val stop = minOf(i + classifier.dimensions, partitions)
                decodePass(reader, buffer, classes, passBooks, i until stop)
                i = stop
            }
        }
    }

    /** Decodes the partitions in [range] with the book of each class in one pass, if it has one. */
    private fun decodePass(
        reader: BitReader,
        buffer: DoubleArray,
        classes: IntArray,
        passBooks: List<Codebook?>,
        range: IntRange,
    ) {
        for (i in range) {
            val book = passBooks[classes[i]] ?: continue
            decodePartition(reader, book, buffer, begin + i * partitionSize)
        }
    }

    /** Stores the [count] classes packed in [value], first class in the highest digit, from [start]. */
    private fun readClasses(
        value: Int,
        count: Int,
        classes: IntArray,
        start: Int,
    ) {
        var rest = value
        for (j in count - 1 downTo 0) {
            if (start + j < classes.size) classes[start + j] = rest % classifications
            rest /= classifications
        }
    }

    private fun decodePartition(
        reader: BitReader,
        book: Codebook,
        buffer: DoubleArray,
        offset: Int,
    ) {
        val dimensions = book.dimensions
        if (type == INTERLEAVED_TYPE) {
            val step = partitionSize / dimensions
            for (t in 0 until step) {
                val vector = book.decodeVector(reader)
                for (d in 0 until dimensions) {
                    val index = offset + t + d * step
                    buffer[index] = fround(buffer[index] + vector[d])
                }
            }
        } else {
            var j = 0
            while (j < partitionSize) {
                val vector = book.decodeVector(reader)
                for (d in 0 until dimensions) {
                    buffer[offset + j] = fround(buffer[offset + j] + vector[d])
                    j++
                }
            }
        }
    }

    companion object {
        fun parse(reader: BitReader): Residue {
            val type = reader.bits(TYPE_BITS)
            val begin = reader.bits(RANGE_BITS)
            val end = reader.bits(RANGE_BITS)
            val partitionSize = reader.bits(RANGE_BITS) + 1
            val classifications = reader.bits(CLASSIFICATIONS_BITS) + 1
            val classbook = reader.bits(BOOK_BITS)
            val cascades =
                IntArray(classifications) {
                    val low = reader.bits(CASCADE_LOW_BITS)
                    val high = if (reader.bit() != 0) reader.bits(CASCADE_HIGH_BITS) else 0
                    (high shl CASCADE_LOW_BITS) or low
                }
            val books =
                IntArray(classifications * PASSES) {
                    val cls = it / PASSES
                    val pass = it % PASSES
                    if (cascades[cls] and (1 shl pass) != 0) reader.bits(BOOK_BITS) else -1
                }
            return Residue(type, begin, end, partitionSize, classbook, books)
        }
    }
}

/** Vorbis mapping type 0 for one channel. */
internal class Mapping(
    /** Floor of each submap. */
    val submapFloors: IntArray,
    /** Residue of each submap. */
    val submapResidues: IntArray,
    /** Submap of the channel. */
    val mux: Int,
) {
    companion object {
        private const val SUBMAPS_BITS = 4
        private const val COUPLING_STEPS_BITS = 8
        private const val RESERVED_BITS = 2
        private const val MUX_BITS = 4
        private const val INDEX_BITS = 8

        fun parse(reader: BitReader): Mapping {
            reader.bits(TYPE_BITS)
            val submaps = if (reader.bit() != 0) reader.bits(SUBMAPS_BITS) + 1 else 1
            if (reader.bit() != 0) reader.bits(COUPLING_STEPS_BITS)
            reader.bits(RESERVED_BITS)
            val mux = if (submaps > 1) reader.bits(MUX_BITS) else 0
            val floors = IntArray(submaps)
            val residues = IntArray(submaps)
            for (i in 0 until submaps) {
                reader.bits(INDEX_BITS)
                floors[i] = reader.bits(INDEX_BITS)
                residues[i] = reader.bits(INDEX_BITS)
            }
            return Mapping(floors, residues, mux)
        }
    }
}
