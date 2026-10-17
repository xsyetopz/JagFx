package io.github.xsyetopz.jagfx.bindings

import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.engine.Wav
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.format.SynthJson
import io.github.xsyetopz.jagfx.core.model.SynthLimits

/** Value of `voiceFilter` that renders every voice slot. */
internal const val ALL_VOICES = -1

private const val BYTE_BITS = 8

/** Platform-independent bodies of the exported JavaScript functions. */
internal object Bindings {
    /**
     * Renders `.synth` [synth] bytes to mono signed 16-bit little-endian PCM at
     * [Renderer.SAMPLE_RATE] Hz.
     *
     * @param loops Number of times the loop region plays; at least `1`.
     * @param voiceFilter The one voice slot to render, or [ALL_VOICES].
     */
    fun renderPcm16Le(
        synth: ByteArray,
        loops: Int,
        voiceFilter: Int,
    ): ByteArray = pcm16Le(render(synth, loops, voiceFilter))

    /** Renders `.synth` [synth] bytes to a 16-bit WAV file. */
    fun renderWav(
        synth: ByteArray,
        loops: Int,
    ): ByteArray = Wav.encode(render(synth, loops, ALL_VOICES))

    /** Converts `.synth` bytes to the JSON interchange format. */
    fun synthToJson(synth: ByteArray): String = SynthJson.encode(SynthCodec.decode(synth).patch)

    /** Converts the JSON interchange format to `.synth` bytes. */
    fun jsonToSynth(json: String): ByteArray = SynthCodec.encode(SynthJson.decode(json))

    /** Converts `.tone` bytes to the JSON form of one voice. */
    fun toneToJson(tone: ByteArray): String =
        SynthJson.encodeVoice(SynthCodec.decodeTone(tone).voice)

    /** Converts the JSON form of one voice to `.tone` bytes. */
    fun jsonToTone(json: String): ByteArray = SynthCodec.encodeTone(SynthJson.decodeVoice(json))

    /** Converts `.filter` bytes to the JSON form of a filter, `null` for no filter. */
    fun filterToJson(filter: ByteArray): String =
        SynthJson.encodeFilter(SynthCodec.decodeFilter(filter).filter)

    /** Converts the JSON form of a filter, or `null`, to `.filter` bytes. */
    fun jsonToFilter(json: String): ByteArray =
        SynthCodec.encodeFilter(SynthJson.decodeFilter(json))

    private fun render(
        synth: ByteArray,
        loops: Int,
        voiceFilter: Int,
    ): ShortArray {
        require(loops >= 1) { "loops must be a positive integer, was $loops" }
        require(voiceFilter == ALL_VOICES || voiceFilter in 0 until SynthLimits.VOICE_SLOTS) {
            "voiceFilter must be $ALL_VOICES or in 0 until ${SynthLimits.VOICE_SLOTS}, was $voiceFilter"
        }
        val patch = SynthCodec.decode(synth).patch
        return Renderer.render(patch, loops, voiceFilter.takeIf { it != ALL_VOICES })
    }
}

/** [samples] as signed 16-bit little-endian bytes. */
internal fun pcm16Le(samples: ShortArray): ByteArray {
    val out = ByteArray(samples.size * Short.SIZE_BYTES)
    for (i in samples.indices) {
        val sample = samples[i].toInt()
        out[i * Short.SIZE_BYTES] = sample.toByte()
        out[i * Short.SIZE_BYTES + 1] = (sample shr BYTE_BITS).toByte()
    }
    return out
}
