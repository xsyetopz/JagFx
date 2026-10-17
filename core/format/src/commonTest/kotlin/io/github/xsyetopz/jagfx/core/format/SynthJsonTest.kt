package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Echo
import io.github.xsyetopz.jagfx.core.model.Gate
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SynthJsonTest {
    @Test
    fun everyReferenceJsonFileIsTheEncodingOfItsSynthFile() {
        assertEquals(ReferenceSynths.hex.keys, ReferenceJson.hex.keys)
        for (name in ReferenceSynths.hex.keys) {
            val patch = SynthCodec.decode(referenceSynth(name)).patch
            assertEquals(referenceJson(name).removeSuffix("\n"), SynthJson.encode(patch), name)
        }
    }

    @Test
    fun everyReferenceJsonFileDecodesToTheBytesOfItsSynthFile() {
        for (name in ReferenceJson.hex.keys) {
            val patch = SynthJson.decode(referenceJson(name))
            assertTrue(SynthCodec.encode(patch).contentEquals(referenceSynth(name)), name)
        }
    }

    @Test
    fun referenceVoicesAndFiltersRoundTripAsJson() {
        for (name in ReferenceSynths.hex.keys) {
            for (voice in SynthCodec
                .decode(referenceSynth(name))
                .patch.voices
                .filterNotNull()) {
                assertEquals(voice, SynthJson.decodeVoice(SynthJson.encodeVoice(voice)), name)
                assertEquals(
                    voice.filter,
                    SynthJson.decodeFilter(SynthJson.encodeFilter(voice.filter)),
                    name,
                )
            }
        }
    }

    @Test
    fun noFilterIsJsonNull() {
        assertEquals("null", SynthJson.encodeFilter(null))
        assertEquals(null, SynthJson.decodeFilter("null"))
    }

    @Test
    fun voiceJsonIsTheVoiceObjectOfPatchJson() {
        val patch = SynthCodec.decode(referenceSynth(ReferenceSynths.hex.keys.first())).patch
        val voice = patch.voices.filterNotNull().first()

        assertContains(
            SynthJson.encode(patch),
            SynthJson.encodeVoice(voice).replace("\n", "\n    "),
        )
    }

    @Test
    fun patchWithEveryOptionalFieldRoundTrips() {
        val lfo =
            Lfo(
                rate = envelope(Waveform.Square, 1, 2, listOf(Segment(3, 4))),
                depth = envelope(Waveform.Off, -5, 6),
            )
        val full =
            voice(
                partials = List(5) { partial(it + 1, pitchOffset = -16384 + it, delay = it * 7) },
                echo = Echo(Milliseconds(30), Percent(40)),
                filter =
                    filter(
                        feedforward = listOf(pole(1, 2, endPhase = 3), pole(4, 5)),
                        feedback = listOf(pole(6, 7, endMagnitude = 8)),
                        unityGain0 = 9,
                        unityGain1 = 10,
                        modulationEnvelope = listOf(Segment(0, 1), Segment(65535, 2)),
                    ),
            ).copy(
                pitchLfo = lfo,
                amplitudeLfo = Lfo(envelope(Waveform(245)), envelope(Waveform.Off)),
                gate = Gate(envelope(Waveform.Noise), envelope(Waveform.Saw)),
                offset = Milliseconds(12),
            )
        val patch =
            patchOf(null, full, voice(), loop = Loop(Milliseconds(100), Milliseconds(200)))

        assertEquals(patch, SynthJson.decode(SynthJson.encode(patch)))
    }

    @Test
    fun encoderOmitsDefaultsAndTrailingEmptySlots() {
        val json = SynthJson.encode(patchOf(null, voice(partials = emptyList())))

        assertEquals(2, Regex("\"frequencyEnvelope\"|null").findAll(json).count())
        val omitted =
            listOf("loop", "pitchLfo", "gapOffEnvelope", "partials", "echo", "offsetMs", "filter")
        for (key in omitted) {
            assertTrue("\"$key\"" !in json, key)
        }
        assertTrue('\r' !in json)
        assertEquals(Patch.Empty, SynthJson.decode(SynthJson.encode(Patch.Empty)))
    }

    @Test
    fun decoderAppliesDefaultsToMissingFields() {
        val patch =
            SynthJson.decode(
                """
                {"voices": [{
                  "frequencyEnvelope": {"waveform": "sine", "startValue": 32768, "endValue": 32768,
                    "segments": []},
                  "amplitudeEnvelope": {"waveform": "off", "startValue": 0, "endValue": 65535,
                    "segments": [{"duration": 16384, "targetLevel": 65535}]},
                  "durationMs": 500,
                  "partials": [{"amplitude": 100, "pitchOffsetSemitones": -3}]
                }], "loop": null}
                """,
            )

        val expected =
            voice(
                frequency = envelope(Waveform.Sine, 32768, 32768),
                amplitude = envelope(Waveform.Off, 0, 65535, listOf(Segment(16384, 65535))),
                partials = listOf(partial(pitchOffset = -3)),
            )
        assertEquals(patchOf(expected), patch)
    }

    @Test
    fun decoderRejectsTextOutsideTheSchema() {
        val cases =
            mapOf(
                "not json" to "Unexpected JSON token",
                """{"voices": [], "extra": 1}""" to "extra",
                """{"loop": null}""" to "voices",
                """{"voices": [${List(11) { "null" }.joinToString()}]}""" to "voices",
                patchWithVoice("\"sine\"" to "\"Sine\"") to "voices[0].frequencyEnvelope.waveform",
                patchWithVoice("\"sine\"" to "\"undefined4\"") to "frequencyEnvelope.waveform",
                patchWithVoice("\"sine\"" to "\"undefined256\"") to "frequencyEnvelope.waveform",
                patchWithVoice("\"sine\"" to "\"undefined05\"") to "frequencyEnvelope.waveform",
                """{"voices": [], "appendedData": "AQI="}""" to "appendedData",
                patchWithVoice("\"durationMs\": 500" to "\"durationMs\": 65536") to "voices[0]",
                patchWithVoice("\"amplitude\": 100" to "\"amplitude\": 0") to
                    "voices[0].partials[0]",
                patchWithVoiceField("gapOnEnvelope", SINE) to "gapOffEnvelope and gapOnEnvelope",
                patchWithVoiceField("filter", filterJson(phaseBaseline = "[]")) to
                    "voices[0].filter.feedforward.polePhase.baseline",
                patchWithVoiceField("filter", filterJson(modulationEnvelope = SINE)) to
                    "voices[0].filter.modulationEnvelope",
                """{"voices": [], "loop": {"beginMs": 0, "endMs": 4294967296}}""" to "endMs",
            )
        for ((text, expected) in cases) {
            val error = assertFailsWith<SynthJsonException>(text) { SynthJson.decode(text) }
            assertContains(error.message.orEmpty(), expected, ignoreCase = true, message = text)
        }
    }

    /** A one-voice patch whose voice JSON has [edit] applied. */
    private fun patchWithVoice(edit: Pair<String, String>): String =
        SynthJson.encode(patchOf(voice())).replace(edit.first, edit.second)

    /** A one-voice patch whose voice JSON has the extra field [name] with [value]. */
    private fun patchWithVoiceField(
        name: String,
        value: String,
    ): String = patchWithVoice("\"durationMs\"" to "\"$name\": $value, \"durationMs\"")

    private fun filterJson(
        phaseBaseline: String = "[1]",
        modulationEnvelope: String = "null",
    ): String {
        val phase = """{"feedforward": {"baseline": $phaseBaseline, "modulated": [1]},
            "feedback": $NO_POLES}"""
        val magnitude = """{"feedforward": {"baseline": [1], "modulated": [1]},
            "feedback": $NO_POLES}"""
        return """{"poleCounts": [1, 0], "unityGain": [0, 1],
            "polePhase": $phase, "poleMagnitude": $magnitude,
            "modulationEnvelope": $modulationEnvelope}"""
    }

    private companion object {
        const val NO_POLES = """{"baseline": [], "modulated": []}"""
        const val SINE = """{"waveform": "sine", "startValue": 0, "endValue": 0, "segments": []}"""
    }
}

internal fun referenceJson(name: String): String =
    hexToBytes(ReferenceJson.hex.getValue(name)).decodeToString()
