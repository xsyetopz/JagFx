package io.github.xsyetopz.jagfx.core.model

/** Field domains of the `.synth` format (`docs/synth-format-spec.md` §3, §4.1, §11). */
public object SynthLimits {
    public const val VOICE_SLOTS: Int = 10
    public const val MAX_PARTIALS: Int = 5
    public const val MAX_SEGMENTS: Int = 255
    public const val MAX_POLES: Int = 4

    public const val U8_MAX: Int = 0xFF
    public const val U16_MAX: Int = 0xFFFF
    public const val USMART16_MAX: Int = 32767
    public const val SMART16_MIN: Int = -16384
    public const val SMART16_MAX: Int = 16383

    internal val u8: IntRange = 0..U8_MAX
    internal val u16: IntRange = 0..U16_MAX
    internal val usmart16: IntRange = 0..USMART16_MAX
    internal val smart16: IntRange = SMART16_MIN..SMART16_MAX
}

internal fun requireIn(
    range: IntRange,
    value: Int,
    field: String,
) {
    require(value in range) { "$field must be in $range, was $value" }
}
