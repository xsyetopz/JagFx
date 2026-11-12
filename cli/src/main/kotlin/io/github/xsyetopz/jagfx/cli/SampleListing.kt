package io.github.xsyetopz.jagfx.cli

import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisFormatException
import java.util.Locale

/**
 * Header listing of a `.jagvorbis` sample, in the comment style of [SynthListing].
 * Returns the lines, and `false` when the decoder rejected the input.
 */
internal fun sampleListing(
    path: String,
    bytes: ByteArray,
): Pair<List<String>, Boolean> {
    val lines = mutableListOf("; File: $path", "; Size: ${bytes.size} bytes", "")
    val sample =
        try {
            JagVorbisCodec.decode(bytes)
        } catch (e: JagVorbisFormatException) {
            lines += "; ERROR at 0x%04X: %s".format(e.offset, e.message)
            return lines to false
        }
    val seconds = sample.sampleCount.toDouble() / sample.sampleRate
    val direction = if (sample.pingPong) "ping-pong" else "forward"
    lines += "; Sample rate: ${sample.sampleRate} Hz"
    lines += String.format(Locale.ROOT, "; Samples: %d (%.3f s)", sample.sampleCount, seconds)
    lines += "; Loop: ${sample.loopBegin}..${sample.loopEnd}, $direction"
    lines += "; Setup: ${sample.setup.size} bytes"
    lines += "; Packets: ${sample.packetCount}"
    return lines to true
}
