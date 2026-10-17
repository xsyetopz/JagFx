package io.github.xsyetopz.jagfx.core.format

/**
 * Cursor over the input of [SynthDecoder], with the truncation flag and the decode warnings.
 *
 * Reads past the end of [bytes] give `0` and set [truncated]; the cursor still advances,
 * so [remaining] can become negative and every later presence check fails.
 *
 * Every read reports a [TracedField] to [trace], when set. Field paths are built only then.
 */
internal class SynthReader(
    private val bytes: ByteArray,
    private val trace: ((TracedField) -> Unit)?,
) {
    var cursor = 0
        private set
    var truncated = false
    val warnings = ArrayList<String>()

    val remaining: Int get() = bytes.size - cursor

    fun startsWithGitLfsPointer(): Boolean {
        if (bytes.size < GIT_LFS_POINTER_PREFIX.length) return false
        return GIT_LFS_POINTER_PREFIX.indices.all {
            bytes[it] == GIT_LFS_POINTER_PREFIX[it].code.toByte()
        }
    }

    fun peek(k: Int): Int =
        if (cursor + k < bytes.size) bytes[cursor + k].toInt() and LOW_BYTE_MASK else 0

    inline fun u8(path: () -> String): Int = traced(path) { fixed(1) }

    inline fun u16(path: () -> String): Int = traced(path) { fixed(Short.SIZE_BYTES) }

    inline fun s32(path: () -> String): Int = traced(path) { fixed(Int.SIZE_BYTES) }

    inline fun usmart16(path: () -> String): Int = traced(path) { smart(USMART16_BIAS, 0) }

    inline fun smart16(path: () -> String): Int =
        traced(path) { smart(SMART16_BIAS, SMART16_ONE_BYTE_BIAS) }

    inline fun traced(
        path: () -> String,
        read: () -> Int,
    ): Int {
        val offset = cursor
        val value = read()
        trace?.invoke(TracedField(offset, cursor - offset, path(), value))
        return value
    }

    fun fixed(width: Int): Int {
        if (cursor + width > bytes.size) {
            truncated = true
            cursor += width
            return 0
        }
        var value = 0
        repeat(width) {
            value = (value shl Byte.SIZE_BITS) or (bytes[cursor + it].toInt() and LOW_BYTE_MASK)
        }
        cursor += width
        return value
    }

    /** Spec §6.2: a truncated two-byte form gives `0`, with no bias. */
    fun smart(
        twoByteBias: Int,
        oneByteBias: Int,
    ): Int =
        when {
            cursor >= bytes.size -> 0
            peek(0) < SMART_ONE_BYTE_LIMIT -> fixed(1) - oneByteBias
            cursor + 2 > bytes.size -> fixed(Short.SIZE_BYTES)
            else -> fixed(Short.SIZE_BYTES) - twoByteBias
        }
}
