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

/** Decoder of spec §6. One instance decodes one input. */
internal class SynthDecoder(
    bytes: ByteArray,
    trace: ((TracedField) -> Unit)? = null,
) {
    private val reader = SynthReader(bytes, trace)

    fun decode(): DecodeResult {
        if (reader.startsWithGitLfsPointer()) {
            throw SynthFormatException(
                SynthFormatError.GitLfsPointer,
                0,
                "file",
                "input is a Git LFS pointer file",
            )
        }
        val voices = arrayOfNulls<Voice>(SynthLimits.VOICE_SLOTS)
        var slotsRead = 0
        // Spec §6.4 step 2. With R < 1, the remaining slots are empty, so the loop can stop.
        for (slot in voices.indices) {
            val marker = reader.peek(0)
            when {
                reader.remaining >= 1 && marker == 0 -> {
                    reader.u8 { "voices[$slot]" }
                }

                reader.remaining >= MIN_VOICE_BYTES -> {
                    voices[slot] = reader.decodeVoice("voices[$slot]")
                }

                else -> {
                    break
                }
            }
            slotsRead++
        }
        val hasLoop = reader.remaining >= LOOP_BYTES
        val loop =
            if (hasLoop) {
                Loop(
                    Milliseconds(reader.u16 { "loop.begin" }),
                    Milliseconds(reader.u16 { "loop.end" }),
                )
            } else {
                if (reader.remaining > 0) reader.truncated = true
                Loop.None
            }
        if (reader.truncated) reader.warnings += "File truncated at byte offset ${reader.cursor}"
        // Spec §6.4 step 4. After a slot stop byte, the input is truncated, not followed by data.
        if (slotsRead == SynthLimits.VOICE_SLOTS && hasLoop && reader.remaining > 0) {
            throw SynthFormatException(
                SynthFormatError.TrailingData,
                reader.cursor,
                "file",
                "${reader.remaining} bytes follow the loop region; " +
                    "a whole cache group stores its files one after another, so extract file 0",
            )
        }
        return DecodeResult(Patch(voices.asList(), loop), reader.warnings.toList())
    }

    /**
     * Decodes one voice as stored in a voice slot. No voice follows it, so a nonzero byte after
     * `Offset` always starts a filter.
     */
    fun decodeTone(): ToneDecodeResult {
        val voice = reader.decodeVoice("voice")
        return ToneDecodeResult(voice, finishedWarnings())
    }

    /** Decodes one filter field of a voice: `0x00` for no filter, or a filter body. */
    fun decodeFilterOnly(): FilterDecodeResult {
        if (reader.remaining <= 0) reader.truncated = true
        val filter = reader.decodeFilter("filter")
        return FilterDecodeResult(filter, finishedWarnings())
    }

    private fun finishedWarnings(): List<String> {
        if (reader.truncated) reader.warnings += "File truncated at byte offset ${reader.cursor}"
        if (reader.remaining >
            0
        ) {
            reader.warnings +=
                "Ignored ${reader.remaining} bytes after byte offset ${reader.cursor}"
        }
        return reader.warnings.toList()
    }
}

internal fun SynthReader.decodeVoice(path: String): Voice {
    val frequency = decodeEnvelope("$path.frequencyEnvelope")
    val amplitude = decodeEnvelope("$path.amplitudeEnvelope")
    val pitchLfo = decodePair("$path.pitchLfo")?.let { (rate, depth) -> Lfo(rate, depth) }
    val amplitudeLfo =
        decodePair("$path.amplitudeLfo")?.let { (rate, depth) -> Lfo(rate, depth) }
    val gate = decodePair("$path.gate")?.let { (off, on) -> Gate(off, on) }
    val partials = decodePartials("$path.partials")
    val echo =
        Echo(
            Milliseconds(usmart16 { "$path.echo.delay" }),
            Percent(usmart16 { "$path.echo.feedback" }),
        )
    val duration = Milliseconds(u16 { "$path.duration" })
    val offset = Milliseconds(u16 { "$path.offset" })
    val filter = decodeFilter("$path.filter")
    return Voice(
        frequency,
        amplitude,
        pitchLfo,
        amplitudeLfo,
        gate,
        partials,
        echo,
        duration,
        offset,
        filter,
    )
}

private fun SynthReader.decodeEnvelope(path: String): Envelope {
    val waveform = Waveform(u8 { "$path.waveform" })
    val startValue = s32 { "$path.startValue" }
    val endValue = s32 { "$path.endValue" }
    return Envelope(waveform, startValue, endValue, decodeSegments(path))
}

internal fun SynthReader.decodeSegments(path: String): List<Segment> =
    List(u8 { "$path.segmentCount" }) {
        Segment(
            u16 { "$path.segments[$it].duration" },
            u16 { "$path.segments[$it].targetLevel" },
        )
    }

private fun SynthReader.decodePair(path: String): Pair<Envelope, Envelope>? {
    if (peek(0) == 0) {
        u8 { path }
        return null
    }
    return decodeEnvelope("$path.first") to decodeEnvelope("$path.second")
}

/**
 * The partial list ends at amplitude `0`, traced as `partials[n].amplitude`.
 * The game client has room for [SynthLimits.MAX_PARTIALS] partials, so a longer list is an error.
 */
private fun SynthReader.decodePartials(path: String): List<Partial> {
    val partials = ArrayList<Partial>(SynthLimits.MAX_PARTIALS)
    while (remaining > 0) {
        val n = partials.size
        val offset = cursor
        val amplitude = usmart16 { "$path[$n].amplitude" }
        if (amplitude == 0) break
        if (n == SynthLimits.MAX_PARTIALS) {
            throw SynthFormatException(
                SynthFormatError.TooManyPartials,
                offset,
                "$path[$n].amplitude",
                "voice has more than ${SynthLimits.MAX_PARTIALS} partials",
            )
        }
        partials +=
            Partial(
                Percent(amplitude),
                smart16 { "$path[$n].pitchOffset" },
                Milliseconds(usmart16 { "$path[$n].delay" }),
            )
    }
    return partials
}
