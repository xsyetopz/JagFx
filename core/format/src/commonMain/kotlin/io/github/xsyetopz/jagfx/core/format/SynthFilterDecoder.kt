package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.SynthLimits

/** Spec §6.7: the byte after `Offset` is `0` for no filter, or starts the filter. */
internal fun SynthReader.decodeFilter(path: String): Filter? =
    when {
        remaining <= 0 -> null
        peek(0) == 0 -> null.also { u8 { path } }
        else -> readFilter(path)
    }

/** Spec §6.8. */
private fun SynthReader.readFilter(path: String): Filter? {
    val offset = cursor
    val packed = u8 { "$path.poleCounts" }
    val feedforwardCount = packed shr NIBBLE_BITS
    val feedbackCount = packed and NIBBLE_MASK
    if (feedforwardCount > SynthLimits.MAX_POLES || feedbackCount > SynthLimits.MAX_POLES) {
        throw SynthFormatException(
            SynthFormatError.InvalidPoleCount,
            offset,
            "$path.poleCounts",
            "pole counts $feedforwardCount and $feedbackCount; " +
                "the maximum is ${SynthLimits.MAX_POLES}",
        )
    }
    val unityGain0 = u16 { "$path.unityGain0" }
    val unityGain1 = u16 { "$path.unityGain1" }
    val mask = u8 { "$path.modulationMask" }
    val feedforwardStart =
        List(feedforwardCount) { coefficient("$path.feedforward[$it].start") }
    val feedbackStart = List(feedbackCount) { coefficient("$path.feedback[$it].start") }
    val feedforward = decodePoleEnds("$path.feedforward", feedforwardStart, mask)
    val feedback = decodePoleEnds("$path.feedback", feedbackStart, mask shr NIBBLE_BITS)
    val envelope =
        if (mask != 0 || unityGain0 != unityGain1) {
            decodeSegments("$path.modulationEnvelope")
        } else {
            null
        }
    if (truncated) {
        warnings += "Filter truncated (discarding partial filter payload)"
        return null
    }
    val filter = Filter(unityGain0, unityGain1, feedforward, feedback, envelope)
    // Spec §6.8 step 9.
    return if (filter.needsModulationEnvelope) {
        filter
    } else {
        filter.copy(modulationEnvelope = null)
    }
}

/** Mask bit `p` of [directionMask] marks a stored end coefficient for pole `p`. */
private fun SynthReader.decodePoleEnds(
    path: String,
    starts: List<Coefficient>,
    directionMask: Int,
): List<Pole> =
    starts.mapIndexed { p, start ->
        val end = if (directionMask and (1 shl p) != 0) coefficient("$path[$p].end") else start
        Pole(start, end)
    }

private fun SynthReader.coefficient(path: String): Coefficient =
    Coefficient(u16 { "$path.phase" }, u16 { "$path.magnitude" })
