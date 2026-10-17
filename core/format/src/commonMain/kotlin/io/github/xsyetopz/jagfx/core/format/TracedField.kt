package io.github.xsyetopz.jagfx.core.format

/**
 * One field the decoder read: [size] bytes at byte [offset] of the input, decoded to [value].
 *
 * [path] names the field after the model, for example `voices[0].frequencyEnvelope.startValue`.
 * Bytes with no model property use these paths:
 * - `voices[n]`: an empty slot marker.
 * - `voices[n].pitchLfo`, `.amplitudeLfo`, `.gate`, `.filter`: an absent marker.
 * - `<envelope>.segmentCount`, `filter.poleCounts`, `filter.modulationMask`: counts and masks.
 * - `voices[n].partials[k].amplitude` with value `0`: the partial list terminator.
 *
 * A read past the end of the input has an [offset] + [size] beyond the input size.
 */
public data class TracedField(
    val offset: Int,
    val size: Int,
    val path: String,
    val value: Int,
)
