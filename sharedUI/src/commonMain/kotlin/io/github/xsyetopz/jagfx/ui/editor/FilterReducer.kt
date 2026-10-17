package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice

private val ZeroPole = Pole(Coefficient(0, 0), Coefficient(0, 0))

/** The filter that turning a filter on creates when none was kept: one zero feedforward pole. */
internal val DefaultFilter: Filter = Filter(0, 0, listOf(ZeroPole), emptyList(), null)

/** Applies a filter edit; edits other than adding poles need an existing filter. */
internal fun reduceFilter(
    voice: Voice,
    intent: FilterIntent,
): VoiceOutcome {
    val filter = voice.filter
    return when {
        intent is FilterIntent.SetFilter -> {
            VoiceOutcome.Changed(voice.copy(filter = intent.filter))
        }

        intent is FilterIntent.SetPoleCount -> {
            VoiceOutcome.Changed(
                voice.copy(filter = setPoleCount(filter, intent.channel, intent.count)),
            )
        }

        filter == null -> {
            VoiceOutcome.Rejected(RejectReason.MissingFilter)
        }

        intent is FilterIntent.SetUnityGain -> {
            VoiceOutcome.Changed(voice.copy(filter = normalize(setUnityGain(filter, intent))))
        }

        intent is FilterIntent.SetCoefficient -> {
            VoiceOutcome.Changed(voice.copy(filter = normalize(setCoefficient(filter, intent))))
        }

        else -> {
            VoiceOutcome.Changed(voice)
        }
    }
}

private fun setPoleCount(
    filter: Filter?,
    channel: Int,
    count: Int,
): Filter? {
    val n = count.coerceIn(0, SynthLimits.MAX_POLES)
    val base = filter ?: Filter(0, 0, emptyList(), emptyList(), null)
    val resized =
        when (channel) {
            0 -> base.copy(feedforward = resize(base.feedforward, n))
            1 -> base.copy(feedback = resize(base.feedback, n))
            else -> base
        }
    return if (filter == null && resized == base) null else normalize(resized)
}

private fun resize(
    poles: List<Pole>,
    count: Int,
): List<Pole> = poles.take(count) + List((count - poles.size).coerceAtLeast(0)) { ZeroPole }

private fun setUnityGain(
    filter: Filter,
    intent: FilterIntent.SetUnityGain,
): Filter =
    when (intent.index) {
        0 -> filter.copy(unityGain0 = u16(intent.value))
        1 -> filter.copy(unityGain1 = u16(intent.value))
        else -> filter
    }

private fun setCoefficient(
    filter: Filter,
    intent: FilterIntent.SetCoefficient,
): Filter {
    val coefficient = Coefficient(u16(intent.phase), u16(intent.magnitude))

    fun edit(poles: List<Pole>): List<Pole> =
        if (intent.pole in poles.indices) {
            val pole = poles[intent.pole]
            val edited =
                when (intent.phaseIndex) {
                    0 -> pole.copy(start = coefficient)
                    1 -> pole.copy(end = coefficient)
                    else -> pole
                }
            poles.toMutableList().apply { set(intent.pole, edited) }
        } else {
            poles
        }
    return when (intent.channel) {
        0 -> filter.copy(feedforward = edit(filter.feedforward))
        1 -> filter.copy(feedback = edit(filter.feedback))
        else -> filter
    }
}

/**
 * Removes a filter without poles (invariant I3) and keeps the modulation envelope present
 * exactly when the filter is modulated (invariant I4), creating an empty one when needed.
 */
private fun normalize(filter: Filter): Filter? =
    when {
        filter.feedforward.isEmpty() && filter.feedback.isEmpty() -> {
            null
        }

        filter.needsModulationEnvelope -> {
            filter.copy(
                modulationEnvelope =
                    filter.modulationEnvelope ?: emptyList(),
            )
        }

        else -> {
            filter.copy(modulationEnvelope = null)
        }
    }
