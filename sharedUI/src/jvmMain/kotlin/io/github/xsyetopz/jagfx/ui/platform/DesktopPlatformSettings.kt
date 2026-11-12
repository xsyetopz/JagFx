package io.github.xsyetopz.jagfx.ui.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
private const val MIN_SCALE = 100
private const val MAX_SCALE = 200
private const val PERCENT = 100

/** Runs command and returns its trimmed standard output, or `null` when it fails or times out. */
internal typealias CommandRunner = (List<String>) -> String?

/** Runs [command] with [timeoutMs] limit. Non-zero exit, timeout, or I/O error gives `null`. */
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

/** Platform's accessibility values, as read from system's own settings tools. */
internal data class PlatformValues(
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val textScalePercent: Int = PERCENT,
)

/** Turns output of `gsettings get ... text-scaling-factor` into percent in 100..200; garbage gives 100. */
internal fun parseTextScale(output: String?): Int {
    val factor =
        output
            ?.trim()
            ?.removePrefix("double ")
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
    return factor?.let { (it * PERCENT).roundToInt().coerceIn(MIN_SCALE, MAX_SCALE) } ?: MIN_SCALE
}

/** Registry key that holds Windows "Make text bigger" percent as `TextScaleFactor`. */
internal const val WINDOWS_ACCESSIBILITY_KEY = "HKCU\\Software\\Microsoft\\Accessibility"

private val RegistryDword = Regex("""REG_DWORD\s+0x([0-9a-fA-F]+)""")

/** Turns `reg query` output for `REG_DWORD` percent into 100..200; anything else gives 100. */
internal fun parseRegistryPercent(output: String?): Int =
    output
        ?.let { RegistryDword.find(it) }
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull(radix = 16)
        ?.coerceIn(MIN_SCALE, MAX_SCALE) ?: MIN_SCALE

/** Plasma's default UI font size, `Noto Sans,10` in `kcms/fonts/fontssettings.kcfg`. */
private const val KDE_DEFAULT_POINTS = 10.0

/** Turns Plasma `QFont` string (`Noto Sans,12,-1,...`) into percent of 10 pt in 100..200. */
internal fun parseKdeFontScale(output: String?): Int =
    output
        ?.split(',')
        ?.getOrNull(1)
        ?.toDoubleOrNull()
        ?.let { parseTextScale((it / KDE_DEFAULT_POINTS).toString()) } ?: MIN_SCALE

private fun String?.isOn() = this?.trim().let { it == "1" || it == "true" }

private fun CommandRunner.kdeglobals(
    group: String,
    key: String,
) = this(listOf("kreadconfig6", "--file", "kdeglobals", "--group", group, "--key", key))

private fun CommandRunner.gsettings(
    schema: String,
    key: String,
) = this(listOf("gsettings", "get", schema, key))

private fun CommandRunner.defaults(
    domain: String,
    key: String,
) = this(listOf("defaults", "read", domain, key)).isOn()

/**
 * Reads macOS, Windows, GNOME, and KDE Plasma accessibility settings with [run]. Missing
 * key, failed command, and timeout all read as default.
 */
internal fun probePlatform(
    osName: String,
    run: CommandRunner,
): PlatformValues =
    when {
        osName.startsWith("Mac") -> {
            // `universalaccess` holds keys on older macOS; newer releases keep them in
            // `com.apple.Accessibility`. Missing key exits non-zero and reads as off.
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
            PlatformValues(
                textScalePercent =
                    parseRegistryPercent(
                        run(
                            listOf(
                                "reg",
                                "query",
                                WINDOWS_ACCESSIBILITY_KEY,
                                "/v",
                                "TextScaleFactor",
                            ),
                        ),
                    ),
            )
        }

        else -> {
            PlatformValues(
                highContrast =
                    run
                        .gsettings(
                            "org.gnome.desktop.a11y.interface",
                            "high-contrast",
                        ).isOn(),
                // GNOME answers through `gsettings`, Plasma through `kreadconfig6`;
                // missing tool reads as default, so neither needs desktop check.
                reducedMotion =
                    run.gsettings("org.gnome.desktop.interface", "enable-animations")?.trim() ==
                        "false" ||
                        run
                            .kdeglobals(
                                "KDE",
                                "AnimationDurationFactor",
                            )?.trim()
                            ?.toDoubleOrNull() ==
                        0.0,
                textScalePercent =
                    maxOf(
                        parseTextScale(
                            run.gsettings("org.gnome.desktop.interface", "text-scaling-factor"),
                        ),
                        parseKdeFontScale(run.kdeglobals("General", "font")),
                    ),
            )
        }
    }

/**
 * Desktop platform settings. Windows high contrast comes from AWT desktop property and Windows
 * text size from `reg query`. On macOS, GNOME, and KDE Plasma values come from `defaults`,
 * `gsettings`, and `kreadconfig6`.
 * Commands run once by [start] (daemon thread by default) and are cached, so change shows
 * after restart.
 * Values are Compose state: they hold defaults until read finishes, then composable
 * that read them recomposes. No getter waits.
 */
class DesktopPlatformSettings internal constructor(
    private val osName: String,
    private val windowsHighContrast: () -> Boolean,
    start: (() -> Unit) -> Unit = ::startDaemon,
    run: CommandRunner,
) : PlatformSettings {
    /** Reads settings of OS this JVM runs on. */
    constructor() : this(
        System.getProperty("os.name").orEmpty(),
        ::awtHighContrast,
        run = ::runCommand,
    )

    private var values by mutableStateOf(PlatformValues())

    init {
        start {
            values =
                try {
                    probePlatform(osName, run)
                } catch (_: RuntimeException) {
                    PlatformValues()
                }
        }
    }

    override val highContrast: Boolean
        get() = if (osName.startsWith("Windows")) windowsHighContrast() else values.highContrast

    override val reducedMotion: Boolean get() = values.reducedMotion
    override val textScalePercent: Int get() = values.textScalePercent
}

private fun startDaemon(task: () -> Unit) {
    Thread(task, "jagfx-platform-settings").apply { isDaemon = true }.start()
}

private fun awtHighContrast(): Boolean =
    try {
        Toolkit.getDefaultToolkit().getDesktopProperty("win.highContrast.on") == true
    } catch (_: AWTError) {
        false
    } catch (_: HeadlessException) {
        false
    }
