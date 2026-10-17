package io.github.xsyetopz.jagfx.core.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class WaveTablesTest {
    @Test
    fun sha256MatchesKnownVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.hex(ByteArray(0)),
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.hex(
                "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray(),
            ),
        )
    }

    /** Spec §8.2. */
    @Test
    fun sineTableHasTheSpecifiedHash() {
        assertEquals(
            "235defb8b6d7c1db0137caec395de88f299393de4c5a3f95a2b5536c525c3513",
            Sha256.hex(bigEndian(WaveTables.sine)),
        )
    }

    /** Spec §8.2. */
    @Test
    fun noiseTableHasTheSpecifiedHash() {
        assertEquals(
            "09b13db6e95d0057e3e6a6f342285ccd00d4b34cb08b7855b3ab02393e7f8f25",
            Sha256.hex(bigEndian(WaveTables.noise)),
        )
    }

    @Test
    fun pitchRatioDoublesAt120() {
        assertEquals(2.0, WaveTables.pitchRatio(120), 1e-12)
    }

    private fun bigEndian(values: IntArray): ByteArray =
        ByteArray(values.size * 4) { (values[it / 4] shr (24 - 8 * (it % 4))).toByte() }
}
