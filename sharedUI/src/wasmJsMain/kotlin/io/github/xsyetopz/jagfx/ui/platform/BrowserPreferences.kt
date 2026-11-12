@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.EditorPreferences
import io.github.xsyetopz.jagfx.ui.editor.PlatformSettings
import io.github.xsyetopz.jagfx.ui.editor.Preferences
import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

private const val STORAGE_KEY = "jagfx.preferences"

private val json =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

/** Keeps preferences as JSON in `localStorage`. Unreadable or blocked storage gives defaults. */
class BrowserPreferences : Preferences {
    @Suppress("TooGenericExceptionCaught")
    override fun load(): EditorPreferences =
        try {
            localStorage.getItem(STORAGE_KEY)?.let { json.decodeFromString<EditorPreferences>(it) }
                ?: EditorPreferences()
        } catch (_: Throwable) {
            // Corrupt JSON, or JS SecurityError when browser blocks storage.
            EditorPreferences()
        }

    @Suppress("TooGenericExceptionCaught")
    override fun save(preferences: EditorPreferences) {
        try {
            localStorage.setItem(STORAGE_KEY, json.encodeToString(preferences))
        } catch (_: Throwable) {
            // Full or blocked storage loses setting, not session.
        }
    }
}

@Suppress("UnusedParameter") // `query` is read by the js() body.
private fun mediaMatches(query: String): Boolean = js("window.matchMedia(query).matches")

/** Reads high contrast and reduced motion from CSS media queries each time it is asked. */
class BrowserPlatformSettings : PlatformSettings {
    override val highContrast: Boolean
        get() = mediaMatches("(prefers-contrast: more)") || mediaMatches("(forced-colors: active)")

    override val reducedMotion: Boolean
        get() = mediaMatches("(prefers-reduced-motion: reduce)")

    /** Browsers scale page themselves, so no extra text scale is reported. */
    override val textScalePercent: Int = 100
}
