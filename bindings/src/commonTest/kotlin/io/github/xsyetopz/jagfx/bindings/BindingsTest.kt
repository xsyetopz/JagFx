package io.github.xsyetopz.jagfx.bindings

import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.engine.Wav
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.format.SynthJson
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BindingsTest {
    private val synth = Bindings.jsonToSynth(toneJson)
    private val patch = SynthCodec.decode(synth).patch

    @Test
    fun jsonRoundTripsThroughSynthBytes() {
        assertEquals(SynthJson.decode(toneJson), patch)
        assertContentEquals(synth, Bindings.jsonToSynth(Bindings.synthToJson(synth)))
    }

    @Test
    fun pcmIsLittleEndianRendererOutput() {
        val samples = Renderer.render(patch, 2, null)
        val pcm = Bindings.renderPcm16Le(synth, 2, ALL_VOICES)

        assertTrue(samples.isNotEmpty())
        assertEquals(samples.size * Short.SIZE_BYTES, pcm.size)
        for (i in samples.indices) {
            val sample = (pcm[2 * i].toInt() and 0xFF) or (pcm[2 * i + 1].toInt() shl 8)
            assertEquals(samples[i], sample.toShort(), "sample $i")
        }
    }

    @Test
    fun voiceFilterRendersOneVoice() {
        val pcm = Bindings.renderPcm16Le(synth, 1, 0)

        assertEquals(Renderer.render(patch, 1, 0).size * Short.SIZE_BYTES, pcm.size)
    }

    @Test
    fun wavWrapsAllVoices() {
        assertContentEquals(
            Wav.encode(Renderer.render(patch, 3, null)),
            Bindings.renderWav(synth, 3),
        )
    }

    @Test
    fun invalidArgumentsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Bindings.renderPcm16Le(synth, 0, ALL_VOICES) }
        assertFailsWith<IllegalArgumentException> { Bindings.renderWav(synth, 0) }
        assertFailsWith<IllegalArgumentException> { Bindings.renderPcm16Le(synth, 1, -2) }
        assertFailsWith<IllegalArgumentException> { Bindings.renderPcm16Le(synth, 1, 10) }
    }

    @Test
    fun toneAndFilterRoundTripThroughJson() {
        val voice = patch.voices.filterNotNull().first()
        val tone = SynthCodec.encodeTone(voice)
        val filter = SynthCodec.encodeFilter(voice.filter)

        assertEquals(SynthJson.encodeVoice(voice), Bindings.toneToJson(tone))
        assertContentEquals(tone, Bindings.jsonToTone(Bindings.toneToJson(tone)))
        assertEquals("null", Bindings.filterToJson(filter))
        assertContentEquals(filter, Bindings.jsonToFilter("null"))
    }

    @Test
    fun samplesDecodeToPcmAndWavAtTheirRate() {
        for ((name, hex) in ReferenceSamples.hex) {
            val bytes = hexToBytes(hex)
            val sample = JagVorbisCodec.decode(bytes)
            val pcm = sample.decodePcm()

            assertContentEquals(
                Wav.encode(pcm, sampleRate = sample.sampleRate),
                SampleBindings.sampleToWav(bytes),
                name,
            )
            val le = SampleBindings.samplePcm16Le(bytes)
            assertEquals(pcm.size * Short.SIZE_BYTES, le.size, name)
            assertEquals(
                pcm.last(),
                (
                    (le[le.size - 2].toInt() and 0xFF) or
                        (le.last().toInt() shl 8)
                ).toShort(),
            )
            assertEquals(
                "{\"sampleRate\":${sample.sampleRate},\"sampleCount\":${sample.sampleCount}," +
                    "\"loopBegin\":${sample.loopBegin},\"loopEnd\":${sample.loopEnd}," +
                    "\"pingPong\":${sample.pingPong},\"packetCount\":${sample.packetCount}}",
                SampleBindings.sampleInfo(bytes),
                name,
            )
        }
    }
}

private fun hexToBytes(hex: String): ByteArray =
    ByteArray(hex.length / 2) {
        hex
            .substring(
                2 * it,
                2 * it + 2,
            ).toInt(16)
            .toByte()
    }

private val toneJson =
    """
    {
      "voices": [
        {
          "frequencyEnvelope": {
            "waveform": "sine",
            "startValue": 32768,
            "endValue": 32768,
            "segments": []
          },
          "amplitudeEnvelope": {
            "waveform": "off",
            "startValue": 0,
            "endValue": 65535,
            "segments": [{ "duration": 65535, "targetLevel": 0 }]
          },
          "durationMs": 50
        }
      ]
    }
    """.trimIndent()
