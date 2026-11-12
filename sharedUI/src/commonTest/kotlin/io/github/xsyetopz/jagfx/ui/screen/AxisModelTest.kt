package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AxisModelTest {
    private val pitch =
        Envelope(Waveform.Sine, 30, 250, listOf(Segment(0, 0), Segment(32768, 32768)))

    @Test
    fun timeTicksSpanTheWholeVoiceAtZoomOne() {
        val ticks = timeTicks(1200, 0f..1f, plotWidth = 600f, minGapPx = 50f)

        assertEquals(0, ticks.first())
        assertEquals(1200, ticks.last())
        assertEquals(listOf(100), ticks.zipWithNext { a, b -> b - a }.distinct())
    }

    @Test
    fun timeTicksStayInsideTheVisibleWindow() {
        val ticks = timeTicks(1000, 0.25f..0.5f, plotWidth = 500f, minGapPx = 50f)

        assertTrue(ticks.all { it in 250..500 })
        assertEquals(listOf(250, 300, 350, 400, 450, 500), ticks)
    }

    @Test
    fun timeTicksOfAnEmptyVoiceAreEmpty() {
        assertEquals(emptyList(), timeTicks(0, 0f..1f, 600f, 50f))
    }

    @Test
    fun valueTicksRunFromStartToEndInTheSlotUnit() {
        val ticks = valueTicks(SignalChainSlot.Pitch, pitch, count = 3)

        assertEquals(listOf(0f, 0.5f, 1f), ticks.map { it.fraction })
        assertEquals(listOf(30.0, 140.0, 250.0), ticks.map { it.value })
    }

    @Test
    fun valueTicksConvertPercentAndSkipSlotsWithoutAUnit() {
        val volume = Envelope(Waveform.Sine, 0, 65535, emptyList())

        assertEquals(100.0, valueTicks(SignalChainSlot.Volume, volume).last().value, 0.01)
        assertEquals(emptyList(), valueTicks(SignalChainSlot.Filter, volume))
    }

    @Test
    fun gateValuesConvertToMilliseconds() {
        assertEquals(1.0, ValueUnit.Ms.fromRaw(256.0 * 22.05), 1e-9)
        assertEquals(50.0, ValueUnit.Percent.fromRaw(32767.5), 1e-9)
    }

    @Test
    fun readoutShowsTimeValueAndRawNumbers() {
        val readout = pointReadout(SignalChainSlot.Pitch, pitch, 1, durationMs = 1000)

        assertEquals(PointReadout(500f, 140.0, ValueUnit.Hz, 32768, 32768), readout)
    }

    @Test
    fun readoutOfBreakpointZeroIsAtTimeZero() {
        val envelope = Envelope(Waveform.Sine, 0, 100, listOf(Segment(900, 0)))

        assertEquals(0, pointReadout(SignalChainSlot.Pitch, envelope, 0, 1000)?.position)
    }

    @Test
    fun readoutOfAMissingPointIsNull() {
        assertNull(pointReadout(SignalChainSlot.Pitch, pitch, 5, 1000))
    }

    @Test
    fun filterReadoutHasNoUnit() {
        val curve = Envelope(Waveform.Off, 0, 0, listOf(Segment(0, 100)))

        val readout = pointReadout(SignalChainSlot.Filter, curve, 0, 1000)

        assertNull(readout?.unit)
        assertEquals(100.0, readout?.value)
    }
}
