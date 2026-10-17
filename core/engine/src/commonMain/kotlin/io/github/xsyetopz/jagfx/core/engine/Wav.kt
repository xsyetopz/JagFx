package io.github.xsyetopz.jagfx.core.engine

/** Sample encoding of a WAV file (spec §10.1). */
public enum class PcmEncoding(
    public val bitsPerSample: Int,
) {
    /** Unsigned 8-bit samples: `(s >> 8) + 128`. */
    Pcm8(bitsPerSample = 8),

    /** Signed 16-bit little-endian samples. */
    Pcm16(bitsPerSample = 16),
}

/** WAV container for rendered samples (spec §10.2). */
public object Wav {
    private const val HEADER_SIZE = 44
    private const val RIFF_SIZE_BASE = 36
    private const val FMT_CHUNK_SIZE = 16
    private const val FORMAT_PCM = 1
    private const val CHANNELS = 1
    private const val PCM8_OFFSET = 128
    private const val BYTE_BITS = 8
    private const val BYTE_MASK = 0xFF

    /** Encodes mono [samples] at [sampleRate] Hz as a WAV file. */
    public fun encode(
        samples: ShortArray,
        encoding: PcmEncoding = PcmEncoding.Pcm16,
        sampleRate: Int = Renderer.SAMPLE_RATE,
    ): ByteArray {
        val bytesPerSample = encoding.bitsPerSample / BYTE_BITS
        val dataSize = samples.size * bytesPerSample
        val out = ByteArray(HEADER_SIZE + dataSize)
        var pos = 0

        fun ascii(text: String) {
            for (c in text) out[pos++] = c.code.toByte()
        }

        fun le(
            value: Int,
            bytes: Int,
        ) {
            for (i in 0 until bytes) out[pos++] = (value shr (BYTE_BITS * i) and BYTE_MASK).toByte()
        }

        ascii("RIFF")
        le(RIFF_SIZE_BASE + dataSize, Int.SIZE_BYTES)
        ascii("WAVE")
        ascii("fmt ")
        le(FMT_CHUNK_SIZE, Int.SIZE_BYTES)
        le(FORMAT_PCM, Short.SIZE_BYTES)
        le(CHANNELS, Short.SIZE_BYTES)
        le(sampleRate, Int.SIZE_BYTES)
        le(sampleRate * bytesPerSample, Int.SIZE_BYTES)
        le(bytesPerSample, Short.SIZE_BYTES)
        le(encoding.bitsPerSample, Short.SIZE_BYTES)
        ascii("data")
        le(dataSize, Int.SIZE_BYTES)
        when (encoding) {
            PcmEncoding.Pcm8 -> for (s in samples) {
                out[pos++] =
                    ((s.toInt() shr BYTE_BITS) + PCM8_OFFSET).toByte()
            }

            PcmEncoding.Pcm16 -> for (s in samples) le(s.toInt(), Short.SIZE_BYTES)
        }
        return out
    }
}
