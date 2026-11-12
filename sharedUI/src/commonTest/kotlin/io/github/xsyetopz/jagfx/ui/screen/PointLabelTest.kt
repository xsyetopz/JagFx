package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class PointLabelTest {
    @Test
    fun labelHasTimeAndValueInUnitOfSlot() {
        val readout = PointReadout(12.5f, 300.0, ValueUnit.Hz, 800, 40000)
        assertEquals("12.5 ms · 300 Hz", pointLabelText(readout))
    }

    @Test
    fun labelOmitsRawPositionAndLevel() {
        val readout = PointReadout(500f, 87.25, ValueUnit.Percent, 32768, 1)
        assertEquals("500 ms · 87.25%", pointLabelText(readout))
    }

    @Test
    fun unitlessPointShowsRawLevel() {
        val readout = PointReadout(0f, 40000.0, null, 0, 40000)
        assertEquals("0 ms · 40000", pointLabelText(readout))
    }

    @Test
    fun everyPointOfEnvelopeGetsLabel() {
        val envelope =
            Envelope(Waveform.Sine, 100, 300, listOf(Segment(0, 0), Segment(32768, 65535)))
        val labels = pointLabels(SignalChainSlot.Pitch, envelope, durationMs = 1000)
        assertEquals(listOf(0, 1), labels.map { it.index })
        assertEquals("0 ms · 100 Hz", labels[0].text)
        assertEquals("500 ms · 300 Hz", labels[1].text)
    }
}
