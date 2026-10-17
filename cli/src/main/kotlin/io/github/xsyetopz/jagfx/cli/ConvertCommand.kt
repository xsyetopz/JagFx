package io.github.xsyetopz.jagfx.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.engine.Wav
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import java.io.File

/** Root command: renders a `.synth` file, or decodes a `.jagvorbis` sample, to a 16-bit WAV file. */
internal class ConvertCommand : CliktCommand(name = "jagfx") {
    override val invokeWithoutSubcommand = true

    private val inputArgument by argument("input").optional()
    private val outputArgument by argument("output").optional()
    private val loopsArgument by argument("loopCount").int().optional()
    private val inputOption by option("-i", "--input", help = "Input .synth or .jagvorbis file")
    private val outputOption by option("-o", "--output", help = "Output .wav file")
    private val loopsOption by option("-l", "--loops", help = "Loop count").int()

    override fun help(context: Context) = "JagFx CLI"

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        val input = inputArgument ?: inputOption
        val output = outputArgument ?: outputOption
        val loops = loopsArgument ?: loopsOption
        if (input.isNullOrBlank() || output.isNullOrBlank()) {
            echo("Error: Input and output files are required.", err = true)
            echo("Usage: jagfx input.synth output.wav [loopCount]", err = true)
            echo("   or: jagfx -i input.synth -o output.wav [-l 4]", err = true)
            failure()
        }
        requireInputFile(input)
        requireFormats(input, output)
        if (extensionOf(input) == JAGVORBIS) {
            if (loops != null) {
                echo("Error: Loop count applies to $SYNTH input only", err = true)
                failure()
            }
            convert(input, output) {
                val sample = JagVorbisCodec.decode(it)
                Wav.encode(sample.decodePcm(), sampleRate = sample.sampleRate)
            }
        } else {
            val loopCount = loops ?: 1
            convert(input, output, "Loop count: $loopCount") {
                Wav.encode(Renderer.render(SynthCodec.decode(it).patch, loopCount))
            }
        }
    }

    private fun convert(
        input: String,
        output: String,
        vararg details: String,
        encode: (ByteArray) -> ByteArray,
    ) {
        echo("Converting: $input")
        echo("Output: $output")
        details.forEach(::echo)
        reportFailure("Error: Failed to process file: ") {
            File(output).writeBytes(encode(File(input).readBytes()))
        }
        echo("Successfully wrote $output")
    }

    private fun requireFormats(
        input: String,
        output: String,
    ) {
        val inputExtension = extensionOf(input)
        val outputExtension = extensionOf(output)
        if (inputExtension == ".tone") {
            echo("TBD: .tone file format not yet supported.")
            echo(" Mod Surma video showcased .tone files in a directory structure,")
            echo("but their binary format is unknown.")
            failure()
        }
        if (inputExtension != SYNTH && inputExtension != JAGVORBIS) {
            echo("Error: Unsupported input format: $inputExtension", err = true)
            echo("Supported formats: $SYNTH, $JAGVORBIS", err = true)
            failure()
        }
        if (outputExtension != WAV) {
            echo("Error: Unsupported output format: $outputExtension", err = true)
            echo("Supported formats: $WAV", err = true)
            failure()
        }
    }

    private companion object {
        const val SYNTH = ".synth"
        const val JAGVORBIS = ".jagvorbis"
        const val WAV = ".wav"
    }
}
