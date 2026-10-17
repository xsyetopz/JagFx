package io.github.xsyetopz.jagfx.core.vorbis

/**
 * The Vorbis audio of a sample: a setup header without packet framing, and its audio packets.
 *
 * The class keeps copies of [setup] and [packets], so later changes to the arrays do not reach it.
 */
public class VorbisStream(
    setup: ByteArray,
    packets: List<ByteArray>,
) {
    private val setupBytes = setup.copyOf()
    private val packetBytes = packets.map { it.copyOf() }

    /** A copy of the setup header bytes. */
    public val setup: ByteArray get() = setupBytes.copyOf()

    /** Copies of the audio packets. */
    public val packets: List<ByteArray> get() = packetBytes.map { it.copyOf() }

    /** Number of audio packets. */
    public val packetCount: Int get() = packetBytes.size

    /** Decodes the packets as the game client does and keeps the first [sampleCount] samples. */
    internal fun decodePcm(sampleCount: Int): ShortArray =
        VorbisDecoder(VorbisSetup.parse(setupBytes)).decode(packetBytes, sampleCount)

    override fun equals(other: Any?): Boolean =
        other is VorbisStream &&
            setupBytes.contentEquals(other.setupBytes) &&
            packetBytes.size == other.packetBytes.size &&
            packetBytes.indices.all { packetBytes[it].contentEquals(other.packetBytes[it]) }

    override fun hashCode(): Int {
        var result = setupBytes.contentHashCode()
        for (packet in packetBytes) result = HASH_PRIME * result + packet.contentHashCode()
        return result
    }

    override fun toString(): String =
        "VorbisStream(setup=${setupBytes.size} bytes, packets=${packetBytes.size})"
}

/**
 * A recorded sound-effect sample: file 1 of a sound-effect cache group.
 *
 * The sample is mono Vorbis audio in a Jagex container, not an Ogg stream.
 * The game client decodes the packets of [audio] and keeps the first [sampleCount] samples.
 *
 * @property sampleRate Samples per second.
 * @property sampleCount Number of samples that the client keeps.
 * @property loopBegin First sample of the loop region.
 * @property loopEnd Sample after the loop region.
 * @property pingPong `true` when the loop plays forward and then backward.
 * @property audio The setup header and audio packets.
 */
public class JagVorbis(
    public val sampleRate: Int,
    public val sampleCount: Int,
    public val loopBegin: Int,
    public val loopEnd: Int,
    public val pingPong: Boolean = false,
    public val audio: VorbisStream,
) {
    init {
        require(sampleCount >= 0) { "sampleCount must be >= 0, was $sampleCount" }
        require(loopEnd >= 0) { "loopEnd must be >= 0, was $loopEnd" }
    }

    /** A copy of the setup header bytes. */
    public val setup: ByteArray get() = audio.setup

    /** Copies of the audio packets. */
    public val packets: List<ByteArray> get() = audio.packets

    /** Number of audio packets. */
    public val packetCount: Int get() = audio.packetCount

    /** Returns this sample with the given header fields and the same audio. */
    public fun copy(
        sampleRate: Int = this.sampleRate,
        sampleCount: Int = this.sampleCount,
        loopBegin: Int = this.loopBegin,
        loopEnd: Int = this.loopEnd,
        pingPong: Boolean = this.pingPong,
    ): JagVorbis = JagVorbis(sampleRate, sampleCount, loopBegin, loopEnd, pingPong, audio)

    /** Decodes the audio as the game client does: mono signed 16-bit samples. */
    public fun decodePcm(): ShortArray = audio.decodePcm(sampleCount)

    override fun equals(other: Any?): Boolean =
        other is JagVorbis &&
            sampleRate == other.sampleRate &&
            sampleCount == other.sampleCount &&
            loopBegin == other.loopBegin &&
            loopEnd == other.loopEnd &&
            pingPong == other.pingPong &&
            audio == other.audio

    override fun hashCode(): Int {
        var result = sampleRate
        result = HASH_PRIME * result + sampleCount
        result = HASH_PRIME * result + loopBegin
        result = HASH_PRIME * result + loopEnd
        result = HASH_PRIME * result + pingPong.hashCode()
        result = HASH_PRIME * result + audio.hashCode()
        return result
    }

    override fun toString(): String =
        "JagVorbis(sampleRate=$sampleRate, sampleCount=$sampleCount, loopBegin=$loopBegin, " +
            "loopEnd=$loopEnd, pingPong=$pingPong, setup=${audio.setup.size} bytes, " +
            "packets=${audio.packetCount})"
}

private const val HASH_PRIME = 31

