package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.EditorPreferences
import io.github.xsyetopz.jagfx.ui.editor.Preferences
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
