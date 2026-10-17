package io.github.xsyetopz.jagfx.core.format

/** Decoder errors of `docs/synth-format-spec.md` §6.3. */
public enum class SynthFormatError(
    public val code: String,
) {
    /** The input is a Git LFS pointer file, not `.synth` data. */
    GitLfsPointer("E-LFS"),

    /** Bytes follow the loop region, as when the input is a whole cache group. */
    TrailingData("E-TRAILING"),

    /** A voice has more than five partials. */
    TooManyPartials("E-PARTIALS"),

    /** A filter direction has more than four poles. */
    InvalidPoleCount("E-POLES"),
}

/** The input cannot be decoded. [offset] is the byte offset of [field]. */
public class SynthFormatException(
    public val error: SynthFormatError,
    public val offset: Int,
    public val field: String,
    detail: String,
) : Exception("${error.code}: $detail ($field at byte offset $offset)")

/** Patch invariants of `docs/synth-format-spec.md` §4.3. */
public enum class PatchInvariant(
    public val description: String,
) {
    I1("frequency envelope waveform must not be Off"),
    I2("first envelope of an LFO or gate must not have waveform Off"),
    I3("filter must have at least one pole"),
    I4("filter modulation envelope must be present exactly when the filter is modulated"),
}

/** The patch breaks [invariant] in voice slot [slot], so it has no binary encoding. */
public class InvalidPatchException(
    public val invariant: PatchInvariant,
    public val slot: Int,
    detail: String,
) : Exception("${invariant.name} (${invariant.description}) in slot $slot: $detail")