/** The input is not a valid `.jagvorbis` file. [offset] is the byte offset of the error. */
public class JagVorbisFormatException(
    public val offset: Int,
    detail: String,
) : Exception("$detail (at byte offset $offset)")

/**
 * Binary codec of `.jagvorbis` files.
 *
 * Layout, big-endian:
 * `u32 setupLength`, the setup header, `s32 sampleRate`, `s32 sampleCount`, `s32 loopBegin`,
 * `s32 loopEnd` (bitwise NOT of the end when the loop is ping-pong), `s32 packetCount`,
 * then each packet as a length in 255-continued bytes followed by its bytes.
 */
public object JagVorbisCodec {
    private const val LACING_CONTINUE = 255
    private const val BYTE_MASK = 0xFF

    /** Decodes [bytes]. Encoding the result gives [bytes] again. */
    public fun decode(bytes: ByteArray): JagVorbis {
        val reader = ByteReader(bytes)
        val setupLength = reader.s32()
        formatCheck(setupLength >= 0, 0) { "setup length $setupLength is negative" }
        val setup = reader.bytes(setupLength)
        val sampleRate = reader.s32()
        val sampleCountOffset = reader.position
        val sampleCount = reader.s32()
        val loopBegin = reader.s32()
        val storedLoopEnd = reader.s32()
        val packetCountOffset = reader.position
        val packetCount = reader.s32()
        formatCheck(sampleCount >= 0, sampleCountOffset) { "sample count $sampleCount is negative" }
        formatCheck(packetCount >= 0, packetCountOffset) { "packet count $packetCount is negative" }
        val packets =
            List(packetCount) {
                var length = 0
                do {
                    val lace = reader.u8()
                    length += lace
                } while (lace == LACING_CONTINUE)
                reader.bytes(length)
            }
        formatCheck(reader.position == bytes.size, reader.position) {
            "${bytes.size - reader.position} bytes follow the last packet"
        }
        val pingPong = storedLoopEnd < 0
        val loopEnd = if (pingPong) storedLoopEnd.inv() else storedLoopEnd
        return JagVorbis(
            sampleRate,
            sampleCount,
            loopBegin,
            loopEnd,
            pingPong,
            VorbisStream(setup, packets),
        )
    }

    /** Encodes [sample] in the layout that [decode] reads. */
    public fun encode(sample: JagVorbis): ByteArray {
        val setup = sample.setup
        val packets = sample.packets
        val lacingSize = packets.sumOf { it.size / LACING_CONTINUE + 1 }
        val size =
            Int.SIZE_BYTES * HEADER_INTS + setup.size + lacingSize + packets.sumOf { it.size }
        val out = ByteArray(size)
        var pos = 0

        fun s32(value: Int) {
            for (shift in Int.SIZE_BITS - Byte.SIZE_BITS downTo 0 step Byte.SIZE_BITS) {
                out[pos++] = (value shr shift and BYTE_MASK).toByte()
            }
        }

        s32(setup.size)
        setup.copyInto(out, pos)
        pos += setup.size
        s32(sample.sampleRate)
        s32(sample.sampleCount)
        s32(sample.loopBegin)
        s32(if (sample.pingPong) sample.loopEnd.inv() else sample.loopEnd)
        s32(packets.size)
        for (packet in packets) {
            repeat(packet.size / LACING_CONTINUE) { out[pos++] = LACING_CONTINUE.toByte() }
            out[pos++] = (packet.size % LACING_CONTINUE).toByte()
            packet.copyInto(out, pos)
            pos += packet.size
        }
        return out
    }

    /** `setupLength`, four header fields, and `packetCount`. */
    private const val HEADER_INTS = 6

    private class ByteReader(
        private val bytes: ByteArray,
    ) {
        var position = 0
            private set

        fun u8(): Int {
            require(1)
            return bytes[position++].toInt() and BYTE_MASK
        }

        fun s32(): Int {
            require(Int.SIZE_BYTES)
            var value = 0
            repeat(Int.SIZE_BYTES) {
                value = (value shl Byte.SIZE_BITS) or (bytes[position++].toInt() and BYTE_MASK)
            }
            return value
        }

        fun bytes(count: Int): ByteArray {
            require(count)
            return bytes.copyOfRange(position, position + count).also { position += count }
        }

        private fun require(count: Int) {
            formatCheck(count <= bytes.size - position, position) {
                "file ends ${count - (bytes.size - position)} bytes early"
            }
        }
    }

    private inline fun formatCheck(
        condition: Boolean,
        offset: Int,
        detail: () -> String,
    ) {
        if (!condition) throw JagVorbisFormatException(offset, detail())
    }
}
