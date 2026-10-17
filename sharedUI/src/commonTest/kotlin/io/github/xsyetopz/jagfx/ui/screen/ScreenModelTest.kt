package io.github.xsyetopz.jagfx.ui.screen

import io.github.xsyetopz.jagfx.core.format.PatchInvariant
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Gate
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.DefaultVoice
import io.github.xsyetopz.jagfx.ui.editor.RejectReason
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.StatusHint
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScreenModelTest {
    private val strings = Strings(UiLocale.En)

    private fun envelope(start: Int) = Envelope(Waveform.Sine, start, start, emptyList())

    private fun partial(delay: Int) = Partial(Percent(50), 0, Milliseconds(delay))

    @Test
    fun partialRowsShowEveryHarmonicSlot() {
        val voice = DefaultVoice.copy(partials = listOf(partial(1), partial(2)))
        val rows = partialRows(voice)
        assertEquals(List(SynthLimits.MAX_PARTIALS) { it }, rows.map { it.index })
        assertEquals(listOf(1, 2), rows.mapNotNull { it.partial?.delay?.value })
        assertNull(rows[2].partial)
        assertEquals(emptyList(), partialRows(null).mapNotNull { it.partial })
    }

    @Test
    fun timelineSpansTheLatestVoiceOrLoopEnd() {
        assertEquals(1, timelineSpan(Patch.Empty))
        val voice = DefaultVoice.copy(offset = Milliseconds(100), duration = Milliseconds(400))
        val patch =
            Patch.Empty.copy(
                voices =
                    Patch.Empty.voices
                        .toMutableList()
                        .apply { set(2, voice) },
            )
        assertEquals(500, timelineSpan(patch))
        assertEquals(900, timelineSpan(patch.copy(loop = Loop(Milliseconds(0), Milliseconds(900)))))
    }

    @Test
    fun envelopeOfReadsLfoGateAndFilterEnvelopes() {
        val pitchLfo = Lfo(envelope(1), envelope(2))
        val gate = Gate(envelope(3), envelope(4))
        val segments = listOf(Segment(10, 20))
        val voice =
            DefaultVoice.copy(
                frequencyEnvelope = envelope(5),
                pitchLfo = pitchLfo,
                gate = gate,
                filter = Filter(0, 0, emptyList(), emptyList(), segments),
            )
        assertEquals(envelope(5), envelopeOf(voice, SignalChainSlot.Pitch))
        assertEquals(pitchLfo.rate, envelopeOf(voice, SignalChainSlot.VibratoRate))
        assertEquals(pitchLfo.depth, envelopeOf(voice, SignalChainSlot.VibratoDepth))
        assertEquals(gate.gapOn, envelopeOf(voice, SignalChainSlot.GapOn))
        assertEquals(
            Envelope(Waveform.Off, 0, 0, segments),
            envelopeOf(voice, SignalChainSlot.Filter),
        )
        assertNull(envelopeOf(voice, SignalChainSlot.TremoloRate))
        assertNull(envelopeOf(voice, SignalChainSlot.Output))
    }

    @Test
    fun envelopeOfAnEmptyVoiceIsNull() {
        for (slot in SignalChainSlot.entries) {
            assertNull(envelopeOf(null as Voice?, slot), slot.name)
        }
    }

    @Test
    fun statusTextFormatsEachHint() {
        assertEquals("", statusText(null, strings))
        assertEquals("Loaded a.synth", statusText(StatusHint.Loaded("a.synth"), strings))
        assertEquals(
            "Could not open a.synth: boom",
            statusText(StatusHint.OpenFailed("a.synth", "boom"), strings),
        )
        assertEquals("Saved a.synth", statusText(StatusHint.Saved("a.synth"), strings))
        assertEquals("Exported a.tone", statusText(StatusHint.Exported("a.tone"), strings))
        assertEquals(
            "Could not save /x/a.synth: denied",
            statusText(StatusHint.SaveFailed("/x/a.synth", "denied"), strings),
        )
    }

    @Test
    fun statusTextNamesTheRejectReason() {
        assertEquals(
            "Enable this voice slot to edit it",
            statusText(StatusHint.EditRejected(RejectReason.DisabledSlot), strings),
        )
        assertEquals(
            "The pitch envelope needs a waveform",
            statusText(StatusHint.EditRejected(RejectReason.PitchWaveformOff), strings),
        )
        assertEquals(
            "Pick a waveform to create this envelope first",
            statusText(StatusHint.EditRejected(RejectReason.MissingEnvelope), strings),
        )
        assertEquals(
            "Add filter poles first",
            statusText(StatusHint.EditRejected(RejectReason.MissingFilter), strings),
        )
        assertEquals(
            "This edit has no .synth encoding (I3)",
            statusText(
                StatusHint.EditRejected(RejectReason.NoBinaryEncoding, PatchInvariant.I3),
                strings,
            ),
        )
    }
}
