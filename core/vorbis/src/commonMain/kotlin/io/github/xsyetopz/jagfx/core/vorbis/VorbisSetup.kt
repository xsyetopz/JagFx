package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.math.pow

private const val BLOCKSIZE_BITS = 4
private const val COUNT8_BITS = 8
private const val COUNT6_BITS = 6
private const val TYPE_BITS = 16
private const val FLOOR_TYPE_1 = 1
private const val MODE_FIELD_BITS = 16
private const val MAPPING_INDEX_BITS = 8

/**
 * A parsed Vorbis setup header as the game client stores it: two blocksize exponents,
 * then the codebook, time, floor, residue, mapping, and mode configurations,
 * with no identification header and no packet framing.
 * Only floor type 1 is supported, as in the game client.
 */
internal class VorbisSetup(
    val shortBlocksize: Int,
    val longBlocksize: Int,
    val codebooks: List<Codebook>,
    val floors: List<Floor1>,
    val residues: List<Residue>,
    val modes: List<Mode>,
) {
    val shortTables = TrigTables(shortBlocksize)
    val longTables = TrigTables(longBlocksize)

    class Mode(
        val longBlock: Boolean,
        val mapping: Mapping,
    )

    companion object {
        fun parse(bytes: ByteArray): VorbisSetup {
            val reader = BitReader(bytes)
            val shortBlocksize = 1 shl reader.bits(BLOCKSIZE_BITS)
            val longBlocksize = 1 shl reader.bits(BLOCKSIZE_BITS)
            val codebooks = List(reader.bits(COUNT8_BITS) + 1) { Codebook.parse(reader) }
            repeat(reader.bits(COUNT6_BITS) + 1) { reader.bits(TYPE_BITS) }
            val floors =
                List(reader.bits(COUNT6_BITS) + 1) {
                    val type = reader.bits(TYPE_BITS)
                    check(type == FLOOR_TYPE_1) { "floor type $type is not supported" }
                    Floor1.parse(reader)
                }
            val residues = List(reader.bits(COUNT6_BITS) + 1) { Residue.parse(reader) }
            val mappings = List(reader.bits(COUNT6_BITS) + 1) { Mapping.parse(reader) }
            val modes =
                List(reader.bits(COUNT6_BITS) + 1) {
                    val longBlock = reader.bit() != 0
                    reader.bits(MODE_FIELD_BITS)
                    reader.bits(MODE_FIELD_BITS)
                    Mode(longBlock, mappings[reader.bits(MAPPING_INDEX_BITS)])
                }
            return VorbisSetup(
                shortBlocksize,
                longBlocksize,
                codebooks,
                floors,
                residues,
                modes,
            )
        }
    }
}

/** Reads a packet from its first byte, low bit first, as Vorbis packs its fields. */
internal class BitReader(
    private val bytes: ByteArray,
) {
    private var bitPosition = 0L

    fun bit(): Int {
        val index = (bitPosition ushr BYTE_SHIFT).toInt()
        check(index < bytes.size) { "Vorbis data ends at byte ${bytes.size} before a field" }
        val value = (bytes[index].toInt() ushr (bitPosition and BIT_INDEX_MASK).toInt()) and 1
        bitPosition++
        return value
    }

    fun bits(count: Int): Int {
        var value = 0
        for (i in 0 until count) value = value or (bit() shl i)
        return value
    }

    private companion object {
        const val BYTE_SHIFT = 3
        const val BIT_INDEX_MASK = 7L
    }
}

/** Number of bits in [value]: `0` for zero and negative values. */
internal fun ilog(value: Int): Int =
    if (value <=
        0
    ) {
        0
    } else {
        Int.SIZE_BITS - value.countLeadingZeroBits()
    }

private const val FLOAT_MANTISSA_MASK = 0x1FFFFF
private const val FLOAT_EXPONENT_MASK = 0x7FE00000
private const val FLOAT_EXPONENT_SHIFT = 21
private const val FLOAT_EXPONENT_BIAS = 788

/** Vorbis `float32_unpack`. */
internal fun float32Unpack(packed: Int): Double {
    val magnitude = packed and FLOAT_MANTISSA_MASK
    val mantissa = if (packed < 0) -magnitude else magnitude
    val exponent = (packed and FLOAT_EXPONENT_MASK) ushr FLOAT_EXPONENT_SHIFT
    return fround(mantissa * 2.0.pow(exponent - FLOAT_EXPONENT_BIAS))
}
