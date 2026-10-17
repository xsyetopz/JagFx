package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.math.PI
import kotlin.math.sin

private const val PCM_SCALE = 32768.0

/**
 * Decodes the audio packets of one sample with the arithmetic of the game client:
 * every value is rounded to 32-bit float after each operation.
 */
internal class VorbisDecoder(
    private val setup: VorbisSetup,
) {
    private var previous = DoubleArray(setup.longBlocksize)
    private var previousBlocksize = 0
    private var previousCount = 0
    private var previousUnused = false

    fun decode(
        packets: List<ByteArray>,
        sampleCount: Int,
    ): ShortArray {
        val pcm = ShortArray(sampleCount)
        var written = 0
        for (packet in packets) {
            val samples = decodePacket(packet) ?: continue
            val count = minOf(samples.size, sampleCount - written)
            for (i in 0 until count) {
                val value = fround(PCM_SCALE * samples[i]).toInt()
                pcm[written + i] =
                    value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            written += count
        }
        return pcm
    }

    /** Decodes one packet and returns the samples that it completes, or `null` for the first packet. */
    private fun decodePacket(packet: ByteArray): DoubleArray? {
        val reader = BitReader(packet)
        val current = DoubleArray(setup.longBlocksize)
        reader.bit()
        val mode = setup.modes[reader.bits(ilog(setup.modes.size - 1))]
        val longBlock = mode.longBlock
        val n = if (longBlock) setup.longBlocksize else setup.shortBlocksize
        var previousLong = true
        var nextLong = true
        if (longBlock) {
            previousLong = reader.bit() != 0
            nextLong = reader.bit() != 0
        }
        val window =
            Window(
                n,
                setup.shortBlocksize,
                longBlock && !previousLong,
                longBlock && !nextLong,
            )

        val mapping = mode.mapping
        val floor = setup.floors[mapping.submapFloors[mapping.mux]]
        val floorY = floor.decode(reader, setup.codebooks)
        for (residue in mapping.submapResidues) {
            setup.residues[residue].decode(reader, current, n / 2, floorY == null, setup.codebooks)
        }
        if (floorY == null) {
            current.fill(0.0, n / 2, n)
        } else {
            floor.render(floorY, current, n / 2)
            inverseMdct(current, n, if (longBlock) setup.longTables else setup.shortTables)
            window.apply(current)
        }

        val output =
            if (previousBlocksize >
                0
            ) {
                overlap(current, n, window.leftStart, floorY != null)
            } else {
                null
            }
        previous = current
        previousBlocksize = n
        previousCount = window.rightEnd - n / 2
        previousUnused = floorY == null
        return output
    }

    private fun overlap(
        current: DoubleArray,
        n: Int,
        leftStart: Int,
        currentUsed: Boolean,
    ): DoubleArray {
        val output = DoubleArray((n + previousBlocksize) shr 2)
        if (!previousUnused) {
            for (i in 0 until previousCount) {
                output[i] = fround(output[i] + previous[previousBlocksize / 2 + i])
            }
        }
        if (currentUsed) {
            for (i in leftStart until n / 2) {
                val j = output.size - n / 2 + i
                output[j] = fround(output[j] + current[i])
            }
        }
        return output
    }

    /** The slopes of the window of one block; a short slope is used next to a short block. */
    private class Window(
        n: Int,
        shortBlocksize: Int,
        shortLeft: Boolean,
        shortRight: Boolean,
    ) {
        private val half = n / 2
        private val quarter = half / 2
        private val shortHalf = shortBlocksize / 2
        private val shortQuarter = shortHalf / 2
        val leftStart = if (shortLeft) quarter - shortQuarter else 0
        val leftEnd = if (shortLeft) shortQuarter + quarter else half
        val leftHalf = if (shortLeft) shortHalf else half
        val rightStart = if (shortRight) n - quarter - shortQuarter else half
        val rightEnd = if (shortRight) shortQuarter + (n - quarter) else n
        val rightHalf = if (shortRight) shortHalf else half

        fun apply(buffer: DoubleArray) {
            for (i in leftStart until leftEnd) {
                val s = fround(sin(PI * (HALF * ((HALF + (i - leftStart)) / leftHalf))))
                buffer[i] = fround(buffer[i] * fround(sin(s * ((PI / 2) * s))))
            }
            for (i in rightStart until rightEnd) {
                val s = fround(sin((PI / 2) + (HALF + (i - rightStart)) / rightHalf * HALF * PI))
                buffer[i] = fround(buffer[i] * fround(sin(s * (PI / 2) * s)))
            }
        }

        private companion object {
            const val HALF = 0.5
        }
    }
}
