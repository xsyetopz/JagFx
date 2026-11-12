package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.EditorPreferences
import io.github.xsyetopz.jagfx.ui.editor.PlatformSettings
import io.github.xsyetopz.jagfx.ui.editor.Preferences
import kotlinx.serialization.json.Json
import java.awt.AWTError
import java.awt.HeadlessException
import java.awt.Toolkit
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

private const val FILE_NAME = "preferences.json"

private val json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
    }

/**
 * Keeps the preferences as JSON in `preferences.json` under [directory]. A missing, unreadable,
 * or corrupt file gives the defaults; a failed write is dropped.
 */
class DesktopPreferences(
    private val directory: File = defaultConfigDirectory(),
) : Preferences {
    private val file get() = File(directory, FILE_NAME)

    override fun load(): EditorPreferences =
        try {
            json.decodeFromString<EditorPreferences>(file.readText())
        } catch (_: IOException) {
            EditorPreferences()
        } catch (_: IllegalArgumentException) {
            // A SerializationException is an IllegalArgumentException.
            EditorPreferences()
        }

    override fun save(preferences: EditorPreferences) {
        try {
            Files.createDirectories(directory.toPath())
            val temporary = File(directory, "$FILE_NAME.tmp").toPath()
            Files.writeString(temporary, json.encodeToString(preferences))
            Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (_: IOException) {
            // Losing a setting must not end the session.
        } catch (_: SecurityException) {
            // Same.
        }
    }
}

/**
 * The user's config folder for JagFx: `%APPDATA%\JagFx` on Windows,
 * `~/Library/Application Support/JagFx` on macOS, and `$XDG_CONFIG_HOME/jagfx` or
 * `~/.config/jagfx` elsewhere.
 */
fun defaultConfigDirectory(
    osName: String = System.getProperty("os.name").orEmpty(),
    home: String = System.getProperty("user.home").orEmpty(),
    env: (String) -> String? = System::getenv,
): File =
    when {
        osName.startsWith("Windows") -> {
            val base = env("APPDATA")?.takeIf { it.isNotBlank() }
            File(base ?: File(home, "AppData/Roaming").path, "JagFx")
        }

        osName.startsWith("Mac") -> {
            File(home, "Library/Application Support/JagFx")
        }

        else -> {
            val base = env("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
            File(base ?: File(home, ".config").path, "jagfx")
        }
    }

private const val DEFAULTS_TIMEOUT_MS = 1000L

/** `true` when the output of `defaults read` for a boolean key is `1`. */
internal fun parseDefaultsBoolean(output: String): Boolean = output.trim() == "1"

/**
 * The macOS Reduce motion setting, read once with `defaults`. It is `false` off macOS, when the
 * key is absent (the default), and when the command fails or takes longer than a second.
 */
internal fun readMacReducedMotion(
    osName: String = System.getProperty("os.name").orEmpty(),
): Boolean {
    if (!osName.startsWith("Mac")) return false
    return try {
        val process =
            ProcessBuilder("defaults", "read", "com.apple.universalaccess", "reduceMotion")
                .redirectErrorStream(true)
                .start()
        if (process.waitFor(DEFAULTS_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            process.exitValue() == 0 &&
                parseDefaultsBoolean(process.inputStream.bufferedReader().readText())
        } else {
            process.destroyForcibly()
            false
        }
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/**
 * Desktop platform settings. The JVM exposes the Windows high-contrast flag, and macOS Reduce
 * motion through [readMacReducedMotion]; the text size setting is not exposed, so it stays at the
 * default.
 */
class DesktopPlatformSettings : PlatformSettings {
    override val highContrast: Boolean
        get() =
            try {
                Toolkit.getDefaultToolkit().getDesktopProperty("win.highContrast.on") == true
            } catch (_: AWTError) {
                false
            } catch (_: HeadlessException) {
                false
            }

    override val reducedMotion: Boolean = readMacReducedMotion()
    override val textScalePercent: Int = 100
}
