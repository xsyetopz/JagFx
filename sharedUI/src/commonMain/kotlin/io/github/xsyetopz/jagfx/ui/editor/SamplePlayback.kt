package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.vorbis.JagVorbis

/**
 * The PCM to play for [sample], from its decoded audio [pcm], with [passes] passes of the loop
 * region (`1` plays without looping), in the game client's loop order.
 *
 * Without a loop, or when the loop region is empty: samples `0 until sampleCount`. With [passes]
 * above `1`: `0 until loopEnd`, then the loop region `loopBegin until loopEnd` repeated
 * `passes - 1` more times, then `loopEnd until sampleCount`. A ping-pong sample plays every other
 * repetition reversed, starting with a reversed one; when the last repetition is reversed, it runs
 * on backward to sample `0` and playback stops there, with no tail.
 *
 * A loop end past the sample count is cut to the count.
 */
internal fun samplePlaybackPcm(
    pcm: ShortArray,
    sample: JagVorbis,
    passes: Int,
): ShortArray {
    val count = minOf(sample.sampleCount, pcm.size)
    val end = sample.loopEnd.coerceIn(0, count)
    val begin = sample.loopBegin.coerceIn(0, end)
    val repeats = if (passes > 1 && begin < end) passes - 1 else 0
    if (repeats == 0) return pcm.copyOf(count)
    val region = end - begin
    val endsReversed = sample.pingPong && repeats % 2 == 1
    val middle = if (endsReversed) repeats - 1 else repeats
    val tail = if (endsReversed) end else count - end
    val out = ShortArray(end + middle * region + tail)
    pcm.copyInto(out, 0, 0, end)
    var position = end
    for (repeat in 1..middle) {
        if (sample.pingPong && repeat % 2 == 1) {
            for (i in 0 until region) out[position + i] = pcm[end - 1 - i]
        } else {
            pcm.copyInto(out, position, begin, end)
        }
        position += region
    }
    if (endsReversed) {
        for (i in 0 until end) out[position + i] = pcm[end - 1 - i]
    } else {
        pcm.copyInto(out, position, end, count)
    }
    return out
}
