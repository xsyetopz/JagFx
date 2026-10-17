package io.github.xsyetopz.jagfx.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ModelDomainTest {
    @Test
    fun partialFieldsMustFitTheirEncodings() {
        Partial(Percent(1), SynthLimits.SMART16_MIN, Milliseconds(SynthLimits.USMART16_MAX))
        Partial(Percent(SynthLimits.USMART16_MAX), SynthLimits.SMART16_MAX, Milliseconds(0))

        assertFailsWith<IllegalArgumentException> { Partial(Percent(0), 0, Milliseconds(0)) }
        // Spec §12 item b: -20000 has no valid smart16 encoding.
        assertFailsWith<IllegalArgumentException> { Partial(Percent(1), -20000, Milliseconds(0)) }
        assertFailsWith<IllegalArgumentException> {
            Partial(Percent(1), SynthLimits.SMART16_MIN - 1, Milliseconds(0))
        }
        assertFailsWith<IllegalArgumentException> {
            Partial(Percent(1), SynthLimits.SMART16_MAX + 1, Milliseconds(0))
        }
        assertFailsWith<IllegalArgumentException> {
            Partial(
                Percent(1),
                0,
                Milliseconds(SynthLimits.USMART16_MAX + 1),
            )
        }
    }

    @Test
    fun countsAreBounded() {
        val segment = Segment(0, 0)
        val envelope = Envelope(Waveform.Sine, 0, 0, emptyList())
        val partial = Partial(Percent(1), 0, Milliseconds(0))
        val pole = Pole(Coefficient(0, 0), Coefficient(0, 0))

        assertFailsWith<IllegalArgumentException> {
            envelope.copy(
                segments = List(256) { segment },
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Filter(
                0,
                0,
                List(5) { pole },
                emptyList(),
                null,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Voice(
                envelope,
                envelope,
                null,
                null,
                null,
                List(11) {
                    partial
                },
                Echo.None,
                Milliseconds(0),
                Milliseconds(0),
                null,
            )
        }
        assertFailsWith<IllegalArgumentException> { Patch(List(9) { null }, Loop.None) }
    }

    @Test
    fun unsigned16FieldsAreBounded() {
        assertFailsWith<IllegalArgumentException> { Segment(SynthLimits.U16_MAX + 1, 0) }
        assertFailsWith<IllegalArgumentException> { Coefficient(-1, 0) }
        assertFailsWith<IllegalArgumentException> {
            Loop(
                Milliseconds(0),
                Milliseconds(SynthLimits.U16_MAX + 1),
            )
        }
    }

    @Test
    fun modulationMaskSetsOneBitPerModulatedPole() {
        val still = Pole(Coefficient(1, 2), Coefficient(1, 2))
        val moving = Pole(Coefficient(1, 2), Coefficient(1, 3))
        val filter = Filter(5, 5, listOf(still, moving), listOf(still, still, still, moving), null)

        assertEquals(0b1000_0010, filter.modulationMask)
        assertEquals(true, filter.needsModulationEnvelope)
        assertEquals(
            false,
            filter
                .copy(
                    feedforward = listOf(still),
                    feedback = listOf(still),
                ).needsModulationEnvelope,
        )
    }

    @Test
    fun waveformIdsMatchTheFormat() {
        assertEquals(listOf(0, 1, 2, 3, 4), Waveform.selectable.map { it.id })
        assertEquals(listOf(false, true, true), listOf(4, 5, 255).map { Waveform(it).isUndefined })
        assertFailsWith<IllegalArgumentException> { Waveform(256) }
        assertFailsWith<IllegalArgumentException> { Waveform(-1) }
    }
}
