package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Voice
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/** The JSON text is not a patch, voice, or filter in the form of `schemas/synth.schema.json`. */
public class SynthJsonException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** JSON interchange form of a [Patch], as defined by `schemas/synth.schema.json`. */
public object SynthJson {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }

    /**
     * Encodes [patch] as indented JSON with LF line endings. Empty trailing voice slots and
     * fields with default values are omitted.
     */
    public fun encode(patch: Patch): String =
        json.encodeToString(PatchJson.serializer(), SynthJsonMapper.toJson(patch))

    /**
     * Decodes [text]. Missing trailing voice slots are empty.
     *
     * @throws SynthJsonException if [text] is not valid JSON, does not match the schema, or has
     *   a value outside its field domain.
     */
    public fun decode(text: String): Patch =
        SynthJsonMapper.fromJson(parse(PatchJson.serializer(), text))

    /** Encodes [voice] in the form of the schema's `voice` definition, the JSON form of a `.tone`. */
    public fun encodeVoice(voice: Voice): String =
        json.encodeToString(VoiceJson.serializer(), SynthJsonMapper.voiceToJson(voice))

    /**
     * Decodes [text] in the form of the schema's `voice` definition.
     *
     * @throws SynthJsonException as [decode] does.
     */
    public fun decodeVoice(text: String): Voice =
        SynthJsonMapper.voiceFromJson(parse(VoiceJson.serializer(), text))

    /**
     * Encodes [filter] in the form of the schema's `filter` definition, the JSON form of a
     * `.filter`. No filter is `null`.
     */
    public fun encodeFilter(filter: Filter?): String =
        json.encodeToString(
            FilterJson.serializer().nullable,
            filter?.let(SynthJsonMapper::filterToJson),
        )

    /**
     * Decodes [text] in the form of the schema's `filter` definition, or `null` for no filter.
     *
     * @throws SynthJsonException as [decode] does.
     */
    public fun decodeFilter(text: String): Filter? =
        parse(FilterJson.serializer().nullable, text)?.let(SynthJsonMapper::filterFromJson)

    private fun <T> parse(
        serializer: KSerializer<T>,
        text: String,
    ): T =
        try {
            json.decodeFromString(serializer, text)
        } catch (e: SerializationException) {
            throw SynthJsonException(e.message ?: "invalid synth JSON", e)
        }
}
