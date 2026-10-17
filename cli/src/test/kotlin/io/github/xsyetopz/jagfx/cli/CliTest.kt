package io.github.xsyetopz.jagfx.cli

import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The commands give the expected files, output, and exit status. */
class CliTest {
    private val references = File(System.getProperty("jagfx.references"))
    private val synths =
        references
            .resolve("synths")
            .listFiles { f -> f.extension == "synth" }!!
            .sortedBy { it.name }
    private val temp = createTempDirectory("jagfx-cli").toFile()

    @AfterTest
    fun deleteTemp() {
        temp.deleteRecursively()
    }

    private fun run(vararg argv: String): CliktCommandTestResult = jagFx().test(argv.asList())

    private fun referenceJson(synth: File): File =
        references.resolve("json/${synth.nameWithoutExtension}.json")

    @Test
    fun toJsonPrintsTheReferenceJson() {
        assertEquals(REFERENCE_SYNTHS, synths.size)
        for (synth in synths) {
            val result = run("to-json", synth.path)
            assertEquals(0, result.statusCode, synth.name)
            assertEquals(referenceJson(synth).readText(), result.stdout, synth.name)
        }
    }

    @Test
    fun toJsonWritesTheReferenceJsonWithoutTrailingNewline() {
        val synth = synths.first()
        val out = temp.resolve("out.json")
        val result = run("to-json", synth.path, out.path)
        assertEquals("Wrote ${out.path}\n", result.stdout)
        assertEquals(referenceJson(synth).readText().removeSuffix("\n"), out.readText())
    }

    @Test
    fun fromJsonWritesTheReferenceSynth() {
        for (synth in synths) {
            val out = temp.resolve(synth.name)
            val result = run("from-json", referenceJson(synth).path, out.path)
            assertEquals("Wrote ${out.path}\n", result.stdout, synth.name)
            assertContentEquals(synth.readBytes(), out.readBytes(), synth.name)
        }
    }

    @Test
    fun convertWritesTheReferenceWav() {
        for (synth in synths) {
            val out = temp.resolve("${synth.nameWithoutExtension}.wav")
            val result = run(synth.path, out.path)
            assertEquals(
                "Converting: ${synth.path}\nOutput: ${out.path}\nLoop count: 1\n" +
                    "Successfully wrote ${out.path}\n",
                result.stdout,
            )
            assertEquals(0, result.statusCode)
            val expected = references.resolve("wav/${out.name}").readBytes()
            assertContentEquals(expected, out.readBytes(), synth.name)
        }
    }

    @Test
    fun convertTakesOptionsAndLoopCount() {
        val synth = references.resolve("coverage/coverage_gate_echo_loop.synth")
        val positional = temp.resolve("a.wav")
        val options = temp.resolve("b.wav")
        assertEquals(0, run(synth.path, positional.path, "3").statusCode)
        assertEquals(0, run("-i", synth.path, "-o", options.path, "-l", "3").statusCode)
        val expected = references.resolve("wav/coverage_gate_echo_loop.loops3.wav").readBytes()
        assertContentEquals(expected, positional.readBytes())
        assertContentEquals(expected, options.readBytes())
    }

