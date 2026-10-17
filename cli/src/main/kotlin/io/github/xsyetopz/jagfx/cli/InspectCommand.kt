package io.github.xsyetopz.jagfx.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import java.io.File

internal class InspectCommand : CliktCommand(name = "inspect") {
    private val file by argument("file").help("Path to .synth or .jagvorbis file")

    override fun help(context: Context) = "Inspect synth or sample file structure"

    override fun run() {
        if (!File(file).isFile) {
            echo("Error: File not found: $file", err = true)
            failure()
        }
        val bytes = File(file).readBytes()
        val (lines, complete) =
            if (extensionOf(file) == ".jagvorbis") {
                sampleListing(file, bytes)
            } else {
                SynthListing.of(file, bytes).let { it.lines to it.complete }
            }
        lines.forEach(::echo)
        if (!complete) failure()
    }
}
