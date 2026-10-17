package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JagVorbisTest {
    @Test
    fun decodeThenEncodeGivesTheSameBytes() {
        for ((name, hex) in ReferenceSamples.hex) {
            // Arrange
            val bytes = hexToBytes(hex)

            // Act
            val encoded = JagVorbisCodec.encode(JagVorbisCodec.decode(bytes))

            // Assert
            assertContentEquals(bytes, encoded, name)
        }
    }

    @Test
    fun decodeReadsTheHeader() {
        // Arrange
        val bytes = sample("12015")

        // Act
        val sample = JagVorbisCodec.decode(bytes)

        // Assert
        assertEquals(44_100, sample.sampleRate)
        assertEquals(17_664, sample.sampleCount)
        assertEquals(0, sample.loopBegin)
        assertEquals(17_664, sample.loopEnd)
        assertEquals(false, sample.pingPong)
        assertEquals(4032, sample.setup.size)
        assertEquals(31, sample.packetCount)
    }

    @Test
    fun pingPongLoopEndIsStoredInverted() {
        // Arrange
        val sample =
            JagVorbisCodec
                .decode(
                    sample("12015"),
                ).copy(loopBegin = 100, loopEnd = 900, pingPong = true)

        // Act
        val decoded = JagVorbisCodec.decode(JagVorbisCodec.encode(sample))

        // Assert
        assertEquals(sample, decoded)
    }

    @Test
    fun decodeRejectsTrailingBytes() {
        // Arrange
        val bytes = sample("12015") + byteArrayOf(0)

        // Act
        val error = assertFailsWith<JagVorbisFormatException> { JagVorbisCodec.decode(bytes) }

        // Assert
        assertEquals(bytes.size - 1, error.offset)
    }

    @Test
    fun decodeRejectsTruncatedFiles() {
        // Arrange
        val bytes = sample("12015").copyOf(10_000)

        // Act
        val error = assertFailsWith<JagVorbisFormatException> { JagVorbisCodec.decode(bytes) }

        // Assert
        assertEquals(true, error.offset <= 10_000)
    }

    /** FNV-1a hashes of the big-endian PCM that the game client decodes from each sample. */
    @Test
    fun decodePcmMatchesTheGameClient() {
        val expected = mapOf("11102" to 0x20f98fc5303c7cabUL, "12015" to 0xea1064f064dc8d7bUL)
        for ((name, hash) in expected) {
            // Arrange
            val sample = JagVorbisCodec.decode(sample(name))

            // Act
            val pcm = sample.decodePcm()

            // Assert
            assertEquals(sample.sampleCount, pcm.size, name)
            assertEquals(hash, fnv1a(pcm), name)
        }
    }

    @Test
    fun decodePcmStopsAtTheSampleCount() {
        // Arrange
        val full = JagVorbisCodec.decode(sample("12015"))
        val trimmed = full.copy(sampleCount = 1000)

        // Act
        val pcm = trimmed.decodePcm()

        // Assert
        assertContentEquals(full.decodePcm().copyOf(1000), pcm)
    }

    private fun sample(name: String): ByteArray = hexToBytes(ReferenceSamples.hex.getValue(name))

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun fnv1a(pcm: ShortArray): ULong {
        var hash = 0xcbf29ce484222325UL
        for (value in pcm) {
            for (byte in intArrayOf(value.toInt() shr 8, value.toInt())) {
                hash = (hash xor (byte and 0xFF).toULong()) * 0x100000001b3UL
            }
        }
        return hash
    }
}
