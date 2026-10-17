package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Waveform
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EditorFuzzTest {
    private val extremes =
        intArrayOf(Int.MIN_VALUE, -65536, -1, 0, 1, 15, 32767, 32768, 65535, 65536, Int.MAX_VALUE)

    private fun Random.value(): Int =
        if (nextBoolean()) extremes[nextInt(extremes.size)] else nextInt(-70_000, 70_000)

    private fun Random.small(): Int = nextInt(-2, 12)

    private fun Random.target(): SignalChainSlot =
        SignalChainSlot.entries[nextInt(SignalChainSlot.entries.size)]

    private fun Random.segment(): Segment = Segment(nextInt(0, 65536), nextInt(0, 65536))

    private fun Random.segmentIntent(target: SignalChainSlot): EditorIntent =
        when (nextInt(4)) {
            0 -> EnvelopeIntent.InsertSegment(target, small(), segment())
            1 -> EnvelopeIntent.SetSegment(target, small(), segment())
            2 -> EnvelopeIntent.RemoveSegment(target, small())
            else -> EnvelopeIntent.SetSegments(target, List(nextInt(0, 300)) { segment() })
        }

    private fun Random.intent(): EditorIntent =
        when (nextInt(5)) {
            0 -> envelopeIntent()
            1 -> voiceIntent()
            2 -> filterIntent()
            else -> stateIntent()
        }

    private fun Random.envelopeIntent(): EditorIntent {
        val target = target()
        return when (nextInt(4)) {
            0 -> {
                EnvelopeIntent.SetWaveform(
                    target,
                    Waveform.selectable[nextInt(Waveform.selectable.size)],
                )
            }

            1 -> {
                EnvelopeIntent.SetStartValue(target, value())
            }

            2 -> {
                EnvelopeIntent.SetEndValue(target, value())
            }

            else -> {
                segmentIntent(target)
            }
        }
    }

    private fun Random.voiceIntent(): EditorIntent =
        when (nextInt(9)) {
            0 -> VoiceIntent.SetDuration(value())
            1 -> VoiceIntent.SetOffset(value())
            2 -> VoiceIntent.SetEchoDelay(value())
            3 -> VoiceIntent.SetEchoFeedback(value())
            4 -> PartialIntent.TogglePartial(small())
            5 -> PartialIntent.SetAmplitude(small(), value())
            6 -> PartialIntent.SetPitchOffset(small(), value())
            7 -> PartialIntent.SetDelay(small(), value())
            else -> FilterIntent.SetEnabled(nextBoolean())
        }

    private fun Random.filterIntent(): EditorIntent =
        when (nextInt(4)) {
            0 -> {
                FilterIntent.SetPoleCount(nextInt(-1, 3), small())
            }

            1 -> {
                FilterIntent.SetUnityGain(nextInt(-1, 3), value())
            }

            2 -> {
                FilterIntent.SetCoefficient(
                    nextInt(-1, 3),
                    small(),
                    nextInt(-1, 3),
                    value(),
                    value(),
                )
            }

            else -> {
                segmentIntent(SignalChainSlot.Filter)
            }
        }

    private fun Random.stateIntent(): EditorIntent =
        when (nextInt(10)) {
            0 -> {
                SlotIntent.SelectVoice(small())
            }

            1, 2 -> {
                SlotIntent.SetVoiceEnabled(small(), nextInt(4) != 0)
            }

            3 -> {
                if (nextBoolean()) SlotIntent.CopyVoice else SlotIntent.ResetVoice
            }

            4 -> {
                SlotIntent.SelectTargetByOffset(value())
            }

            5 -> {
                if (nextBoolean()) {
                    LoopIntent.SetLoopBegin(
                        value(),
                    )
                } else {
                    LoopIntent.SetLoopEnd(value())
                }
            }

            6 -> {
                ViewIntent.SetLoopCount(value())
            }

            7 -> {
                if (nextBoolean()) HistoryIntent.Undo else HistoryIntent.Redo
            }

            else -> {
                if (nextBoolean()) HistoryIntent.BeginGesture else HistoryIntent.EndGesture
            }
        }

    @Test
    fun randomIntentsAlwaysLeaveAnEncodablePatch() {
        val random = Random(20261003)
        var state = EditorState()
        var accepted = 0
        repeat(6000) {
            // An empty slot turns every edit into a no-op, so refill it most of the time.
            val intent =
                if (state.voice == null && random.nextInt(4) != 0) {
                    SlotIntent.SetVoiceEnabled(state.selectedVoice, true)
                } else {
                    random.intent()
                }
            val next = reduce(state, intent)
            if (next.patch != state.patch) {
                accepted++
                assertRoundTrips(next.patch)
            }
            state = next
        }
        assertRoundTrips(state.patch)
        check(accepted > 1000) { "fuzz accepted only $accepted edits" }
    }

    @Test
    fun patchBuiltFromEmptyByIntentsRoundTrips() {
        val state =
            EditorState().after(
                SlotIntent.SelectVoice(2),
                SlotIntent.SetVoiceEnabled(2, true),
                EnvelopeIntent.SetWaveform(SignalChainSlot.Pitch, Waveform.Square),
                EnvelopeIntent.SetStartValue(SignalChainSlot.Pitch, 440),
                EnvelopeIntent.SetEndValue(SignalChainSlot.Pitch, 880),
                EnvelopeIntent.SetSegments(
                    SignalChainSlot.Pitch,
                    listOf(Segment(0, 0), Segment(65535, 65535)),
                ),
                EnvelopeIntent.SetStartValue(SignalChainSlot.Volume, 65535),
                EnvelopeIntent.InsertSegment(SignalChainSlot.Volume, 0, Segment(32768, 65535)),
                EnvelopeIntent.SetWaveform(SignalChainSlot.VibratoRate, Waveform.Sine),
                EnvelopeIntent.SetEndValue(SignalChainSlot.VibratoDepth, 6553),
                EnvelopeIntent.SetWaveform(SignalChainSlot.TremoloRate, Waveform.Noise),
                EnvelopeIntent.SetWaveform(SignalChainSlot.GapOff, Waveform.Saw),
                EnvelopeIntent.SetWaveform(SignalChainSlot.GapOn, Waveform.Square),
                PartialIntent.TogglePartial(0),
                PartialIntent.TogglePartial(1),
                PartialIntent.SetPitchOffset(1, -120),
                PartialIntent.SetDelay(1, 30),
                VoiceIntent.SetDuration(800),
                VoiceIntent.SetOffset(100),
                VoiceIntent.SetEchoDelay(50),
                VoiceIntent.SetEchoFeedback(40),
                FilterIntent.SetPoleCount(0, 2),
                FilterIntent.SetPoleCount(1, 1),
                FilterIntent.SetCoefficient(0, 0, 0, 1200, 30_000),
                FilterIntent.SetCoefficient(1, 0, 1, 900, 20_000),
                FilterIntent.SetUnityGain(1, 4000),
                EnvelopeIntent.SetSegments(SignalChainSlot.Filter, listOf(Segment(65535, 65535))),
                LoopIntent.SetLoopBegin(100),
                LoopIntent.SetLoopEnd(600),
                SlotIntent.CopyVoice,
                SlotIntent.SelectVoice(5),
            )
        assertNull(state.statusHint)
        val voice = assertNotNull(state.patch.voices[2])
        assertEquals(voice, state.patch.voices[5])
        assertEquals(Coefficient(900, 20_000), voice.filter!!.feedback[0].end)
        assertEquals(listOf(Segment(65535, 65535)), voice.filter!!.modulationEnvelope)
        assertEquals(Waveform.Square, voice.gate!!.gapOn.waveform)
        assertEquals(2, voice.partials.size)
        assertRoundTrips(state.patch)
    }
}
