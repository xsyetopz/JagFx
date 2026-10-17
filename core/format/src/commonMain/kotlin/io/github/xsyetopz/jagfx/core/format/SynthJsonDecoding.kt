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

internal fun VoiceJson.toModel(path: String): Voice {
    val gate =
        when {
            gapOffEnvelope != null && gapOnEnvelope != null -> {
                Gate(
                    gapOffEnvelope.toModel("$path.gapOffEnvelope"),
                    gapOnEnvelope.toModel("$path.gapOnEnvelope"),
                )
            }

            gapOffEnvelope == null && gapOnEnvelope == null -> {
                null
            }

            else -> {
                invalid(
                    path,
                    "gapOffEnvelope and gapOnEnvelope must both be present or both be absent",
                )
            }
        }
    val partials = partials.orEmpty()
    if (partials.size > SynthLimits.MAX_PARTIALS) {
        invalid(
            "$path.partials",
            "has ${partials.size} items, the maximum is ${SynthLimits.MAX_PARTIALS}",
        )
    }
    return field(path) {
        Voice(
            frequencyEnvelope = frequencyEnvelope.toModel("$path.frequencyEnvelope"),
            amplitudeEnvelope = amplitudeEnvelope.toModel("$path.amplitudeEnvelope"),
            pitchLfo = pitchLfo?.toModel("$path.pitchLfo"),
            amplitudeLfo = amplitudeLfo?.toModel("$path.amplitudeLfo"),
            gate = gate,
            partials =
                partials.mapIndexed { i, partial ->
                    field("$path.partials[$i]") {
                        Partial(
                            Percent(partial.amplitude),
                            partial.pitchOffsetSemitones,
                            Milliseconds(partial.delay),
                        )
                    }
                },
            echo =
                echo?.let {
                    field(
                        "$path.echo",
                    ) { Echo(Milliseconds(it.delayMilliseconds), Percent(it.feedbackPercent)) }
                } ?: Echo.None,
            duration = Milliseconds(durationMs),
            offset = Milliseconds(offsetMs),
            filter = filter?.toModel("$path.filter"),
        )
    }
}

private fun LfoJson.toModel(path: String): Lfo =
    Lfo(
        rateEnvelope.toModel("$path.rateEnvelope"),
        modulationDepth.toModel("$path.modulationDepth"),
    )

private fun EnvelopeJson.toModel(path: String): Envelope {
    val waveform =
        waveformFromJsonName(waveform)
            ?: invalid(
                "$path.waveform",
                "\"$waveform\" is not one of ${Waveform.selectable.map { it.jsonName }} " +
                    "or \"$UNDEFINED_WAVEFORM_PREFIX<id>\" with id " +
                    "${Waveform.Noise.id + 1}..${SynthLimits.U8_MAX}",
            )
    return field(
        path,
    ) { Envelope(waveform, startValue, endValue, segments.toModel("$path.segments")) }
}

private fun List<SegmentJson>.toModel(path: String): List<Segment> =
    mapIndexed { i, segment ->
        field("$path[$i]") { Segment(segment.duration, segment.targetLevel) }
    }

internal fun FilterJson.toModel(path: String): Filter {
    requireSize(poleCounts, FILTER_DIRECTIONS, "$path.poleCounts")
    requireSize(unityGain, FILTER_DIRECTIONS, "$path.unityGain")
    val envelope =
        modulationEnvelope?.let {
            val envelopePath = "$path.modulationEnvelope"
            val parsed = it.toModel(envelopePath)
            if (parsed.waveform != Waveform.Off || parsed.startValue != 0 ||
                parsed.endValue != 0
            ) {
                invalid(
                    envelopePath,
                    "the binary format stores only segments, " +
                        "so waveform must be \"off\" and start and end values must be 0",
                )
            }
            parsed.segments
        }
    return field(path) {
        Filter(
            unityGain0 = unityGain[0],
            unityGain1 = unityGain[1],
            feedforward =
                poles(
                    poleCounts[0],
                    polePhase.feedforward,
                    poleMagnitude.feedforward,
                    "$path.feedforward",
                ),
            feedback =
                poles(
                    poleCounts[1],
                    polePhase.feedback,
                    poleMagnitude.feedback,
                    "$path.feedback",
                ),
            modulationEnvelope = envelope,
        )
    }
}

private fun poles(
    count: Int,
    phase: FilterPhasesJson,
    magnitude: FilterPhasesJson,
    path: String,
): List<Pole> {
    if (count !in
        0..SynthLimits.MAX_POLES
    ) {
        invalid(path, "pole count $count is not in 0..${SynthLimits.MAX_POLES}")
    }
    for ((name, values) in listOf(
        "polePhase.baseline" to phase.baseline,
        "polePhase.modulated" to phase.modulated,
        "poleMagnitude.baseline" to magnitude.baseline,
        "poleMagnitude.modulated" to magnitude.modulated,
    )) {
        requireSize(values, count, "$path.$name")
    }
    return List(count) { p ->
        field("$path[$p]") {
            Pole(
                start = Coefficient(phase.baseline[p], magnitude.baseline[p]),
                end = Coefficient(phase.modulated[p], magnitude.modulated[p]),
            )
        }
    }
}

private fun waveformFromJsonName(name: String): Waveform? =
    Waveform.selectable.firstOrNull { it.jsonName == name }
        ?: name
            .removePrefix(UNDEFINED_WAVEFORM_PREFIX)
            .takeIf { it != name && it.firstOrNull() != '0' }
            ?.toIntOrNull()
            ?.takeIf { it in Waveform.Noise.id + 1..SynthLimits.U8_MAX }
            ?.let(::Waveform)

private fun requireSize(
    values: List<Int>,
    size: Int,
    path: String,
) {
    if (values.size != size) invalid(path, "has ${values.size} items, expected $size")
}

/** Runs [build] and reports a model range check that fails as an error at [path]. */
internal inline fun <T> field(
    path: String,
    build: () -> T,
): T =
    try {
        build()
    } catch (e: SynthJsonException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw SynthJsonException("$path: ${e.message}", e)
    }

internal fun invalid(
    path: String,
    detail: String,
): Nothing = throw SynthJsonException("$path: $detail")

private const val FILTER_DIRECTIONS = 2
