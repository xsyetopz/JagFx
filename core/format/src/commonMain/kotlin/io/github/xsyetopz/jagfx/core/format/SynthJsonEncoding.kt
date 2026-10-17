package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Echo
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Gate
import io.github.xsyetopz.jagfx.core.model.Lfo
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

internal fun Voice.toJson(): VoiceJson =
    VoiceJson(
        frequencyEnvelope = frequencyEnvelope.toJson(),
        amplitudeEnvelope = amplitudeEnvelope.toJson(),
        pitchLfo = pitchLfo?.toJson(),
        amplitudeLfo = amplitudeLfo?.toJson(),
        gapOffEnvelope = gate?.gapOff?.toJson(),
        gapOnEnvelope = gate?.gapOn?.toJson(),
        partials =
            partials.takeIf { it.isNotEmpty() }?.map {
                PartialJson(it.amplitude.value, it.pitchOffset, it.delay.value)
            },
        echo =
            echo.takeIf { it != Echo.None }?.let {
                EchoJson(
                    it.delay.value,
                    it.feedback.value,
                )
            },
        durationMs = duration.value,
        offsetMs = offset.value,
        filter = filter?.toJson(),
    )

internal fun Lfo.toJson(): LfoJson = LfoJson(rate.toJson(), depth.toJson())

internal fun Envelope.toJson(): EnvelopeJson =
    EnvelopeJson(waveform.jsonName, startValue, endValue, segments.toJson())

internal fun List<Segment>.toJson(): List<SegmentJson> =
    map {
        SegmentJson(it.duration, it.targetLevel)
    }

/**
 * The binary filter modulation envelope has segments only. The JSON form is a full envelope, so
 * the mapper writes waveform `off` and zero start and end values.
 */
internal fun Filter.toJson(): FilterJson =
    FilterJson(
        poleCounts = listOf(feedforward.size, feedback.size),
        unityGain = listOf(unityGain0, unityGain1),
        polePhase =
            FilterCoefficientsJson(
                feedforward = feedforward.phases { it.phase },
                feedback = feedback.phases { it.phase },
            ),
        poleMagnitude =
            FilterCoefficientsJson(
                feedforward = feedforward.phases { it.magnitude },
                feedback = feedback.phases { it.magnitude },
            ),
        modulationEnvelope =
            modulationEnvelope?.let {
                EnvelopeJson(Waveform.Off.jsonName, 0, 0, it.toJson())
            },
    )

private inline fun List<Pole>.phases(value: (Coefficient) -> Int): FilterPhasesJson =
    FilterPhasesJson(baseline = map { value(it.start) }, modulated = map { value(it.end) })

/** A waveform id with no shape in the game client is named [UNDEFINED_WAVEFORM_PREFIX] + id. */
internal val Waveform.jsonName: String
    get() = if (isUndefined) "$UNDEFINED_WAVEFORM_PREFIX$id" else toString().lowercase()

internal const val UNDEFINED_WAVEFORM_PREFIX = "undefined"
