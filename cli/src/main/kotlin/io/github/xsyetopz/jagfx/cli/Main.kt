package io.github.xsyetopz.jagfx.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

/** The `jagfx` root command with every subcommand. */
internal fun jagFx() =
    ConvertCommand().subcommands(InspectCommand(), ToJsonCommand(), FromJsonCommand())

fun main(args: Array<String>) = jagFx().main(args)
