package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlotSummaryTest {
    private val en = Strings(UiLocale.En)
    private val durationMs = 1200
    private val segments =
        listOf(Segment(0, 0), Segment(20000, 30000), Segment(40000, 50000), Segment(65535, 65535))
    private val pitch = Envelope(Waveform.Sine, 0, 65535, segments)

    private fun pole() = Pole(Coefficient(1000, 20000), Coefficient(1000, 20000))

    private fun filter(
        zeros: Int,
        poles: Int,
    ) = Filter(
        unityGain0 = 65535,
        unityGain1 = 65535,
        feedforward = List(zeros) { pole() },
        feedback = List(poles) { pole() },
        modulationEnvelope = null,
    )

    @Test
    fun envelopeSummaryNamesTheEnvelopeItsPointsRangeAndLength() {
        val summary = envelopeSummary(en, SignalChainSlot.Pitch, pitch, durationMs)

        assertTrue(summary.startsWith(en[titleOf(SignalChainSlot.Pitch)]), summary)
        assertTrue(" ${segments.size} " in summary, summary)
        assertTrue(summary.endsWith("over $durationMs ms"), summary)
        assertTrue(" Hz to " in summary && summary.contains(" Hz over"), summary)
    }

    @Test
    fun envelopeSummaryRangeIsLowestToHighestPointValue() {
        val values =
            segments.indices.map {
                pointReadout(
                    SignalChainSlot.Pitch,
                    pitch,
                    it,
                    durationMs,
                )!!.value
            }
        val summary = envelopeSummary(en, SignalChainSlot.Pitch, pitch, durationMs)

        assertTrue(
            formatDecimal(values.min(), 2) + " Hz to " + formatDecimal(values.max(), 2) in summary,
        )
    }

    @Test
    fun envelopeSummaryOfAMissingEnvelopeSaysSo() {
        val missing = envelopeSummary(en, SignalChainSlot.Pitch, null, durationMs)
        val empty =
            envelopeSummary(
                en,
                SignalChainSlot.Pitch,
                pitch.copy(segments = emptyList()),
                durationMs,
            )

        assertEquals(missing, empty)
        assertTrue(missing.startsWith(en[titleOf(SignalChainSlot.Pitch)]))
    }

    @Test
    fun envelopeStateDescribesTheSelectedPointOnly() {
        assertNull(envelopeState(en, SignalChainSlot.Pitch, pitch, -1, durationMs))
        assertNull(envelopeState(en, SignalChainSlot.Pitch, null, 0, durationMs))
        val state = envelopeState(en, SignalChainSlot.Pitch, pitch, 1, durationMs)

        assertNotNull(state)
        assertTrue(state.startsWith("Point 2 of ${segments.size}"), state)
        assertTrue(
            readoutText(pointReadout(SignalChainSlot.Pitch, pitch, 1, durationMs)!!) in state,
        )
    }

    @Test
    fun outputSummaryGivesLengthAndPeak() {
        val samples = FloatArray(22050) { if (it == 5) -0.5f else 0.25f }

        assertEquals("Output waveform, 1000 ms, peak 50%", outputSummary(en, samples))
    }

    @Test
    fun outputSummaryOfNoSamplesSaysNothingIsRendered() {
        assertEquals(outputSummary(en, null), outputSummary(en, FloatArray(0)))
        assertFalse("ms" in outputSummary(en, null))
    }

    @Test
    fun poleZeroSummaryCountsWithSingularAndPlural() {
        assertTrue(poleZeroSummary(en, filter(zeros = 1, poles = 2)).endsWith("2 poles, 1 zero"))
        assertTrue(poleZeroSummary(en, filter(zeros = 2, poles = 1)).endsWith("1 pole, 2 zeros"))
        assertTrue(poleZeroSummary(en, filter(zeros = 0, poles = 0)).endsWith("0 poles, 0 zeros"))
        assertFalse(poleZeroSummary(en, null).endsWith("zeros"))
    }

    @Test
    fun responseSummaryNamesThePeakAndEmptyFiltersSayNone() {
        val summary = responseSummary(en, filter(zeros = 0, poles = 2))

        assertTrue(" dB at " in summary && summary.endsWith(" Hz"), summary)
        assertEquals(responseSummary(en, null), responseSummary(en, filter(zeros = 0, poles = 0)))
    }

    @Test
    fun everyLocaleBuildsEverySummaryWithoutLeftoverPlaceholders() {
        for (locale in UiLocale.entries) {
            val strings = Strings(locale)
            val texts =
                listOf(
                    envelopeSummary(strings, SignalChainSlot.Pitch, pitch, durationMs),
                    envelopeSummary(strings, SignalChainSlot.Pitch, null, durationMs),
                    envelopeState(strings, SignalChainSlot.Pitch, pitch, 0, durationMs)!!,
                    outputSummary(strings, FloatArray(100) { 0.1f }),
                    outputSummary(strings, null),
                    poleZeroSummary(strings, filter(1, 1)),
                    poleZeroSummary(strings, filter(2, 3)),
                    poleZeroSummary(strings, null),
                    responseSummary(strings, filter(0, 2)),
                    responseSummary(strings, null),
                )
            for (text in texts) {
                assertFalse('{' in text, "${locale.tag}: $text")
            }
        }
    }

    @Test
    fun poleStateNamesTheSelectedMarkerItsPlaceAndValues() {
        val state = poleState(en, filter(zeros = 1, poles = 3), PoleRef(1, 1, 1))
        assertNotNull(state)
        assertTrue(state.startsWith("Pole 2 of 3, end: "), state)
        assertTrue(state.endsWith(" Hz, 30.5 dB"), state)
        assertTrue(
            poleState(
                en,
                filter(zeros = 2, poles = 1),
                PoleRef(0, 0, 0),
            )!!.startsWith("Zero 1 of 2, start: "),
        )
    }

    @Test
    fun poleStateIsNullWithoutAMatchingSelection() {
        assertNull(poleState(en, filter(1, 1), null))
        assertNull(poleState(en, null, PoleRef(0, 0, 0)))
        assertNull(poleState(en, filter(zeros = 1, poles = 1), PoleRef(1, 1, 0)))
    }
}
