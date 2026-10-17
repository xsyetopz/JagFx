package io.github.xsyetopz.jagfx.core.format

import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.core.model.Pole
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform

/** Encoder of spec §7. */
internal object SynthEncoder {
    fun encode(patch: Patch): ByteArray {
        patch.voices.forEachIndexed { slot, voice ->
            if (voice !=
                null
            ) {
                checkVoiceInvariants(slot, voice)
            }
        }
        val sink = ByteSink()
        for (voice in patch.voices) {
            if (voice == null) sink.u8(0) else sink.voice(voice)
        }
        sink.u16(patch.loop.begin.value)
        sink.u16(patch.loop.end.value)
        return sink.toByteArray()
    }

    fun encodeTone(voice: Voice): ByteArray {
        checkVoiceInvariants(0, voice)
        return ByteSink().apply { voice(voice) }.toByteArray()
    }

    fun encodeFilter(filter: Filter?): ByteArray {
        val sink = ByteSink()
        if (filter == null) {
            sink.u8(0)
        } else {
            checkFilterInvariants(0, filter)
            sink.filter(filter)
        }
        return sink.toByteArray()
    }

    private fun ByteSink.voice(voice: Voice) {
        envelope(voice.frequencyEnvelope)
        envelope(voice.amplitudeEnvelope)
        pair(voice.pitchLfo?.let { it.rate to it.depth })
        pair(voice.amplitudeLfo?.let { it.rate to it.depth })
        pair(voice.gate?.let { it.gapOff to it.gapOn })
        for (partial in voice.partials) {
            usmart16(partial.amplitude.value)
            smart16(partial.pitchOffset)
            usmart16(partial.delay.value)
        }
        u8(0)
        usmart16(voice.echo.delay.value)
        usmart16(voice.echo.feedback.value)
        u16(voice.duration.value)
        u16(voice.offset.value)
        val filter = voice.filter
        if (filter == null) u8(0) else filter(filter)
    }

    private fun ByteSink.envelope(envelope: Envelope) {
        u8(envelope.waveform.id)
        s32(envelope.startValue)
        s32(envelope.endValue)
        segments(envelope.segments)
    }

    private fun ByteSink.segments(segments: List<Segment>) {
        u8(segments.size)
        for (segment in segments) {
            u16(segment.duration)
            u16(segment.targetLevel)
        }
    }

    private fun ByteSink.pair(pair: Pair<Envelope, Envelope>?) {
        if (pair == null) {
            u8(0)
        } else {
            envelope(pair.first)
            envelope(pair.second)
        }
    }

    private fun ByteSink.filter(filter: Filter) {
        u8((filter.feedforward.size shl NIBBLE_BITS) or filter.feedback.size)
        u16(filter.unityGain0)
        u16(filter.unityGain1)
        u8(filter.modulationMask)
        for (pole in filter.feedforward) coefficient(pole.start.phase, pole.start.magnitude)
        for (pole in filter.feedback) coefficient(pole.start.phase, pole.start.magnitude)
        modulatedEnds(filter.feedforward)
        modulatedEnds(filter.feedback)
        filter.modulationEnvelope?.let { segments(it) }
    }

    private fun ByteSink.modulatedEnds(poles: List<Pole>) {
        for (pole in poles) {
            if (pole.isModulated) coefficient(pole.end.phase, pole.end.magnitude)
        }
    }

    private fun ByteSink.coefficient(
        phase: Int,
        magnitude: Int,
    ) {
        u16(phase)
        u16(magnitude)
    }
}

/** Growable big-endian byte buffer. */
private class ByteSink {
    private var buffer = ByteArray(INITIAL_CAPACITY)
    private var size = 0

    fun u8(value: Int) {
        ensureCapacity(1)
        buffer[size++] = value.toByte()
    }

    fun u16(value: Int) {
        ensureCapacity(2)
        buffer[size++] = (value shr Byte.SIZE_BITS).toByte()
        buffer[size++] = value.toByte()
    }

    fun s32(value: Int) {
        ensureCapacity(Int.SIZE_BYTES)
        for (shift in Int.SIZE_BITS - Byte.SIZE_BITS downTo 0 step Byte.SIZE_BITS) {
            buffer[size++] = (value shr shift).toByte()
        }
    }

    /** Spec §6.2 `USmart16`. */
    fun usmart16(value: Int) {
        if (value < SMART_ONE_BYTE_LIMIT) u8(value) else u16(value + USMART16_BIAS)
    }

    /** Spec §6.2 `Smart16`. */
    fun smart16(value: Int) {
        if (value in
            smart16OneByteRange
        ) {
            u8(value + SMART16_ONE_BYTE_BIAS)
        } else {
            u16(value + SMART16_BIAS)
        }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    private fun ensureCapacity(extra: Int) {
        if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
    }

    private companion object {
        const val INITIAL_CAPACITY = 1024
    }
}
