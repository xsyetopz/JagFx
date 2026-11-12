package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Coefficient
import io.github.xsyetopz.jagfx.core.model.Echo
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Loop
import io.github.xsyetopz.jagfx.core.model.Milliseconds
import io.github.xsyetopz.jagfx.core.model.Partial
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Percent
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

internal fun hexToBytes(hex: String): ByteArray =
    ByteArray(hex.length / 2) {
        hex
            .substring(
                it * 2,
                it * 2 + 2,
            ).toInt(16)
            .toByte()
    }

internal fun referenceSynth(name: String): ByteArray =
    hexToBytes(ReferenceSynths.hex.getValue(name))

internal fun envelope(
    waveform: Waveform = Waveform.Sine,
    start: Int = 0,
    end: Int = 0,
    segments: List<Segment> = emptyList(),
): Envelope = Envelope(waveform, start, end, segments)

internal fun partial(
    amplitude: Int = 100,
    pitchOffset: Int = 0,
    delay: Int = 0,
): Partial = Partial(Percent(amplitude), pitchOffset, Milliseconds(delay))

internal fun pole(
    phase: Int,
    magnitude: Int,
    endPhase: Int = phase,
    endMagnitude: Int = magnitude,
): Pole = Pole(Coefficient(phase, magnitude), Coefficient(endPhase, endMagnitude))

internal fun filter(
    feedforward: List<Pole> = emptyList(),
    feedback: List<Pole> = emptyList(),
    unityGain0: Int = 0,
    unityGain1: Int = 0,
    modulationEnvelope: List<Segment>? = null,
): Filter = Filter(unityGain0, unityGain1, feedforward, feedback, modulationEnvelope)

internal fun voice(
    frequency: Envelope = envelope(start = 1000, end = 1000),
    amplitude: Envelope = envelope(start = 0, end = 0, segments = listOf(Segment(65535, 65535))),
    partials: List<Partial> = listOf(partial()),
    echo: Echo = Echo.None,
    filter: Filter? = null,
): Voice =
    Voice(
        frequencyEnvelope = frequency,
        amplitudeEnvelope = amplitude,
        pitchLfo = null,
        amplitudeLfo = null,
        gate = null,
        partials = partials,
        echo = echo,
        duration = Milliseconds(500),
        offset = Milliseconds(0),
        filter = filter,
    )

internal fun patchOf(
    vararg voices: Voice?,
    loop: Loop = Loop.None,
): Patch = Patch(voices.toList() + List(10 - voices.size) { null }, loop)

/** Byte offset of the filter presence byte of a one-voice patch in slot 0. */
internal fun filterOffset(single: Voice): Int =
    SynthCodec.encode(patchOf(single.copy(filter = null))).size - 1 - 9 - 4

internal fun roundTrip(patch: Patch): DecodeResult = SynthCodec.decode(SynthCodec.encode(patch))
