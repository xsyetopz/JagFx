package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.PlatformSettings
import java.awt.AWTError
import java.awt.HeadlessException
import java.awt.Toolkit
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.roundToInt

private const val PROBE_TIMEOUT_MS = 1_500L
private const val WAIT_MS = 2_500L
private const val MIN_SCALE = 100
private const val MAX_SCALE = 200
private const val PERCENT = 100

/** Runs a command and returns its trimmed standard output, or `null` when it fails or times out. */
internal typealias CommandRunner = (List<String>) -> String?

/** Runs [command] with a [timeoutMs] limit. A non-zero exit, a timeout, or an I/O error gives `null`. */
internal fun runCommand(
    command: List<String>,
    timeoutMs: Long = PROBE_TIMEOUT_MS,
): String? {
    var process: Process? = null
    return try {
        val started = ProcessBuilder(command).start()
        process = started
        started.outputStream.close()
        val output = CompletableFuture.supplyAsync { started.inputStream.readBytes() }
        if (!started.waitFor(timeoutMs, TimeUnit.MILLISECONDS) || started.exitValue() != 0) {
            null
        } else {
            String(output.get(timeoutMs, TimeUnit.MILLISECONDS)).trim()
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    } catch (_: ExecutionException) {
        null
    } catch (_: TimeoutException) {
        null
    } finally {
        process?.destroyForcibly()
    }
}

/** The platform's accessibility values, as read from the system's own settings tools. */
internal data class PlatformValues(
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val textScalePercent: Int = PERCENT,
)

/** Turns the output of `gsettings get ... text-scaling-factor` into a percent in 100..200; garbage gives 100. */
internal fun parseTextScale(output: String?): Int {
    val factor =
        output
            ?.trim()
            ?.removePrefix("double ")
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
    return factor?.let { (it * PERCENT).roundToInt().coerceIn(MIN_SCALE, MAX_SCALE) } ?: MIN_SCALE
}

private fun String?.isOn() = this?.trim().let { it == "1" || it == "true" }

private fun CommandRunner.gsettings(
    schema: String,
    key: String,
) = this(listOf("gsettings", "get", schema, key))

private fun CommandRunner.defaults(
    domain: String,
    key: String,
) = this(listOf("defaults", "read", domain, key)).isOn()

/**
 * Reads the macOS and GNOME accessibility settings with [run]. A missing key, a failed command, and
 * a timeout all read as the default.
 */
internal fun probePlatform(
    osName: String,
    run: CommandRunner,
): PlatformValues =
    when {
        osName.startsWith("Mac") -> {
            // `universalaccess` holds the keys on older macOS; newer releases keep them in
            // `com.apple.Accessibility`. A missing key exits non-zero and reads as off.
            PlatformValues(
                highContrast =
                    run.defaults("com.apple.universalaccess", "increaseContrast") ||
                        run.defaults(
                            "com.apple.Accessibility",
                            "EnhancedBackgroundContrastEnabled",
                        ),
                reducedMotion =
                    run.defaults("com.apple.universalaccess", "reduceMotion") ||
                        run.defaults("com.apple.Accessibility", "ReduceMotionEnabled"),
            )
        }

        osName.startsWith("Windows") -> {
            PlatformValues()
        }

        else -> {
            PlatformValues(
                highContrast =
                    run
                        .gsettings(
                            "org.gnome.desktop.a11y.interface",
                            "high-contrast",
                        ).isOn(),
                reducedMotion =
                    run.gsettings("org.gnome.desktop.interface", "enable-animations")?.trim() ==
                        "false",
                textScalePercent =
                    parseTextScale(
                        run.gsettings("org.gnome.desktop.interface", "text-scaling-factor"),
                    ),
            )
        }
    }

/**
 * Desktop platform settings. Windows high contrast comes from the AWT desktop property. On macOS and
 * GNOME the values come from `defaults` and `gsettings`, read once on a background thread started at
 * construction and cached, so a change shows after a restart. A read that has not finished within a
 * few seconds gives the defaults.
 */
class DesktopPlatformSettings internal constructor(
    private val osName: String,
    private val windowsHighContrast: () -> Boolean,
    run: CommandRunner,
) : PlatformSettings {
    /** Reads the settings of the OS this JVM runs on. */
    constructor() : this(System.getProperty("os.name").orEmpty(), ::awtHighContrast, ::runCommand)

    private val values: CompletableFuture<PlatformValues> =
        CompletableFuture
            .supplyAsync {
                probePlatform(
                    osName,
                    run,
                )
            }.exceptionally { PlatformValues() }

    private fun cached(): PlatformValues =
        try {
            values.get(WAIT_MS, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            PlatformValues()
        } catch (_: ExecutionException) {
            PlatformValues()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            PlatformValues()
        }

    override val highContrast: Boolean
        get() = if (osName.startsWith("Windows")) windowsHighContrast() else cached().highContrast

    override val reducedMotion: Boolean get() = cached().reducedMotion
    override val textScalePercent: Int get() = cached().textScalePercent
}

private fun awtHighContrast(): Boolean =
    try {
        Toolkit.getDefaultToolkit().getDesktopProperty("win.highContrast.on") == true
    } catch (_: AWTError) {
        false
    } catch (_: HeadlessException) {
        false
    }