    @Test
    fun convertWritesASampleAtItsSampleRate() {
        // Arrange
        val sample = references.resolve("vorbis/12015.jagvorbis")
        val out = temp.resolve("12015.wav")
        val pcm = JagVorbisCodec.decode(sample.readBytes()).decodePcm()

        // Act
        val result = run(sample.path, out.path)

        // Assert
        assertEquals(0, result.statusCode, result.stderr)
        val wav = ByteBuffer.wrap(out.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(44_100, wav.getInt(SAMPLE_RATE_OFFSET))
        assertEquals(pcm.size * 2, wav.getInt(DATA_SIZE_OFFSET))
        val data = ShortArray(pcm.size) { wav.getShort(DATA_OFFSET + it * 2) }
        assertContentEquals(pcm, data)
    }

    @Test
    fun convertRejectsALoopCountForASample() {
        // Arrange
        val sample = references.resolve("vorbis/12015.jagvorbis")

        // Act
        val result = run(sample.path, temp.resolve("a.wav").path, "2")

        // Assert
        assertEquals(1, result.statusCode)
        assertEquals("Error: Loop count applies to .synth input only\n", result.stderr)
    }

    @Test
    fun inspectPrintsTheSampleHeader() {
        // Arrange
        val sample = references.resolve("vorbis/12015.jagvorbis")

        // Act
        val result = run("inspect", sample.path)

        // Assert
        assertEquals(
            "; File: ${sample.path}\n; Size: 10760 bytes\n\n" +
                "; Sample rate: 44100 Hz\n; Samples: 17664 (0.401 s)\n" +
                "; Loop: 0..17664, forward\n; Setup: 4032 bytes\n; Packets: 31\n",
            result.stdout,
        )
        assertEquals(0, result.statusCode)
    }

    @Test
    fun inspectReportsASampleError() {
        // Arrange
        val bad = temp.resolve("bad.jagvorbis").apply { writeBytes(ByteArray(3)) }

        // Act
        val result = run("inspect", bad.path)

        // Assert
        assertEquals(1, result.statusCode)
        assertEquals(
            "; ERROR at 0x0000: file ends 1 bytes early (at byte offset 0)",
            result.stdout.lines()[3],
        )
    }

    @Test
    fun inspectPrintsTheReferenceListing() {
        val listings = references.resolve("cli").listFiles()!!.sortedBy { it.name }
        assertEquals(REFERENCE_SYNTHS + 1, listings.size)
        for (listing in listings) {
            val name = listing.name.removeSuffix(".inspect.txt")
            val synth =
                synths.firstOrNull { it.nameWithoutExtension == name }
                    ?: references.resolve("coverage/$name.synth")
            val result = run("inspect", synth.path)
            val expected =
                listing.readText().replaceFirst("; File: $name.synth", "; File: ${synth.path}")
            assertEquals(expected, result.stdout, name)
            assertEquals(0, result.statusCode, name)
        }
    }

    @Test
    fun inspectReportsADecoderError() {
        // Ten empty slots and the loop region, then one byte more.
        val bad = temp.resolve("bad.synth")
        bad.writeBytes(ByteArray(15))
        val result = run("inspect", bad.path)
        assertEquals(1, result.statusCode)
        val lines = result.stdout.lines()
        val error = lines[lines.size - 2]
        assertTrue(error.startsWith("; ERROR at 0x000E: "), error)
    }

    @Test
    fun errorsMatchTheTwoXMessages() {
        val missing = temp.resolve("missing.synth").path
        val text = temp.resolve("a.txt").apply { writeText("") }.path
        val synth = synths.first().path
        val cases =
            listOf(
                arrayOf<String>() to
                    "Error: Input and output files are required.\n" +
                    "Usage: jagfx input.synth output.wav [loopCount]\n" +
                    "   or: jagfx -i input.synth -o output.wav [-l 4]\n",
                arrayOf(missing, "o.wav") to "Error: Input file not found: $missing\n",
                arrayOf(text, "o.wav") to
                    "Error: Unsupported input format: .txt\nSupported formats: .synth, .jagvorbis\n",
                arrayOf(synth, "o.mp3") to
                    "Error: Unsupported output format: .mp3\nSupported formats: .wav\n",
                arrayOf("inspect", missing) to "Error: File not found: $missing\n",
                arrayOf("to-json", missing) to "Error: Input file not found: $missing\n",
                arrayOf("from-json", text, "o.synth") to "Error: ",
            )
        for ((argv, stderr) in cases) {
            val result = run(*argv)
            assertEquals(1, result.statusCode, argv.joinToString(" "))
            assertEquals(stderr, result.stderr.take(stderr.length), argv.joinToString(" "))
        }
    }

    @Test
    fun toneInputPrintsTheUnsupportedNotice() {
        val tone = temp.resolve("a.TONE").apply { writeText("") }
        val result = run(tone.path, "o.wav")
        assertEquals(1, result.statusCode)
        assertEquals(
            "TBD: .tone file format not yet supported.\n" +
                " Mod Surma video showcased .tone files in a directory structure,\n" +
                "but their binary format is unknown.\n",
            result.stdout,
        )
    }

    private companion object {
        const val REFERENCE_SYNTHS = 15
        const val SAMPLE_RATE_OFFSET = 24
        const val DATA_SIZE_OFFSET = 40
        const val DATA_OFFSET = 44
    }
}
