package io.github.xsyetopz.jagfx.ui

import io.github.xsyetopz.jagfx.ui.editor.EditorPreferences
import io.github.xsyetopz.jagfx.ui.editor.PanelSide
import io.github.xsyetopz.jagfx.ui.editor.ThemePreference
import io.github.xsyetopz.jagfx.ui.platform.DesktopPreferences
import io.github.xsyetopz.jagfx.ui.platform.defaultConfigDirectory
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPreferencesTest {
    private val root = Files.createTempDirectory("jagfx-prefs").toFile()

    private fun dir() = File(root, "nested/JagFx")

    @Test
    fun missingFileGivesDefaults() {
        assertEquals(EditorPreferences(), DesktopPreferences(dir()).load())
    }

    @Test
    fun savedPreferencesComeBack() {
        val saved = EditorPreferences(PanelSide.Left, 150, ThemePreference.HighContrast)
        DesktopPreferences(dir()).save(saved)
        assertEquals(saved, DesktopPreferences(dir()).load())
    }

    @Test
    fun corruptFileGivesDefaults() {
        dir().mkdirs()
        File(dir(), "preferences.json").writeText("{ not json")
        assertEquals(EditorPreferences(), DesktopPreferences(dir()).load())
    }

    @Test
    fun unknownValuesFallBackToDefaults() {
        dir().mkdirs()
        File(dir(), "preferences.json")
            .writeText("""{"panelSide":"Top","textScalePercent":120,"x":1}""")
        assertEquals(EditorPreferences(textScalePercent = 120), DesktopPreferences(dir()).load())
    }

    @Test
    fun configFolderFollowsThePlatform() {
        val env = mapOf("XDG_CONFIG_HOME" to "/xdg", "APPDATA" to "C:/Roaming")
        assertEquals(File("/xdg/jagfx"), defaultConfigDirectory("Linux", "/h", env::get))
        assertEquals(File("/h/.config/jagfx"), defaultConfigDirectory("Linux", "/h") { null })
        assertEquals(
            File("/h/Library/Application Support/JagFx"),
            defaultConfigDirectory("Mac OS X", "/h", env::get),
        )
        assertEquals(File("C:/Roaming/JagFx"), defaultConfigDirectory("Windows 11", "/h", env::get))
    }
}
