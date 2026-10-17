package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ReferenceSynthTest {
    @Test
    fun everyReferenceFileIsAConformingFile() {
        assertEquals(15, ReferenceSynths.hex.size)
        val mismatches =
            ReferenceSynths.hex.keys.mapNotNull { name ->
                val bytes = referenceSynth(name)
                val decoded = SynthCodec.decode(bytes)
                assertEquals(emptyList(), decoded.warnings, name)
                val encoded = SynthCodec.encode(decoded.patch)
                if (encoded.contentEquals(
                        bytes,
                    )
                ) {
                    null
                } else {
                    "$name: ${bytes.size} -> ${encoded.size} bytes"
                }
            }
        assertEquals(emptyList(), mismatches)
    }

    @Test
    fun cowDeathDecodesToOneSineVoice() {
        val patch = SynthCodec.decode(referenceSynth("cow_death")).patch

        assertEquals(1, patch.voices.count { it != null })
        val voice = patch.voices[0] ?: fail("slot 0 is empty")
        assertEquals(Waveform.Sine, voice.frequencyEnvelope.waveform)
        assertTrue(voice.amplitudeEnvelope.segments.isNotEmpty())
        assertTrue(voice.partials.isNotEmpty())
    }

    @Test
    fun traceCoversEveryByteOfEveryReferenceFileInOrder() {
        for ((name, hex) in ReferenceSynths.hex) {
            val bytes = hexToBytes(hex)
            val fields = ArrayList<TracedField>()
            val traced = SynthCodec.decode(bytes) { fields += it }
            assertEquals(SynthCodec.decode(bytes), traced, name)
            var end = 0
            for (field in fields) {
                assertEquals(end, field.offset, "$name: ${field.path}")
                end += field.size
            }
            assertEquals(bytes.size, end, name)
        }
    }

    @Test
    fun cowDeathTraceNamesEveryField() {
        val fields = ArrayList<TracedField>()
        SynthCodec.decode(referenceSynth("cow_death")) { fields += it }

        assertEquals(
            TracedField(1, 4, "voices[0].frequencyEnvelope.startValue", 30),
            fields[1],
        )
        assertEquals(
            TracedField(0x64, 1, "voices[0].amplitudeLfo", 0),
            fields.first {
                it.offset ==
                    0x64
            },
        )
        assertEquals(
            listOf("voices[0].echo.delay", "voices[0].echo.feedback"),
            fields.filter { ".echo." in it.path }.map { it.path },
        )
        assertEquals(TracedField(0x75, 1, "voices[1]", 0), fields.first { it.offset == 0x75 })
        assertEquals(TracedField(0x80, 2, "loop.end", 0), fields.last())
    }

    /** Spec §6.9 and §7.3: any decodable input gives a valid patch that round-trips. */
    @Test
    fun everyPrefixOfEveryReferenceFileDecodesToAValidPatch() {
        for ((name, hex) in ReferenceSynths.hex) {
            val bytes = hexToBytes(hex)
            for (length in 0..bytes.size) {
                assertDecodedPatchRoundTrips(bytes.copyOf(length), "$name[0..$length)")
            }
        }
    }

    @Test
    fun mutatedReferenceFilesDecodeToValidPatchesOrFormatErrors() {
        val random = Random(SEED)
        for ((name, hex) in ReferenceSynths.hex) {
            val original = hexToBytes(hex)
            repeat(MUTATIONS_PER_FILE) { iteration ->
                val bytes = original.copyOf()
                repeat(random.nextInt(1, 4)) {
                    bytes[random.nextInt(bytes.size)] =
                        random.nextInt(256).toByte()
                }
                try {
                    assertDecodedPatchRoundTrips(bytes, "$name mutation $iteration")
                } catch (_: SynthFormatException) {
                    // A defined decoder error; the input has no patch.
                }
            }
        }
    }

    private fun assertDecodedPatchRoundTrips(
        bytes: ByteArray,
        label: String,
    ) {
        val patch = SynthCodec.decode(bytes).patch
        val again = roundTrip(patch)
        assertEquals(patch, again.patch, label)
        assertEquals(emptyList(), again.warnings, label)
    }

    private companion object {
        const val SEED = 0x5EED
        const val MUTATIONS_PER_FILE = 200
    }
}
