package io.github.xsyetopz.jagfx.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.format.SynthJson
import java.io.File

internal class ToJsonCommand : CliktCommand(name = "to-json") {
    private val input by argument("input").help("Path to input .synth file")
    private val output by argument("output")
        .help("Path to output .json file (stdout if omitted)")
        .optional()

    override fun help(context: Context) = "Convert .synth binary to JSON format"

    override fun run() {
        requireInputFile(input)
        val json =
            reportFailure(
                "Error: ",
            ) { SynthJson.encode(SynthCodec.decode(File(input).readBytes()).patch) }
        val target = output
        if (target.isNullOrBlank()) {
            echo(json)
        } else {
            reportFailure("Error: ") { File(target).writeText(json) }
            echo("Wrote $target")
        }
    }
}

internal class FromJsonCommand : CliktCommand(name = "from-json") {
    private val input by argument("input").help("Path to input .json file")
    private val output by argument("output").help("Path to output .synth file")

    override fun help(context: Context) = "Convert JSON to .synth binary format"

    override fun run() {
        requireInputFile(input)
        reportFailure("Error: ") {
            File(output).writeBytes(SynthCodec.encode(SynthJson.decode(File(input).readText())))
        }
        echo("Wrote $output")
    }
}
