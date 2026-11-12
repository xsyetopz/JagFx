package io.github.xsyetopz.jagfx.core.vorbis

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JagVorbisTest {
    @Test
    fun decodeThenEncodeGivesSameBytes() {
        for ((name, hex) in ReferenceSamples.hex) {
            val bytes = hexToBytes(hex)

            val encoded = JagVorbisCodec.encode(JagVorbisCodec.decode(bytes))

            assertContentEquals(bytes, encoded, name)
        }
    }

    @Test
    fun decodeReadsHeader() {
        val bytes = sample("12015")

        val sample = JagVorbisCodec.decode(bytes)

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
        val sample =
            JagVorbisCodec
                .decode(
                    sample("12015"),
                ).copy(loopBegin = 100, loopEnd = 900, pingPong = true)

        val decoded = JagVorbisCodec.decode(JagVorbisCodec.encode(sample))

        assertEquals(sample, decoded)
    }

    @Test
    fun decodeRejectsTrailingBytes() {
        val bytes = sample("12015") + byteArrayOf(0)

        val error = assertFailsWith<JagVorbisFormatException> { JagVorbisCodec.decode(bytes) }

        assertEquals(bytes.size - 1, error.offset)
    }

    @Test
    fun decodeRejectsTruncatedFiles() {
        val bytes = sample("12015").copyOf(10_000)

        val error = assertFailsWith<JagVorbisFormatException> { JagVorbisCodec.decode(bytes) }

        assertEquals(true, error.offset <= 10_000)
    }

    /** FNV-1a hashes of the big-endian PCM that the game client decodes from each sample. */
    @Test
    fun decodePcmMatchesGameClient() {
        val expected = mapOf("11102" to 0x20f98fc5303c7cabUL, "12015" to 0xea1064f064dc8d7bUL)
        for ((name, hash) in expected) {
            val sample = JagVorbisCodec.decode(sample(name))

            val pcm = sample.decodePcm()

            assertEquals(sample.sampleCount, pcm.size, name)
            assertEquals(hash, fnv1a(pcm), name)
        }
    }

    @Test
    fun decodePcmStopsAtSampleCount() {
        val full = JagVorbisCodec.decode(sample("12015"))
        val trimmed = full.copy(sampleCount = 1000)

        val pcm = trimmed.decodePcm()

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
