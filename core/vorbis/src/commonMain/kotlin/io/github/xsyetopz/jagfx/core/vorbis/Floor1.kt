package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.math.abs
import kotlin.math.min

private const val PARTITIONS_BITS = 5
private const val CLASS_BITS = 4
private const val CLASS_DIMENSIONS_BITS = 3
private const val SUBCLASS_BITS = 2
private const val BOOK_BITS = 8
private const val MULTIPLIER_BITS = 2
private const val RANGE_BITS_BITS = 4

/** Vorbis floor type 1. */
internal class Floor1(
    private val partitionClasses: IntArray,
    private val classes: List<PartitionClass>,
    private val multiplier: Int,
    private val xList: IntArray,
) {
    private val range = FLOOR_RANGES[multiplier - 1]

    class PartitionClass(
        val dimensions: Int,
        val subclassBits: Int,
        val masterBook: Int,
        /** Book of each subclass, or `-1`. */
        val subBooks: IntArray,
    )

    /** For each X list index from 2, the earlier index with the closest smaller X. */
    private val lowNeighbors = IntArray(xList.size)

    /** For each X list index from 2, the earlier index with the closest larger X. */
    private val highNeighbors = IntArray(xList.size)

    /** X list indices in ascending order of X. */
    private val sortedOrder = xList.indices.sortedBy { xList[it] }.toIntArray()

    init {
        for (i in 2 until xList.size) {
            var low = 0
            var high = 1
            var lowX = -1
            var highX = Int.MAX_VALUE
            for (j in 0 until i) {
                val x = xList[j]
                if (x in (lowX + 1)..<xList[i]) {
                    low = j
                    lowX = x
                }
                if (x > xList[i] && x < highX) {
                    high = j
                    highX = x
                }
            }
            lowNeighbors[i] = low
            highNeighbors[i] = high
        }
    }

    /** Reads the floor of one packet: the coded Y values, or `null` when the floor is unused. */
    fun decode(
        reader: BitReader,
        books: List<Codebook>,
    ): IntArray? {
        if (reader.bit() == 0) return null
        val y = IntArray(xList.size)
        val yBits = ilog(range - 1)
        y[0] = reader.bits(yBits)
        y[1] = reader.bits(yBits)
        var offset = 2
        for (index in partitionClasses) {
            val cls = classes[index]
            val mask = (1 shl cls.subclassBits) - 1
            var subclass =
                if (cls.subclassBits >
                    0
                ) {
                    books[cls.masterBook].decodeScalar(reader)
                } else {
                    0
                }
            repeat(cls.dimensions) {
                val book = cls.subBooks[subclass and mask]
                subclass = subclass ushr cls.subclassBits
                y[offset++] = if (book >= 0) books[book].decodeScalar(reader) else 0
            }
        }
        return y
    }

    /** Multiplies `buffer[0 until n2]` by the floor curve of the coded values [y]. */
    fun render(
        y: IntArray,
        buffer: DoubleArray,
        n2: Int,
    ) {
        val finalY = IntArray(xList.size)
        val used = BooleanArray(xList.size)
        finalY[0] = y[0]
        finalY[1] = y[1]
        used[0] = true
        used[1] = true
        for (i in 2 until xList.size) {
            val low = lowNeighbors[i]
            val high = highNeighbors[i]
            val predicted =
                renderPoint(xList[low], finalY[low], xList[high], finalY[high], xList[i])
            val value = y[i]
            if (value == 0) {
                finalY[i] = predicted
                continue
            }
            used[low] = true
            used[high] = true
            used[i] = true
            finalY[i] = unwrap(value, predicted)
        }
        var lastX = 0
        var lastY = finalY[sortedOrder[0]] * multiplier
        for (s in 1 until sortedOrder.size) {
            val i = sortedOrder[s]
            if (!used[i]) continue
            val x = xList[i]
            val level = finalY[i] * multiplier
            buffer.renderLine(lastX, lastY, x, level, n2)
            if (x >= n2) return
            lastX = x
            lastY = level
        }
        val gain = INVERSE_DB_TABLE[lastY].toDouble()
        for (x in lastX until n2) buffer[x] = fround(buffer[x] * gain)
    }

    private fun unwrap(
        value: Int,
        predicted: Int,
    ): Int {
        val highRoom = range - predicted
        val lowRoom = predicted
        val room = min(highRoom, lowRoom) * 2
        return when {
            value >= room -> if (highRoom > lowRoom) value else highRoom - value + predicted - 1
            value and 1 != 0 -> predicted - (value + 1) / 2
            else -> predicted + value / 2
        }
    }

    companion object {
        fun parse(reader: BitReader): Floor1 {
            val partitionClasses =
                IntArray(reader.bits(PARTITIONS_BITS)) { reader.bits(CLASS_BITS) }
            val classCount = (partitionClasses.maxOrNull() ?: -1) + 1
            val classes =
                List(classCount) {
                    val dimensions = reader.bits(CLASS_DIMENSIONS_BITS) + 1
                    val subclassBits = reader.bits(SUBCLASS_BITS)
                    val masterBook = if (subclassBits != 0) reader.bits(BOOK_BITS) else 0
                    val subBooks = IntArray(1 shl subclassBits) { reader.bits(BOOK_BITS) - 1 }
                    PartitionClass(dimensions, subclassBits, masterBook, subBooks)
                }
            val multiplier = reader.bits(MULTIPLIER_BITS) + 1
            val rangeBits = reader.bits(RANGE_BITS_BITS)
            val xList = ArrayList<Int>()
            xList += 0
            xList += 1 shl rangeBits
            for (index in partitionClasses) {
                repeat(classes[index].dimensions) { xList += reader.bits(rangeBits) }
            }
            return Floor1(partitionClasses, classes, multiplier, xList.toIntArray())
        }

        /** Value range of the floor Y values for multipliers 1 to 4. */
        private val FLOOR_RANGES = intArrayOf(256, 128, 86, 64)

        /**
         * Floor 1 amplitudes: entry `y` is the linear gain of floor value `y`.
         *
         * The values are the `floor1_inverse_dB_table` of the Vorbis I specification, section 10.1.
         */
        private val INVERSE_DB_TABLE: FloatArray =
            floatArrayOf(
                1.0649863e-07f,
                1.1341951e-07f,
                1.2079015e-07f,
                1.2863978e-07f,
                1.3699951e-07f,
                1.4590251e-07f,
                1.5538408e-07f,
                1.6548181e-07f,
                1.7623575e-07f,
                1.8768855e-07f,
                1.9988561e-07f,
                2.1287530e-07f,
                2.2670913e-07f,
                2.4144197e-07f,
                2.5713223e-07f,
                2.7384213e-07f,
                2.9163793e-07f,
                3.1059021e-07f,
                3.3077411e-07f,
                3.5226968e-07f,
                3.7516214e-07f,
                3.9954229e-07f,
                4.2550680e-07f,
                4.5315863e-07f,
                4.8260743e-07f,
                5.1396998e-07f,
                5.4737065e-07f,
                5.8294187e-07f,
                6.2082472e-07f,
                6.6116941e-07f,
                7.0413592e-07f,
                7.4989464e-07f,
                7.9862701e-07f,
                8.5052630e-07f,
                9.0579828e-07f,
                9.6466216e-07f,
                1.0273513e-06f,
                1.0941144e-06f,
                1.1652161e-06f,
                1.2409384e-06f,
                1.3215816e-06f,
                1.4074654e-06f,
                1.4989305e-06f,
                1.5963394e-06f,
                1.7000785e-06f,
                1.8105592e-06f,
                1.9282195e-06f,
                2.0535261e-06f,
                2.1869758e-06f,
                2.3290978e-06f,
                2.4804557e-06f,
                2.6416497e-06f,
                2.8133190e-06f,
                2.9961443e-06f,
                3.1908506e-06f,
                3.3982101e-06f,
                3.6190449e-06f,
                3.8542308e-06f,
                4.1047004e-06f,
                4.3714470e-06f,
                4.6555282e-06f,
                4.9580707e-06f,
                5.2802740e-06f,
                5.6234160e-06f,
                5.9888572e-06f,
                6.3780469e-06f,
                6.7925283e-06f,
                7.2339451e-06f,
                7.7040476e-06f,
                8.2047000e-06f,
                8.7378876e-06f,
                9.3057248e-06f,
                9.9104632e-06f,
                1.0554501e-05f,
                1.1240392e-05f,
                1.1970856e-05f,
                1.2748789e-05f,
                1.3577278e-05f,
                1.4459606e-05f,
                1.5399272e-05f,
                1.6400004e-05f,
                1.7465768e-05f,
                1.8600792e-05f,
                1.9809576e-05f,
                2.1096914e-05f,
                2.2467911e-05f,
                2.3928002e-05f,
                2.5482978e-05f,
                2.7139006e-05f,
                2.8902651e-05f,
                3.0780908e-05f,
                3.2781225e-05f,
                3.4911534e-05f,
                3.7180282e-05f,
                3.9596466e-05f,
                4.2169667e-05f,
                4.4910090e-05f,
                4.7828601e-05f,
                5.0936773e-05f,
                5.4246931e-05f,
                5.7772202e-05f,
                6.1526565e-05f,
                6.5524908e-05f,
                6.9783085e-05f,
                7.4317983e-05f,
                7.9147585e-05f,
                8.4291040e-05f,
                8.9768747e-05f,
                9.5602426e-05f,
                0.00010181521f,
                0.00010843174f,
                0.00011547824f,
                0.00012298267f,
                0.00013097477f,
                0.00013948625f,
                0.00014855085f,
                0.00015820453f,
                0.00016848555f,
                0.00017943469f,
                0.00019109536f,
                0.00020351382f,
                0.00021673929f,
                0.00023082423f,
                0.00024582449f,
                0.00026179955f,
                0.00027881276f,
                0.00029693158f,
                0.00031622787f,
                0.00033677814f,
                0.00035866388f,
                0.00038197188f,
                0.00040679456f,
                0.00043323036f,
                0.00046138411f,
                0.00049136745f,
                0.00052329927f,
                0.00055730621f,
                0.00059352311f,
                0.00063209358f,
                0.00067317058f,
                0.00071691700f,
                0.00076350630f,
                0.00081312324f,
                0.00086596457f,
                0.00092223983f,
                0.00098217216f,
                0.0010459992f,
                0.0011139742f,
                0.0011863665f,
                0.0012634633f,
                0.0013455702f,
                0.0014330129f,
                0.0015261382f,
                0.0016253153f,
                0.0017309374f,
                0.0018434235f,
                0.0019632195f,
                0.0020908006f,
                0.0022266726f,
                0.0023713743f,
                0.0025254795f,
                0.0026895994f,
                0.0028643847f,
                0.0030505286f,
                0.0032487691f,
                0.0034598925f,
                0.0036847358f,
                0.0039241906f,
                0.0041792066f,
                0.0044507950f,
                0.0047400328f,
                0.0050480668f,
                0.0053761186f,
                0.0057254891f,
                0.0060975636f,
                0.0064938176f,
                0.0069158225f,
                0.0073652516f,
                0.0078438871f,
                0.0083536271f,
                0.0088964928f,
                0.009474637f,
                0.010090352f,
                0.010746080f,
                0.011444421f,
                0.012188144f,
                0.012980198f,
                0.013823725f,
                0.014722068f,
                0.015678791f,
                0.016697687f,
                0.017782797f,
                0.018938423f,
                0.020169149f,
                0.021479854f,
                0.022875735f,
                0.024362330f,
                0.025945531f,
                0.027631618f,
                0.029427276f,
                0.031339626f,
                0.033376252f,
                0.035545228f,
                0.037855157f,
                0.040315199f,
                0.042935108f,
                0.045725273f,
                0.048696758f,
                0.051861348f,
                0.055231591f,
                0.058820850f,
                0.062643361f,
                0.066714279f,
                0.071049749f,
                0.075666962f,
                0.080584227f,
                0.085821044f,
                0.091398179f,
                0.097337747f,
                0.10366330f,
                0.11039993f,
                0.11757434f,
                0.12521498f,
                0.13335215f,
                0.14201813f,
                0.15124727f,
                0.16107617f,
                0.17154380f,
                0.18269168f,
                0.19456402f,
                0.20720788f,
                0.22067342f,
                0.23501402f,
                0.25028656f,
                0.26655159f,
                0.28387361f,
                0.30232132f,
                0.32196786f,
                0.34289114f,
                0.36517414f,
                0.38890521f,
                0.41417847f,
                0.44109412f,
                0.46975890f,
                0.50028648f,
                0.53279791f,
                0.56742212f,
                0.60429640f,
                0.64356699f,
                0.68538959f,
                0.72993007f,
                0.77736504f,
                0.82788260f,
                0.88168307f,
                0.9389798f,
                1.0f,
            )

        private fun renderPoint(
            x0: Int,
            y0: Int,
            x1: Int,
            y1: Int,
            x: Int,
        ): Int {
            val dy = y1 - y0
            val offset = abs(dy) * (x - x0) / (x1 - x0)
            return if (dy < 0) y0 - offset else y0 + offset
        }

        private fun DoubleArray.renderLine(
            x0: Int,
            y0: Int,
            x1: Int,
            y1: Int,
            n: Int,
        ) {
            val buffer = this
            val dy = y1 - y0
            val adx = x1 - x0
            val base = dy / adx
            val step = if (dy < 0) base - 1 else base + 1
            val ady = abs(dy) - abs(base) * adx
            var y = y0
            var error = 0
            buffer[x0] = fround(buffer[x0] * INVERSE_DB_TABLE[y])
            for (x in x0 + 1 until min(x1, n)) {
                error += ady
                if (error >= adx) {
                    error -= adx
                    y += step
                } else {
                    y += base
                }
                buffer[x] = fround(buffer[x] * INVERSE_DB_TABLE[y])
            }
        }
    }
}
