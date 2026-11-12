package io.github.xsyetopz.jagfx.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.xsyetopz.jagfx.ui.App
import io.github.xsyetopz.jagfx.ui.platform.BrowserPatchFiles
import io.github.xsyetopz.jagfx.ui.platform.BrowserPlatformSettings
import io.github.xsyetopz.jagfx.ui.platform.BrowserPreferences
import io.github.xsyetopz.jagfx.ui.platform.WebAudioOutput
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val files = BrowserPatchFiles()
    val audio = WebAudioOutput()
    val preferences = BrowserPreferences()
    val settings = BrowserPlatformSettings()
    ComposeViewport {
        App(
            files = files,
            audio = audio,
            preferences = preferences,
            settings = settings,
            onTitleChange = { document.title = it },
        )
    }
}
