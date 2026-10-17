package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Echo
import io.github.xsyetopz.jagfx.core.model.Gate
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SynthCodecTest {
    @Test
    fun emptyInputDecodesToEmptyPatch() {
        val result = SynthCodec.decode(ByteArray(0))

        assertEquals(Patch.Empty, result.patch)
        assertEquals(emptyList(), result.warnings)
    }

    @Test
    fun emptyPatchEncodesToTenSlotMarkersAndLoop() {
        assertContentEquals(ByteArray(14), SynthCodec.encode(Patch.Empty))
    }

    @Test
    fun gitLfsPointerIsRejected() {
        val pointer =
            "version https://git-lfs.github.com/spec/v1\noid sha256:abc\nsize 12\n"
                .encodeToByteArray()

        val error = assertFailsWith<SynthFormatException> { SynthCodec.decode(pointer) }

        assertEquals(SynthFormatError.GitLfsPointer, error.error)
    }

    @Test
    fun smartIntegersRoundTripAtEveryEncodingBoundary() {
        val offsets = listOf(-16384, -65, -64, 0, 63, 64, 16383)
        val delays = listOf(0, 127, 128, 32767, 0, 1, 2)
        val partials =
            offsets.zip(
                delays,
            ) { offset, delay -> partial(pitchOffset = offset, delay = delay) }
        val patch =
            patchOf(
                *partials.chunked(SynthLimits.MAX_PARTIALS) { voice(partials = it) }.toTypedArray(),
            )

        assertEquals(patch, roundTrip(patch).patch)
    }

    @Test
    fun smartIntegersUseTheShortestForm() {
        val oneByte =
            SynthCodec.encode(
                patchOf(voice(partials = listOf(partial(pitchOffset = 63, delay = 127)))),
            )
        val twoByte =
            SynthCodec.encode(
                patchOf(voice(partials = listOf(partial(pitchOffset = 64, delay = 128)))),
            )

        assertEquals(oneByte.size + 2, twoByte.size)
    }

    @Test
    fun decoderAcceptsTwoByteFormOfOneByteValue() {
        val bytes = SynthCodec.encode(patchOf(voice(partials = listOf(partial(pitchOffset = 0)))))
        val pitchAt = PARTIALS_OFFSET + 1
        assertEquals(0x40, bytes[pitchAt].toInt())
        val longForm =
            bytes.copyOfRange(0, pitchAt) + byteArrayOf(0xC0.toByte(), 0) +
                bytes.copyOfRange(pitchAt + 1, bytes.size)

        val decoded = SynthCodec.decode(longForm)

        assertEquals(
            0,
            decoded.patch.voices[0]
                ?.partials
                ?.single()
                ?.pitchOffset,
        )
        assertContentEquals(bytes, SynthCodec.encode(decoded.patch))
    }

    /** Spec §5 rule 7 and §12 item a. */
    @Test
    fun fivePartialsKeepTheTerminatorByte() {
        val four =
            voice(
                partials = List(4) { partial(amplitude = it + 1) },
                echo = Echo(Milliseconds(5), Percent(100)),
            )
        val five = four.copy(partials = List(5) { partial(amplitude = it + 1) })

        assertEquals(
            SynthCodec.encode(patchOf(four)).size + 3,
            SynthCodec.encode(patchOf(five)).size,
        )
        assertEquals(patchOf(five), roundTrip(patchOf(five)).patch)
    }

    @Test
    fun sixthPartialIsRejected() {
        val bytes =
            SynthCodec.encode(
                patchOf(voice(partials = List(5) { partial(amplitude = it + 1) })),
            )
        var terminator = -1
        SynthCodec.decode(bytes) {
            if (it.path == "voices[0].partials[5].amplitude") terminator = it.offset
        }
        // Amplitude 1, pitch offset 0, delay 0.
        val sixPartials =
            bytes.copyOfRange(0, terminator) + byteArrayOf(1, 64, 0) +
                bytes.copyOfRange(terminator, bytes.size)

        val error = assertFailsWith<SynthFormatException> { SynthCodec.decode(sixPartials) }

        assertEquals(SynthFormatError.TooManyPartials, error.error)
        assertEquals(terminator, error.offset)
        assertEquals("voices[0].partials[5].amplitude", error.field)
    }

    @Test
    fun bytesAfterTheLoopAreRejected() {
        val synth = SynthCodec.encode(patchOf(voice()))
        val group = synth + byteArrayOf(0, 0, 15, -64, 1, 2, 3)

        val error = assertFailsWith<SynthFormatException> { SynthCodec.decode(group) }

        assertEquals(SynthFormatError.TrailingData, error.error)
        assertEquals(synth.size, error.offset)
    }

    @Test
    fun undefinedWaveformsRoundTripUnchanged() {
        val lfo = Lfo(envelope(Waveform(5), 1, 2), envelope(Waveform(255)))
        val patch = patchOf(voice().copy(pitchLfo = lfo))
        val bytes = SynthCodec.encode(patch)

        assertEquals(patch, SynthCodec.decode(bytes).patch)
        assertContentEquals(bytes, SynthCodec.encode(SynthCodec.decode(bytes).patch))
    }

    /** Spec §12 items c and e. */
    @Test
    fun filterMaskComesFromPhaseAndMagnitudePairs() {
        val segments = listOf(Segment(65535, 65535))
        val magnitudeOnly =
            filter(
                feedforward = listOf(pole(1000, 2000, endMagnitude = 2500), pole(3000, 4000)),
                feedback = listOf(pole(5000, 6000), pole(7000, 8000, endPhase = 7500)),
                modulationEnvelope = segments,
            )
        val unmodulated =
            filter(
                feedforward = listOf(pole(1000, 2000), pole(3000, 4000)),
                unityGain0 = 1,
                unityGain1 = 2,
                modulationEnvelope = segments,
            )

        assertEquals(0b0010_0001, magnitudeOnly.modulationMask)
        assertEquals(
            patchOf(voice(filter = magnitudeOnly)),
            roundTrip(patchOf(voice(filter = magnitudeOnly))).patch,
        )
        val bytes = SynthCodec.encode(patchOf(voice(filter = unmodulated)))
        val filterSize = 1 + 4 + 1 + 2 * 4 + 1 + 4
        assertEquals(filterOffset(voice()) + filterSize + 9 + 4, bytes.size)
        assertEquals(patchOf(voice(filter = unmodulated)), SynthCodec.decode(bytes).patch)
    }

    @Test
    fun decoderDropsModulationEnvelopeThatHasNoEffect() {
        val single = voice(filter = filter(feedforward = listOf(pole(1000, 2000))))
        val bytes = SynthCodec.encode(patchOf(single)).toMutableList()
        val at = filterOffset(single)
        // Raw mask bit 0 set, an end coefficient equal to the start coefficient, then the envelope.
        bytes[at + 5] = 1
        bytes.addAll(at + 10, listOf<Byte>(0x03, 0xE8.toByte(), 0x07, 0xD0.toByte(), 1, 0, 1, 0, 2))

        val decoded = SynthCodec.decode(bytes.toByteArray())

        assertEquals(emptyList(), decoded.warnings)
        assertNull(
            decoded.patch.voices[0]
                ?.filter
                ?.modulationEnvelope,
        )
    }

    /** Spec §12 item f. */
    @Test
    fun trailingEmptySlotsKeepTheLoop() {
        val patch =
            SynthCodec
                .decode(
                    referenceSynth("cow_death"),
                ).patch
                .copy(loop = Loop(Milliseconds(100), Milliseconds(200)))

        assertEquals(patch, roundTrip(patch).patch)
    }

    /** Spec §12 item m. */
    @Test
    fun envelopeWithoutSegmentsRoundTripsUnchanged() {
        val patch =
            patchOf(
                voice(
                    frequency = envelope(start = 100, end = 900),
                    amplitude = envelope(start = 0, end = 64),
                ),
            )

        assertEquals(patch, roundTrip(patch).patch)
    }

    @Test
    fun pairsRoundTrip() {
        val lfo = Lfo(envelope(Waveform.Noise, 1, 2), envelope(Waveform.Off, 3, 4))
        val gate = Gate(envelope(Waveform.Square, -5, 6), envelope(Waveform.Saw, 7, -8))
        val patch =
            patchOf(
                null,
                voice().copy(pitchLfo = lfo, amplitudeLfo = lfo, gate = gate),
                loop = Loop(Milliseconds(1), Milliseconds(65535)),
            )

        assertEquals(patch, roundTrip(patch).patch)
    }

    /** Game file 11870 starts with frequency waveform `245`. */
    @Test
    fun undefinedFrequencyWaveformStartsAVoice() {
        val bytes = SynthCodec.encode(patchOf(null, voice()))
        bytes[1] = -11

        val result = SynthCodec.decode(bytes)

        val frequency = voice().frequencyEnvelope.copy(waveform = Waveform(245))
        assertEquals(patchOf(null, voice(frequency = frequency)), result.patch)
        assertEquals(emptyList(), result.warnings)
        assertContentEquals(bytes, SynthCodec.encode(result.patch))
    }

    @Test
    fun poleCountAboveFourIsRejected() {
        val single = voice(filter = filter(feedforward = listOf(pole(1, 2))))
        val bytes = SynthCodec.encode(patchOf(single))
        bytes[filterOffset(single)] = 0x51

        val error = assertFailsWith<SynthFormatException> { SynthCodec.decode(bytes) }

        assertEquals(SynthFormatError.InvalidPoleCount, error.error)
        assertEquals(filterOffset(single), error.offset)
    }

    /** Spec §12 item i. */
    @Test
    fun truncatedFilterIsDiscarded() {
        val single = voice(filter = filter(feedforward = listOf(pole(1, 2), pole(3, 4))))
        val bytes = SynthCodec.encode(patchOf(single))
        // Cut inside the first coefficient: packed count, unity gains, and mask are 6 bytes.
        val cut = bytes.copyOf(filterOffset(single) + 7)
        // The cursor still advances over both coefficients (2 x 4 bytes) past the end.
        val endCursor = filterOffset(single) + 6 + 8

        val result = SynthCodec.decode(cut)

        assertEquals(patchOf(single.copy(filter = null)), result.patch)
        assertEquals(
            listOf(
                "Filter truncated (discarding partial filter payload)",
                "File truncated at byte offset $endCursor",
            ),
            result.warnings,
        )
    }

    @Test
    fun encoderRejectsEachInvariant() {
        val cases =
            listOf(
                PatchInvariant.I1 to voice(frequency = envelope(Waveform.Off)),
                PatchInvariant.I2 to
                    voice().copy(pitchLfo = Lfo(envelope(Waveform.Off), envelope())),
                PatchInvariant.I2 to voice().copy(gate = Gate(envelope(Waveform.Off), envelope())),
                PatchInvariant.I3 to voice(filter = filter()),
                PatchInvariant.I4 to
                    voice(filter = filter(feedforward = listOf(pole(1, 2, endPhase = 3)))),
                PatchInvariant.I4 to
                    voice(
                        filter =
                            filter(
                                feedforward = listOf(pole(1, 2)),
                                modulationEnvelope = emptyList(),
                            ),
                    ),
            )

        for ((invariant, bad) in cases) {
            val error =
                assertFailsWith<InvalidPatchException> {
                    SynthCodec.encode(
                        patchOf(voice(), null, bad),
                    )
                }
            assertEquals(invariant, error.invariant)
            assertEquals(2, error.slot)
        }
    }

    @Test
    fun filterWhoseBytesLookLikeAnEnvelopeRoundTrips() {
        val envelopeLike = filter(feedback = listOf(pole(0, 15)), unityGain0 = 0, unityGain1 = 0)
        val patch = patchOf(voice(filter = envelopeLike), voice())

        assertEquals(patch, roundTrip(patch).patch)
    }

    private companion object {
        /** Frequency envelope (10 bytes), amplitude envelope (14), and three absent pairs. */
        const val PARTIALS_OFFSET = 10 + 14 + 3
    }
}
