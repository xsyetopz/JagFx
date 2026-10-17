package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.PatchInvariant
import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EditorReducerTest {
    private val enabled = EditorState().after(SlotIntent.SetVoiceEnabled(0, true))

    private fun rejected(
        reason: RejectReason,
        invariant: PatchInvariant? = null,
    ) = StatusHint.EditRejected(reason, invariant)

    @Test
    fun enablingSlotCreatesDefaultVoiceAndMarksDirty() {
        assertEquals(DefaultVoice, enabled.voice)
        assertTrue(enabled.isDirty)
        assertTrue(enabled.canUndo)
    }

    @Test
    fun editsToDisabledSlotAreRejected() {
        val state = EditorState().after(VoiceIntent.SetDuration(5))
        assertEquals(Patch.Empty, state.patch)
        assertEquals(rejected(RejectReason.DisabledSlot), state.statusHint)
        assertFalse(state.canUndo)
    }

    @Test
    fun pitchWaveformOffIsRejected() {
        val state = enabled.after(EnvelopeIntent.SetWaveform(SignalChainSlot.Pitch, Waveform.Off))
        assertEquals(enabled.patch, state.patch)
        assertEquals(rejected(RejectReason.PitchWaveformOff), state.statusHint)

        val square = state.after(EnvelopeIntent.SetWaveform(SignalChainSlot.Pitch, Waveform.Square))
        assertEquals(Waveform.Square, square.voice!!.frequencyEnvelope.waveform)
        assertNull(square.statusHint)
    }

    @Test
    fun firstWaveformOfMissingPairCreatesPairAndOffRemovesIt() {
        val created =
            enabled.after(
                EnvelopeIntent.SetWaveform(SignalChainSlot.VibratoRate, Waveform.Saw),
            )
        val empty = Envelope(Waveform.Off, 0, 0, emptyList())
        assertEquals(
            Lfo(Envelope(Waveform.Saw, 0, 0, emptyList()), empty),
            created.voice!!.pitchLfo,
        )
        assertRoundTrips(created.patch)

        val shaped = created.after(EnvelopeIntent.SetEndValue(SignalChainSlot.VibratoDepth, 300))
        val removed =
            shaped.after(
                EnvelopeIntent.SetWaveform(SignalChainSlot.VibratoRate, Waveform.Off),
            )
        assertNull(removed.voice!!.pitchLfo)
        assertEquals(shaped.patch, removed.after(HistoryIntent.Undo).patch)
    }

    @Test
    fun secondEnvelopeEditOnMissingPairIsRejected() {
        for (target in listOf(
            SignalChainSlot.VibratoDepth,
            SignalChainSlot.TremoloDepth,
            SignalChainSlot.GapOn,
        )) {
            val state = enabled.after(EnvelopeIntent.SetStartValue(target, 1))
            assertEquals(enabled.patch, state.patch)
            assertEquals(rejected(RejectReason.MissingEnvelope), state.statusHint)
        }
        val gate = enabled.after(EnvelopeIntent.SetStartValue(SignalChainSlot.GapOff, 1))
        assertEquals(rejected(RejectReason.MissingEnvelope), gate.statusHint)
    }

    @Test
    fun startAndEndValuesKeepEveryFormatValue() {
        val state =
            enabled.after(
                EnvelopeIntent.SetStartValue(SignalChainSlot.Pitch, Int.MAX_VALUE),
                EnvelopeIntent.SetEndValue(SignalChainSlot.Pitch, -1),
                EnvelopeIntent.SetStartValue(SignalChainSlot.Volume, Int.MIN_VALUE),
            )
        assertEquals(Int.MAX_VALUE, state.voice!!.frequencyEnvelope.startValue)
        assertEquals(-1, state.voice!!.frequencyEnvelope.endValue)
        assertEquals(Int.MIN_VALUE, state.voice!!.amplitudeEnvelope.startValue)
        assertRoundTrips(state.patch)
    }

    @Test
    fun segmentEditsInsertReplaceAndRemove() {
        val target = SignalChainSlot.Volume
        val a = Segment(10, 1)
        val b = Segment(20, 2)
        val state =
            enabled.after(
                EnvelopeIntent.InsertSegment(target, 99, a),
                EnvelopeIntent.InsertSegment(target, -3, b),
                EnvelopeIntent.SetSegment(target, 1, b),
                EnvelopeIntent.SetSegment(target, 7, a),
                EnvelopeIntent.RemoveSegment(target, 0),
                EnvelopeIntent.RemoveSegment(target, 5),
            )
        assertEquals(listOf(b), state.voice!!.amplitudeEnvelope.segments)

        val many = state.after(EnvelopeIntent.SetSegments(target, List(300) { a }))
        assertEquals(
            255,
            many.voice!!
                .amplitudeEnvelope.segments.size,
        )
        assertEquals(many.patch, many.after(EnvelopeIntent.InsertSegment(target, 0, b)).patch)
    }

    @Test
    fun voiceScalarsClamp() {
        val state =
            enabled.after(
                VoiceIntent.SetDuration(70_000),
                VoiceIntent.SetOffset(-5),
                VoiceIntent.SetEchoDelay(40_000),
                VoiceIntent.SetEchoFeedback(-1),
            )
        val voice = state.voice!!
        assertEquals(65535, voice.duration.value)
        assertEquals(0, voice.offset.value)
        assertEquals(32767, voice.echo.delay.value)
        assertEquals(0, voice.echo.feedback.value)
    }

    @Test
    fun partialsToggleAndClamp() {
        val three = enabled.after(*Array(3) { PartialIntent.TogglePartial(9) })
        assertEquals(3, three.voice!!.partials.size)
        assertEquals(
            100,
            three.voice!!
                .partials[0]
                .amplitude.value,
        )

        val edited =
            three.after(
                PartialIntent.SetAmplitude(0, 0),
                PartialIntent.SetPitchOffset(1, Int.MAX_VALUE),
                PartialIntent.SetDelay(2, Int.MIN_VALUE),
                PartialIntent.SetAmplitude(8, 5),
                PartialIntent.TogglePartial(1),
            )
        val partials = edited.voice!!.partials
        assertEquals(listOf(1, 100), partials.map { it.amplitude.value })
        assertEquals(0, partials[1].delay.value)

        val full = enabled.after(*Array(12) { PartialIntent.TogglePartial(10) })
        assertEquals(SynthLimits.MAX_PARTIALS, full.voice!!.partials.size)
    }

    @Test
    fun setVoiceReplacesTheSlotAsOneUndoStep() {
        val voice = DefaultVoice.copy(duration = Milliseconds(250))
        val state = EditorState().after(SlotIntent.SetVoice(3, voice))
        assertEquals(voice, state.patch.voices[3])
        assertTrue(state.isDirty)
        assertEquals(Patch.Empty, state.after(HistoryIntent.Undo).patch)
        assertSame(state, state.after(SlotIntent.SetVoice(SynthLimits.VOICE_SLOTS, voice)))
    }

    @Test
    fun setFilterReplacesTheFilterOfTheSelectedVoice() {
        val filter = DefaultFilter.copy(unityGain0 = 7, unityGain1 = 7)
        assertEquals(filter, enabled.after(FilterIntent.SetFilter(filter)).voice!!.filter)
        assertEquals(
            rejected(RejectReason.DisabledSlot),
            EditorState().after(FilterIntent.SetFilter(filter)).statusHint,
        )
    }

    @Test
    fun turningTheFilterOffKeepsItForTurningItOn() {
        val filter = DefaultFilter.copy(unityGain0 = 9, unityGain1 = 9)
        val withFilter = enabled.after(FilterIntent.SetFilter(filter))
        val off = withFilter.after(FilterIntent.SetEnabled(false))
        assertNull(off.voice!!.filter)
        assertEquals(filter, off.after(FilterIntent.SetEnabled(true)).voice!!.filter)
        assertEquals(DefaultFilter, enabled.after(FilterIntent.SetEnabled(true)).voice!!.filter)
        assertSame(withFilter, withFilter.after(FilterIntent.SetEnabled(true)))
    }

    @Test
    fun poleCountCreatesResizesAndRemovesFilter() {
        val created = enabled.after(FilterIntent.SetPoleCount(0, 9))
        val filter = assertNotNull(created.voice!!.filter)
        assertEquals(4, filter.feedforward.size)
        assertNull(filter.modulationEnvelope)

        val both = created.after(FilterIntent.SetPoleCount(1, 2), FilterIntent.SetPoleCount(0, 1))
        assertEquals(
            1,
            both.voice!!
                .filter!!
                .feedforward.size,
        )
        assertEquals(
            2,
            both.voice!!
                .filter!!
                .feedback.size,
        )

        val removed = both.after(FilterIntent.SetPoleCount(1, -4), FilterIntent.SetPoleCount(0, 0))
        assertNull(removed.voice!!.filter)
        assertEquals(enabled.patch, enabled.after(FilterIntent.SetPoleCount(0, 0)).patch)
    }

    @Test
    fun feedbackOnlyFilterIsAccepted() {
        val state = enabled.after(FilterIntent.SetPoleCount(1, 1))
        assertEquals(
            1,
            state.voice!!
                .filter!!
                .feedback.size,
        )
        assertNull(state.statusHint)
    }

    @Test
    fun filterEditsWithoutFilterAreRejected() {
        val gain = enabled.after(FilterIntent.SetUnityGain(0, 1))
        assertEquals(rejected(RejectReason.MissingFilter), gain.statusHint)
        val segment =
            enabled.after(
                EnvelopeIntent.SetSegments(SignalChainSlot.Filter, listOf(Segment(1, 1))),
            )
        assertEquals(rejected(RejectReason.MissingFilter), segment.statusHint)
    }

    @Test
    fun modulationEnvelopeFollowsFilterModulation() {
        val filtered = enabled.after(FilterIntent.SetPoleCount(0, 2))
        val noEnvelope =
            filtered.after(
                EnvelopeIntent.SetSegments(SignalChainSlot.Filter, listOf(Segment(1, 1))),
            )
        assertEquals(rejected(RejectReason.MissingEnvelope), noEnvelope.statusHint)

        val modulated =
            filtered.after(
                FilterIntent.SetCoefficient(0, 1, 1, 70_000, -2),
                EnvelopeIntent.SetSegments(SignalChainSlot.Filter, listOf(Segment(1, 1))),
            )
        val filter = modulated.voice!!.filter!!
        assertEquals(Coefficient(65535, 0), filter.feedforward[1].end)
        assertEquals(listOf(Segment(1, 1)), filter.modulationEnvelope)
        assertRoundTrips(modulated.patch)

        val unmodulated = modulated.after(FilterIntent.SetCoefficient(0, 1, 1, 0, 0))
        assertNull(unmodulated.voice!!.filter!!.modulationEnvelope)

        val byGain = filtered.after(FilterIntent.SetUnityGain(0, 99_999))
        assertEquals(65535, byGain.voice!!.filter!!.unityGain0)
        assertEquals(emptyList(), byGain.voice!!.filter!!.modulationEnvelope)
    }

    @Test
    fun copyModePastesIntoAnotherSlot() {
        val source = enabled.after(VoiceIntent.SetDuration(42), SlotIntent.CopyVoice)
        assertTrue(source.isCopyMode)

        val same = source.after(SlotIntent.SelectVoice(0))
        assertTrue(same.isCopyMode)

        val pasted = source.after(SlotIntent.SelectVoice(3))
        assertFalse(pasted.isCopyMode)
        assertEquals(3, pasted.selectedVoice)
        assertEquals(42, pasted.voice!!.duration.value)
        assertEquals(source.patch, pasted.after(HistoryIntent.Undo).patch)
    }

    @Test
    fun slotSelectionAndResetting() {
        assertEquals(9, enabled.after(SlotIntent.SelectVoice(50)).selectedVoice)
        assertNull(enabled.after(SlotIntent.ResetVoice).voice)
        assertNull(enabled.after(SlotIntent.SetVoiceEnabled(0, false)).voice)
        assertEquals(
            enabled,
            enabled.after(
                SlotIntent.SetVoiceEnabled(0, true),
                SlotIntent.SetVoiceEnabled(10, true),
            ),
        )
    }

    @Test
    fun targetOffsetWrapsAroundEnvelopeChain() {
        val previous = enabled.after(SlotIntent.SelectTargetByOffset(-1))
        assertEquals(SignalChainSlot.Filter, previous.selectedTarget)
        val wrapped = enabled.after(SlotIntent.SelectTargetByOffset(10))
        assertEquals(SignalChainSlot.VibratoRate, wrapped.selectedTarget)
        val bode = enabled.after(SlotIntent.SelectTarget(SignalChainSlot.Bode))
        val view = bode.after(SlotIntent.SelectTargetByOffset(1))
        assertEquals(SignalChainSlot.VibratoRate, view.selectedTarget)
    }

    @Test
    fun loopEditsClampAndDefaultLoopSpansVoices() {
        val state = enabled.after(LoopIntent.SetLoopBegin(-1), LoopIntent.SetLoopEnd(1_000_000))
        assertEquals(Loop(Milliseconds(0), Milliseconds(65535)), state.patch.loop)

        val looping = enabled.after(VoiceIntent.SetOffset(250), ViewIntent.SetLooping(true))
        assertEquals(Loop(Milliseconds(0), Milliseconds(1250)), looping.playbackLoop)
        assertEquals(EditorState.INFINITE_LOOP_PASSES, looping.effectiveLoopCount)
        assertEquals(7, looping.after(ViewIntent.SetLoopCount(7)).effectiveLoopCount)
        assertEquals(999, looping.after(ViewIntent.SetLoopCount(5000)).loopCount)
        assertEquals(1, looping.after(ViewIntent.SetLooping(false)).effectiveLoopCount)
    }

    @Test
    fun loadedResetsDocumentState() {
        val patch = enabled.after(SlotIntent.ResetVoice, SlotIntent.SetVoiceEnabled(4, true)).patch
        val state =
            enabled.after(
                FilterIntent.SetEnabled(true),
                FilterIntent.SetEnabled(false),
                ResultIntent.Loaded("bell", "/home/u/bell.synth", patch),
            )
        assertEquals(4, state.selectedVoice)
        assertFalse(state.isDirty)
        assertFalse(state.canUndo)
        assertEquals(List(SynthLimits.VOICE_SLOTS) { null }, state.bypassedFilters)
        assertEquals(StatusHint.Loaded("bell"), state.statusHint)
        assertEquals("JagFx: ~/bell.synth", state.windowTitle("/home/u"))
        assertEquals("JagFx: /home/u/bell.synth", state.windowTitle("/home/us"))
    }

    @Test
    fun savedClearsDirtyAndRenames() {
        val saved =
            enabled.after(
                ResultIntent.Saved("zap.synth", "C:\\sfx\\zap.synth", enabled.patch),
            )
        assertFalse(saved.isDirty)
        assertEquals("zap", saved.fileName)
        assertEquals("JagFx: untitled *", enabled.windowTitle(null))

        val failed = enabled.after(ResultIntent.SaveFailed("/x", "denied"))
        assertEquals(StatusHint.SaveFailed("/x", "denied"), failed.statusHint)
        assertEquals(
            StatusHint.OpenFailed("a", "bad"),
            enabled.after(ResultIntent.LoadFailed("a", "bad")).statusHint,
        )
    }

    @Test
    fun playbackResultsAndPreview() {
        val playing = enabled.after(ResultIntent.PlaybackStarted, ResultIntent.PlaybackPosition(2f))
        assertTrue(playing.isPlaying)
        assertEquals(1f, playing.playbackPosition)
        assertEquals(0f, playing.after(ResultIntent.PlaybackPosition(Float.NaN)).playbackPosition)
        assertFalse(playing.after(ResultIntent.PlaybackStopped).isPlaying)

        val preview =
            enabled
                .after(
                    ResultIntent.PreviewRendered(shortArrayOf(Short.MIN_VALUE, 16384)),
                ).preview
        assertEquals(listOf(-1f, 0.5f), preview.samples.toList())
        assertEquals(
            enabled.patch,
            enabled.after(ResultIntent.PreviewRendered(shortArrayOf(1))).patch,
        )
    }

    @Test
    fun requestsLeaveStateUnchanged() {
        val requests =
            listOf(
                RequestIntent.Open,
                RequestIntent.Save,
                RequestIntent.SaveAs,
                RequestIntent.TogglePlay,
                RequestIntent.Stop,
            )
        for (request in requests) assertSame(enabled, reduce(enabled, request))
    }
}
