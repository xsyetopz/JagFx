package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Gate
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

private val pairFirsts =
    setOf(SignalChainSlot.VibratoRate, SignalChainSlot.TremoloRate, SignalChainSlot.GapOff)

/** Applies [intent] to [voice]. */
internal fun reduceEnvelope(
    voice: Voice,
    intent: EnvelopeIntent,
): VoiceOutcome {
    val target = intent.target
    return when (intent) {
        is EnvelopeIntent.SetWaveform -> {
            setWaveform(voice, target, intent.waveform)
        }

        is EnvelopeIntent.SetStartValue -> {
            editEnvelope(voice, target) {
                it.copy(startValue = valueOf(target, it.startValue, intent.value))
            }
        }

        is EnvelopeIntent.SetEndValue -> {
            editEnvelope(
                voice,
                target,
            ) { it.copy(endValue = valueOf(target, it.endValue, intent.value)) }
        }

        is EnvelopeIntent.InsertSegment -> {
            editSegments(voice, target) { segments ->
                if (segments.size >= SynthLimits.MAX_SEGMENTS) {
                    segments
                } else {
                    segments.toMutableList().apply {
                        add(
                            intent.index.coerceIn(0, size),
                            intent.segment,
                        )
                    }
                }
            }
        }

        is EnvelopeIntent.SetSegment -> {
            editSegments(voice, target) { segments ->
                if (intent.index in segments.indices) {
                    segments.toMutableList().apply { set(intent.index, intent.segment) }
                } else {
                    segments
                }
            }
        }

        is EnvelopeIntent.RemoveSegment -> {
            editSegments(voice, target) { segments ->
                if (intent.index in segments.indices) {
                    segments.toMutableList().apply { removeAt(intent.index) }
                } else {
                    segments
                }
            }
        }

        is EnvelopeIntent.SetSegments -> {
            editSegments(voice, target) { intent.segments.take(SynthLimits.MAX_SEGMENTS) }
        }
    }
}

/** [value] when [target] has start and end values, which take any `i32` (spec §4); else [old]. */
private fun valueOf(
    target: SignalChainSlot,
    old: Int,
    value: Int,
): Int = if (target.range != null) value else old

private fun setWaveform(
    voice: Voice,
    target: SignalChainSlot,
    waveform: Waveform,
): VoiceOutcome =
    when {
        target == SignalChainSlot.Pitch && waveform == Waveform.Off -> {
            VoiceOutcome.Rejected(RejectReason.PitchWaveformOff)
        }

        target in pairFirsts && waveform == Waveform.Off -> {
            VoiceOutcome.Changed(voice.withPair(target, null))
        }

        target in pairFirsts && voice.pairOf(target) == null -> {
            val pair =
                Envelope(waveform, 0, 0, emptyList()) to Envelope(Waveform.Off, 0, 0, emptyList())
            VoiceOutcome.Changed(voice.withPair(target, pair))
        }

        else -> {
            editEnvelope(voice, target) { it.copy(waveform = waveform) }
        }
    }

/** Replaces the segment list of [target]; the filter target edits the modulation envelope. */
private fun editSegments(
    voice: Voice,
    target: SignalChainSlot,
    transform: (List<Segment>) -> List<Segment>,
): VoiceOutcome {
    if (target != SignalChainSlot.Filter) {
        return editEnvelope(voice, target) { it.copy(segments = transform(it.segments)) }
    }
    val filter = voice.filter
    val segments = filter?.modulationEnvelope
    return when {
        filter == null -> {
            VoiceOutcome.Rejected(RejectReason.MissingFilter)
        }

        segments == null -> {
            VoiceOutcome.Rejected(RejectReason.MissingEnvelope)
        }

        else -> {
            VoiceOutcome.Changed(
                voice.copy(filter = filter.copy(modulationEnvelope = transform(segments))),
            )
        }
    }
}

/** Applies [transform] to the envelope of [target]. Views without an envelope stay unchanged. */
private fun editEnvelope(
    voice: Voice,
    target: SignalChainSlot,
    transform: (Envelope) -> Envelope,
): VoiceOutcome {
    val pair = voice.pairOf(target)
    return when (target) {
        SignalChainSlot.Pitch -> {
            VoiceOutcome.Changed(voice.copy(frequencyEnvelope = transform(voice.frequencyEnvelope)))
        }

        SignalChainSlot.Volume -> {
            VoiceOutcome.Changed(voice.copy(amplitudeEnvelope = transform(voice.amplitudeEnvelope)))
        }

        SignalChainSlot.VibratoRate, SignalChainSlot.TremoloRate, SignalChainSlot.GapOff -> {
            if (pair == null) {
                VoiceOutcome.Rejected(RejectReason.MissingEnvelope)
            } else {
                VoiceOutcome.Changed(voice.withPair(target, transform(pair.first) to pair.second))
            }
        }

        SignalChainSlot.VibratoDepth, SignalChainSlot.TremoloDepth, SignalChainSlot.GapOn -> {
            if (pair == null) {
                VoiceOutcome.Rejected(RejectReason.MissingEnvelope)
            } else {
                VoiceOutcome.Changed(voice.withPair(target, pair.first to transform(pair.second)))
            }
        }

        SignalChainSlot.Filter,
        SignalChainSlot.PoleZero,
        SignalChainSlot.Output,
        SignalChainSlot.Bode,
        -> {
            VoiceOutcome.Changed(voice)
        }
    }
}

/** The LFO or gate envelope pair that holds [target], or `null`. */
private fun Voice.pairOf(target: SignalChainSlot): Pair<Envelope, Envelope>? =
    when (target) {
        SignalChainSlot.VibratoRate, SignalChainSlot.VibratoDepth -> {
            pitchLfo?.let { it.rate to it.depth }
        }

        SignalChainSlot.TremoloRate, SignalChainSlot.TremoloDepth -> {
            amplitudeLfo?.let { it.rate to it.depth }
        }

        SignalChainSlot.GapOff, SignalChainSlot.GapOn -> {
            gate?.let { it.gapOff to it.gapOn }
        }

        else -> {
            null
        }
    }

private fun Voice.withPair(
    target: SignalChainSlot,
    pair: Pair<Envelope, Envelope>?,
): Voice =
    when (target) {
        SignalChainSlot.VibratoRate, SignalChainSlot.VibratoDepth -> {
            copy(pitchLfo = pair?.let { Lfo(it.first, it.second) })
        }

        SignalChainSlot.TremoloRate, SignalChainSlot.TremoloDepth -> {
            copy(amplitudeLfo = pair?.let { Lfo(it.first, it.second) })
        }

        SignalChainSlot.GapOff, SignalChainSlot.GapOn -> {
            copy(gate = pair?.let { Gate(it.first, it.second) })
        }

        else -> {
            this
        }
    }
