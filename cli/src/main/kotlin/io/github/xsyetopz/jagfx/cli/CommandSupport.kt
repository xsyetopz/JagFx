package io.github.xsyetopz.jagfx.cli

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import io.github.xsyetopz.jagfx.core.format.InvalidPatchException
import io.github.xsyetopz.jagfx.core.format.SynthFormatException
import io.github.xsyetopz.jagfx.core.format.SynthJsonException
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisFormatException
import java.io.File
import java.io.IOException

/** Ends the command with exit status 1; the command has already printed why. */
internal fun failure(): Nothing = throw ProgramResult(1)

/** Prints the not-found error and fails unless [path] is an existing file. */
internal fun BaseCliktCommand<*>.requireInputFile(path: String) {
    if (!File(path).isFile) {
        echo("Error: Input file not found: $path", err = true)
        failure()
    }
}

/**
 * Runs [block], printing `$prefix<message>` to stderr and failing when [block] throws a decode,
 * encode, validation, or I/O error. Other exceptions propagate.
 */
internal inline fun <T> BaseCliktCommand<*>.reportFailure(
    prefix: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: SynthFormatException) {
        reportAndFail(prefix, e)
    } catch (e: InvalidPatchException) {
        reportAndFail(prefix, e)
    } catch (e: SynthJsonException) {
        reportAndFail(prefix, e)
    } catch (e: JagVorbisFormatException) {
        reportAndFail(prefix, e)
    } catch (e: IOException) {
        reportAndFail(prefix, e)
    } catch (e: IllegalArgumentException) {
        reportAndFail(prefix, e)
    } catch (e: IllegalStateException) {
        reportAndFail(prefix, e)
    }

/** Prints `$prefix<message of [error]>` to stderr and fails. */
internal fun BaseCliktCommand<*>.reportAndFail(
    prefix: String,
    error: Exception,
): Nothing {
    echo("$prefix${error.message}", err = true)
    failure()
}

/** The lowercase extension with its dot, or `""`, like .NET `Path.GetExtension`. */
internal fun extensionOf(path: String): String {
    val extension = File(path).name.substringAfterLast('.', "")
    return if (extension.isEmpty()) "" else ".${extension.lowercase()}"
}
