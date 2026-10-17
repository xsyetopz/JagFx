package io.github.xsyetopz.jagfx.core.engine

import io.github.xsyetopz.jagfx.core.format.SynthCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Spec §13 item 2: every target renders the reference synths exactly as the golden WAV files.
 * `references/wav/<name>.wav` is `<name>.synth` at loop count 1, and `<name>.loops<C>.wav` is
 * the same synth at loop count `C`.
 */
class GoldenWavTest {
    private val synths = ReferenceSynths.hex + CoverageSynths.hex

    @Test
    fun everyReferenceSynthHasAGoldenWav() {
        val names =
            ReferenceWavs.hex.keys
                .map { it.substringBefore('.') }
                .toSet()
        assertEquals(synths.keys, names)
        assertEquals(17, ReferenceWavs.hex.size)
    }

    @Test
    fun renderMatchesEveryGoldenWav() {
        val mismatches =
            ReferenceWavs.hex.mapNotNull { (name, wavHex) ->
                val synthName = name.substringBefore('.')
                val loopCount = name.substringAfter(".loops", "1").toInt()
                val synthHex = synths[synthName] ?: fail("no synth for $name")
                val patch = SynthCodec.decode(hexToBytes(synthHex)).patch
                val actual = Wav.encode(Renderer.render(patch, loopCount))
                firstDifference(hexToBytes(wavHex), actual)?.let { "$name: $it" }
            }
        assertEquals(emptyList(), mismatches)
    }

    private fun firstDifference(
        expected: ByteArray,
        actual: ByteArray,
    ): String? {
        val index = expected.indices.firstOrNull { it >= actual.size || expected[it] != actual[it] }
        return when {
            expected.size != actual.size -> {
                "size ${expected.size} != ${actual.size}"
            }

            index == null -> {
                null
            }

            else -> {
                val sample = (index - 44) / 2
                "first difference at byte $index (sample $sample), " +
                    "expected ${sampleAt(expected, sample)}, was ${sampleAt(actual, sample)}"
            }
        }
    }

    private fun sampleAt(
        wav: ByteArray,
        sample: Int,
    ): Int {
        val o = 44 + sample * 2
        return (wav[o].toInt() and 0xFF) or (wav[o + 1].toInt() shl 8)
    }
}
