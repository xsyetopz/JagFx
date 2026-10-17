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

/** Maps between the model and the wire shape of `schemas/synth.schema.json`. */
internal object SynthJsonMapper {
    fun toJson(patch: Patch): PatchJson =
        PatchJson(
            voices = patch.voices.map { it?.toJson() }.dropLastWhile { it == null },
            loop =
                patch.loop.takeIf { it != Loop.None }?.let {
                    LoopJson(
                        it.begin.value,
                        it.end.value,
                    )
                },
        )

    fun fromJson(json: PatchJson): Patch {
        if (json.voices.size > SynthLimits.VOICE_SLOTS) {
            invalid(
                "voices",
                "has ${json.voices.size} slots, the maximum is ${SynthLimits.VOICE_SLOTS}",
            )
        }
        val voices = json.voices.mapIndexed { slot, voice -> voice?.toModel("voices[$slot]") }
        val loop =
            json.loop?.let {
                field("loop") { Loop(Milliseconds(it.beginMs), Milliseconds(it.endMs)) }
            }
        return Patch(
            voices + List(SynthLimits.VOICE_SLOTS - voices.size) { null },
            loop ?: Loop.None,
        )
    }

    fun voiceToJson(voice: Voice): VoiceJson = voice.toJson()

    fun voiceFromJson(json: VoiceJson): Voice = json.toModel("voice")

    fun filterToJson(filter: Filter): FilterJson = filter.toJson()

    fun filterFromJson(json: FilterJson): Filter = json.toModel("filter")
}
