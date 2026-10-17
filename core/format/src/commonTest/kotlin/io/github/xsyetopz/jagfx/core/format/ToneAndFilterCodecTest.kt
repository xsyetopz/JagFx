package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Segment
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ToneAndFilterCodecTest {
    @Test
    fun referenceSlotsAreTheTonesOfTheirVoices() {
        for (name in ReferenceSynths.hex.keys) {
            val bytes = referenceSynth(name)
            val patch = SynthCodec.decode(bytes).patch

            val slots =
                patch.voices.map {
                    if (it ==
                        null
                    ) {
                        byteArrayOf(0)
                    } else {
                        SynthCodec.encodeTone(it)
                    }
                }
            val loop = bytes.copyOfRange(bytes.size - 4, bytes.size)

            assertContentEquals(bytes, slots.reduce(ByteArray::plus) + loop, name)
        }
    }

    @Test
    fun referenceTonesAndFiltersRoundTrip() {
        for (name in ReferenceSynths.hex.keys) {
            for (voice in SynthCodec
                .decode(referenceSynth(name))
                .patch.voices
                .filterNotNull()) {
                val tone = SynthCodec.decodeTone(SynthCodec.encodeTone(voice))
                val filter = SynthCodec.decodeFilter(SynthCodec.encodeFilter(voice.filter))

                assertEquals(ToneDecodeResult(voice, emptyList()), tone, name)
                assertEquals(FilterDecodeResult(voice.filter, emptyList()), filter, name)
            }
        }
    }

    @Test
    fun noFilterIsOneZeroByte() {
        assertContentEquals(byteArrayOf(0), SynthCodec.encodeFilter(null))
    }

    @Test
    fun emptyFilterFileIsTruncated() {
        val result = SynthCodec.decodeFilter(ByteArray(0))

        assertNull(result.filter)
        assertEquals(listOf("File truncated at byte offset 0"), result.warnings)
    }

    @Test
    fun bytesAfterAToneGiveAWarning() {
        val bytes = SynthCodec.encodeTone(voice()) + byteArrayOf(1, 2)

        val result = SynthCodec.decodeTone(bytes)

        assertEquals(voice(), result.voice)
        assertEquals(listOf("Ignored 2 bytes after byte offset ${bytes.size - 2}"), result.warnings)
    }

    @Test
    fun truncatedToneGivesAWarning() {
        val bytes = SynthCodec.encodeTone(voice())

        val result = SynthCodec.decodeTone(bytes.copyOf(bytes.size - 3))

        assertEquals(listOf("File truncated at byte offset ${bytes.size - 1}"), result.warnings)
    }

    @Test
    fun encodersRejectInvariantsWithSlotZero() {
        val modulatedWithoutEnvelope = filter(feedforward = listOf(pole(1, 2, endPhase = 3)))

        val tone =
            assertFailsWith<InvalidPatchException> {
                SynthCodec.encodeTone(voice(filter = modulatedWithoutEnvelope))
            }
        val filter =
            assertFailsWith<InvalidPatchException> {
                SynthCodec.encodeFilter(
                    modulatedWithoutEnvelope,
                )
            }

        assertEquals(PatchInvariant.I4, tone.invariant)
        assertEquals(0, tone.slot)
        assertEquals(PatchInvariant.I4, filter.invariant)
    }

    @Test
    fun modulatedFilterRoundTrips() {
        val modulated =
            filter(
                feedforward = listOf(pole(1, 2, endPhase = 3)),
                feedback = listOf(pole(4, 5)),
                unityGain0 = 7,
                unityGain1 = 9,
                modulationEnvelope = listOf(Segment(100, 200)),
            )

        assertEquals(modulated, SynthCodec.decodeFilter(SynthCodec.encodeFilter(modulated)).filter)
    }
}
