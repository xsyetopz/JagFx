package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Echo
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

private const val DEFAULT_VOICE_MS = 1000
private const val FULL_AMPLITUDE = 100

/** The voice that enabling an empty slot creates: a silent 1 s sine voice. */
internal val DefaultVoice: Voice =
    Voice(
        frequencyEnvelope = Envelope(Waveform.Sine, 0, 0, emptyList()),
        amplitudeEnvelope = Envelope(Waveform.Sine, 0, 0, emptyList()),
        pitchLfo = null,
        amplitudeLfo = null,
        gate = null,
        partials = emptyList(),
        echo = Echo.None,
        duration = Milliseconds(DEFAULT_VOICE_MS),
        offset = Milliseconds(0),
        filter = null,
    )

internal fun u16(value: Int): Int = value.coerceIn(0, SynthLimits.U16_MAX)

private fun usmart16(value: Int): Int = value.coerceIn(0, SynthLimits.USMART16_MAX)

internal fun reduceVoice(
    voice: Voice,
    intent: VoiceIntent,
): Voice =
    when (intent) {
        is VoiceIntent.SetDuration -> {
            voice.copy(duration = Milliseconds(u16(intent.ms)))
        }

        is VoiceIntent.SetOffset -> {
            voice.copy(offset = Milliseconds(u16(intent.ms)))
        }

        is VoiceIntent.SetEchoDelay -> {
            voice.copy(echo = voice.echo.copy(delay = Milliseconds(usmart16(intent.ms))))
        }

        is VoiceIntent.SetEchoFeedback -> {
            voice.copy(echo = voice.echo.copy(feedback = Percent(usmart16(intent.percent))))
        }
    }

/** Applies a partial edit. */
internal fun reducePartial(
    voice: Voice,
    intent: PartialIntent,
): Voice =
    when (intent) {
        is PartialIntent.TogglePartial -> {
            voice.copy(partials = togglePartial(voice.partials, intent.index))
        }

        is PartialIntent.SetAmplitude -> {
            voice.editPartial(intent.index) {
                it.copy(amplitude = Percent(intent.percent.coerceIn(1, SynthLimits.USMART16_MAX)))
            }
        }

        is PartialIntent.SetPitchOffset -> {
            voice.editPartial(intent.index) {
                it.copy(
                    pitchOffset =
                        intent.decicents.coerceIn(
                            SynthLimits.SMART16_MIN,
                            SynthLimits.SMART16_MAX,
                        ),
                )
            }
        }

        is PartialIntent.SetDelay -> {
            voice.editPartial(intent.index) { it.copy(delay = Milliseconds(usmart16(intent.ms))) }
        }
    }

private fun togglePartial(
    partials: List<Partial>,
    index: Int,
): List<Partial> =
    when {
        index in partials.indices -> {
            partials.filterIndexed { i, _ -> i != index }
        }

        index >= 0 && partials.size < SynthLimits.MAX_PARTIALS -> {
            partials + Partial(Percent(FULL_AMPLITUDE), 0, Milliseconds(0))
        }

        else -> {
            partials
        }
    }

private fun Voice.editPartial(
    index: Int,
    transform: (Partial) -> Partial,
): Voice =
    if (index in partials.indices) {
        copy(partials = partials.toMutableList().apply { set(index, transform(get(index))) })
    } else {
        this
    }
