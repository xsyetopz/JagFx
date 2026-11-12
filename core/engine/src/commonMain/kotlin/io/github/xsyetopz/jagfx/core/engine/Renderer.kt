package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.SynthLimits

/** Renders a [Patch] to mono signed 16-bit samples at [SAMPLE_RATE] Hz (spec §8, §9). */
public object Renderer {
    /** Samples per second of every rendered patch. */
    public const val SAMPLE_RATE: Int = 22050

    /**
     * Mixes the voices of [patch] and repeats its loop region.
     *
     * @param loopCount Number of times the loop region plays. `0` and `1` play it once.
     * @param slot The one voice slot to render, or `null` for all slots.
     * @return The samples, or an empty array when the patch has no sound.
     */
    public fun render(
        patch: Patch,
        loopCount: Int = 1,
        slot: Int? = null,
    ): ShortArray {
        require(loopCount >= 0) { "loopCount must be >= 0, was $loopCount" }
        require(slot == null || slot in 0 until SynthLimits.VOICE_SLOTS) {
            "slot must be in 0 until ${SynthLimits.VOICE_SLOTS}, was $slot"
        }
        val voices =
            patch.voices.withIndex().mapNotNull { (index, voice) ->
                voice?.takeIf { slot == null || index == slot }
            }
        val maxDuration = voices.maxOfOrNull { it.duration.value + it.offset.value } ?: 0
        if (maxDuration == 0) return ShortArray(0)

        val length = samplesOf(maxDuration)
        val loopStart = samplesOf(patch.loop.begin.value)
        val loopEnd = samplesOf(patch.loop.end.value)
        val loops = if (loopEnd > length || loopStart >= loopEnd) 0 else loopCount

        val mix = IntArray(length)
        for (voice in voices) {
            val offset = samplesOf(voice.offset.value)
            val samples = VoiceSynthesizer.synthesize(voice)
            val end = minOf(samples.size, length - offset)
            for (i in 0 until end) mix[offset + i] += samples[i]
        }

        val repeats = maxOf(0, loops - 1)
        val loopLength = loopEnd - loopStart
        val out = ShortArray(length + loopLength * repeats)
        for (i in 0 until loopEnd.coerceAtMost(length)) out[i] = clamp(mix[i])
        for (r in 1..repeats) {
            for (i in loopStart until loopEnd) out[i + loopLength * r] = clamp(mix[i])
        }
        for (i in loopEnd until length) out[i + loopLength * repeats] = clamp(mix[i])
        return out
    }

    private fun clamp(sample: Int): Short =
        sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
}
