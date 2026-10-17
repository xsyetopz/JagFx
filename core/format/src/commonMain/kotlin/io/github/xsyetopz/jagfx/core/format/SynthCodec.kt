package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Voice

/** A decoded patch with the warnings the decoder raised, such as truncation. */
public data class DecodeResult(
    val patch: Patch,
    val warnings: List<String>,
)

/** A decoded `.tone` voice with the warnings the decoder raised. */
public data class ToneDecodeResult(
    val voice: Voice,
    val warnings: List<String>,
)

/** A decoded `.filter` block with the warnings the decoder raised. `null` is "no filter". */
public data class FilterDecodeResult(
    val filter: Filter?,
    val warnings: List<String>,
)

/** Binary `.synth` codec as defined in `docs/synth-format-spec.md` §6 and §7. */
public object SynthCodec {
    /**
     * Decodes [bytes]. Truncated input decodes to a valid patch with a warning.
     *
     * @throws SynthFormatException for the errors of spec §6.3.
     */
    public fun decode(bytes: ByteArray): DecodeResult = SynthDecoder(bytes).decode()

    /**
     * Decodes [bytes] like [decode] and passes every field read to [trace], in input order.
     * Fields read before a [SynthFormatException] are still traced.
     */
    public fun decode(
        bytes: ByteArray,
        trace: (TracedField) -> Unit,
    ): DecodeResult = SynthDecoder(bytes, trace).decode()

    /**
     * Encodes [patch]. The result decodes back to [patch] with no warnings.
     *
     * @throws InvalidPatchException if [patch] breaks an invariant of spec §4.3.
     */
    public fun encode(patch: Patch): ByteArray = SynthEncoder.encode(patch)

    /**
     * Decodes a `.tone` file: one voice, with the bytes of a voice slot (spec §5 `Voice`).
     * Truncated input decodes with a warning, and bytes after the voice give a warning.
     *
     * @throws SynthFormatException for the errors of spec §6.3.
     */
    public fun decodeTone(bytes: ByteArray): ToneDecodeResult = SynthDecoder(bytes).decodeTone()

    /**
     * Encodes [voice] as a `.tone` file. The voice must be valid in a patch.
     *
     * @throws InvalidPatchException if [voice] breaks an invariant of spec §4.3, with slot `0`.
     */
    public fun encodeTone(voice: Voice): ByteArray = SynthEncoder.encodeTone(voice)

    /**
     * Decodes a `.filter` file: the filter field of a voice (spec §5 `OptFilter`).
     * Truncated input gives "no filter" with a warning.
     *
     * @throws SynthFormatException for the errors of spec §6.3.
     */
    public fun decodeFilter(bytes: ByteArray): FilterDecodeResult =
        SynthDecoder(bytes).decodeFilterOnly()

    /**
     * Encodes [filter] as a `.filter` file, or the byte `0x00` for "no filter". The filter must be
     * valid in a patch.
     *
     * @throws InvalidPatchException if [filter] breaks an invariant of spec §4.3, with slot `0`.
     */
    public fun encodeFilter(filter: Filter?): ByteArray = SynthEncoder.encodeFilter(filter)
}
