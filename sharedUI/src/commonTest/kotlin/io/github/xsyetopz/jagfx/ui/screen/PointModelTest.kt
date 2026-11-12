package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PointModelTest {
    private val durationMs = 1000
    private val segments = listOf(Segment(0, 0), Segment(32768, 40000), Segment(60000, 65535))
    private val pitch = Envelope(Waveform.Sine, 100, 300, segments)

    private fun edit(
        field: PointField,
        text: String,
        index: Int = 1,
        envelope: Envelope = pitch,
    ) = editField(field, text, SignalChainSlot.Pitch, envelope, index, durationMs)

    @Test
    fun everySlotHasTitleAndDescriptionOfItsOwn() {
        val titles = SignalChainSlot.entries.map(::titleOf)
        val descriptions = SignalChainSlot.entries.map(::descriptionOf)

        assertEquals(SignalChainSlot.entries.size, titles.toSet().size)
        assertEquals(SignalChainSlot.entries.size, descriptions.toSet().size)
        assertTrue(descriptions.all { it in UiString.entries })
    }

    @Test
    fun formatDecimalTrimsTrailingZerosAndKeepsSign() {
        assertEquals("220", formatDecimal(220.0, 2))
        assertEquals("87.5", formatDecimal(87.5, 2))
        assertEquals("-0.25", formatDecimal(-0.25, 2))
        assertEquals("0.05", formatDecimal(0.05, 2))
    }

    @Test
    fun suffixFollowsUnit() {
        assertEquals(" Hz", ValueUnit.Hz.suffix())
        assertEquals("", (null as ValueUnit?).suffix())
    }

    @Test
    fun positionAndLevelFieldsShowStoredNumbers() {
        val shown = { field: PointField ->
            fieldText(field, SignalChainSlot.Pitch, pitch, 1, durationMs)
        }

        assertEquals(segments[1].duration.toString(), shown(PointField.Position))
        assertEquals(segments[1].targetLevel.toString(), shown(PointField.Level))
    }

    @Test
    fun firstPointIsAlwaysShownAtTimeZero() {
        val first = pitch.copy(segments = listOf(Segment(500, 0)) + segments.drop(1))

        assertEquals("0", fieldText(PointField.Time, SignalChainSlot.Pitch, first, 0, durationMs))
    }

    @Test
    fun timeEntryConvertsToRawPosition() {
        val half = edit(PointField.Time, "500")

        assertEquals(segments[1].copy(duration = 32768), assertIs<FieldEdit.Applied>(half).segment)
    }

    @Test
    fun timeEntryPastLastRawPositionReportsRange() {
        val past = assertIs<FieldEdit.OutOfRange>(edit(PointField.Time, durationMs.toString()))

        assertEquals("0", past.min)
        assertTrue(past.max.toDouble() <= durationMs)
    }

    @Test
    fun rawEntriesAcceptWholeU16RangeAndReportRest() {
        val top = assertIs<FieldEdit.Applied>(edit(PointField.Level, "${SynthLimits.U16_MAX}"))
        val over =
            assertIs<FieldEdit.OutOfRange>(edit(PointField.Position, "${SynthLimits.U16_MAX + 1}"))
        val below = edit(PointField.Level, "-1")

        assertEquals(SynthLimits.U16_MAX, top.segment.targetLevel)
        assertEquals(SynthLimits.U16_MAX.toString(), over.max)
        assertIs<FieldEdit.OutOfRange>(below)
    }

    @Test
    fun valueEntryInSlotUnitRoundTripsThroughTable() {
        val shown = fieldText(PointField.Value, SignalChainSlot.Pitch, pitch, 1, durationMs)
        val applied = assertIs<FieldEdit.Applied>(edit(PointField.Value, shown))

        assertEquals(
            segments[1].targetLevel.toDouble(),
            applied.segment.targetLevel.toDouble(),
            2.0,
        )
    }

    @Test
    fun valueEntryOnFlatEnvelopeIsRefusedInsteadOfDividingByZero() {
        val flat = pitch.copy(startValue = 100, endValue = 100)

        assertIs<FieldEdit.NotANumber>(edit(PointField.Value, "100", envelope = flat))
    }

    @Test
    fun textThatIsNotNumberIsRefused() {
        assertIs<FieldEdit.NotANumber>(edit(PointField.Position, "abc"))
        assertIs<FieldEdit.NotANumber>(edit(PointField.Time, ""))
    }

    @Test
    fun commaIsAcceptedAsDecimalSeparator() {
        val comma = assertIs<FieldEdit.Applied>(edit(PointField.Time, "250,5"))
        val dot = assertIs<FieldEdit.Applied>(edit(PointField.Time, "250.5"))

        assertEquals(dot.segment, comma.segment)
    }
}
