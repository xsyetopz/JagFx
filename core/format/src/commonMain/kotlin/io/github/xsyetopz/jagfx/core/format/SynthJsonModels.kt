package io.github.xsyetopz.jagfx.core.format

import kotlinx.serialization.Serializable

// Wire shape of `schemas/synth.schema.json`. Property order is the output order.
// A property with a default value is omitted from the output when it has that value.

@Serializable
internal data class PatchJson(
    val voices: List<VoiceJson?>,
    val loop: LoopJson? = null,
)

@Serializable
internal data class VoiceJson(
    val frequencyEnvelope: EnvelopeJson,
    val amplitudeEnvelope: EnvelopeJson,
    val pitchLfo: LfoJson? = null,
    val amplitudeLfo: LfoJson? = null,
    val gapOffEnvelope: EnvelopeJson? = null,
    val gapOnEnvelope: EnvelopeJson? = null,
    val partials: List<PartialJson>? = null,
    val echo: EchoJson? = null,
    val durationMs: Int,
    val offsetMs: Int = 0,
    val filter: FilterJson? = null,
)

@Serializable
internal data class EnvelopeJson(
    val waveform: String,
    val startValue: Int,
    val endValue: Int,
    val segments: List<SegmentJson>,
)

@Serializable
internal data class SegmentJson(
    val duration: Int,
    val targetLevel: Int,
)

@Serializable
internal data class LfoJson(
    val rateEnvelope: EnvelopeJson,
    val modulationDepth: EnvelopeJson,
)

@Serializable
internal data class PartialJson(
    val amplitude: Int,
    val pitchOffsetSemitones: Int,
    val delay: Int = 0,
)

@Serializable
internal data class EchoJson(
    val delayMilliseconds: Int,
    val feedbackPercent: Int,
)

@Serializable
internal data class FilterJson(
    val poleCounts: List<Int>,
    val unityGain: List<Int>,
    val polePhase: FilterCoefficientsJson,
    val poleMagnitude: FilterCoefficientsJson,
    val modulationEnvelope: EnvelopeJson? = null,
)

@Serializable
internal data class FilterCoefficientsJson(
    val feedforward: FilterPhasesJson,
    val feedback: FilterPhasesJson,
)

@Serializable
internal data class FilterPhasesJson(
    val baseline: List<Int>,
    val modulated: List<Int>,
)

@Serializable
internal data class LoopJson(
    val beginMs: Int,
    val endMs: Int,
)
