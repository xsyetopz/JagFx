package io.github.xsyetopz.jagfx.bindings

import io.github.xsyetopz.jagfx.core.engine.Wav
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec

/** Platform-independent bodies of the exported `.jagvorbis` functions. */
internal object SampleBindings {
    /** Decodes `.jagvorbis` [sample] bytes to mono signed 16-bit little-endian PCM. */
    fun samplePcm16Le(sample: ByteArray): ByteArray =
        pcm16Le(JagVorbisCodec.decode(sample).decodePcm())

    /** Decodes `.jagvorbis` [sample] bytes to a 16-bit WAV file at the sample's rate. */
    fun sampleToWav(sample: ByteArray): ByteArray {
        val decoded = JagVorbisCodec.decode(sample)
        return Wav.encode(decoded.decodePcm(), sampleRate = decoded.sampleRate)
    }

    /**
     * The header fields of `.jagvorbis` [sample] bytes as a JSON object with `sampleRate`,
     * `sampleCount`, `loopBegin`, `loopEnd`, `pingPong`, and `packetCount`.
     */
    fun sampleInfo(sample: ByteArray): String {
        val decoded = JagVorbisCodec.decode(sample)
        return "{\"sampleRate\":${decoded.sampleRate},\"sampleCount\":${decoded.sampleCount}," +
            "\"loopBegin\":${decoded.loopBegin},\"loopEnd\":${decoded.loopEnd}," +
            "\"pingPong\":${decoded.pingPong},\"packetCount\":${decoded.packetCount}}"
    }
}
