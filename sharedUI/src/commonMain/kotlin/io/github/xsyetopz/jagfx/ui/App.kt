package io.github.xsyetopz.jagfx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.xsyetopz.jagfx.ui.editor.AudioOutput
import io.github.xsyetopz.jagfx.ui.editor.EditorViewModel
import io.github.xsyetopz.jagfx.ui.editor.PatchFiles
import io.github.xsyetopz.jagfx.ui.editor.PlatformSettings
import io.github.xsyetopz.jagfx.ui.editor.Preferences
import io.github.xsyetopz.jagfx.ui.editor.resolveTextScale
import io.github.xsyetopz.jagfx.ui.editor.resolveTheme
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.rememberStrings
import io.github.xsyetopz.jagfx.ui.screen.EditorScreen
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The JagFx editor. [files] and [audio] are the platform's file access and audio output.
 * [preferences] keeps the panel side, text size, and theme between runs; [settings] reports the
 * platform's high-contrast, reduced-motion, and text size settings.
 * [onTitleChange] receives the window title each time it changes; paths under [home] show `~`.
 */
@Composable
fun App(
    files: PatchFiles = NoFiles,
    audio: AudioOutput = SilentAudio,
    home: String? = null,
    preferences: Preferences = Preferences.None,
    settings: PlatformSettings = PlatformSettings.None,
    onTitleChange: (String) -> Unit = {},
) {
    val viewModel = viewModel { EditorViewModel(files, audio, preferences = preferences) }
    EditorApp(viewModel, home, settings, onTitleChange)
}

/** The editor for [viewModel], for platforms that create the view model themselves. */
@Composable
internal fun EditorApp(
    viewModel: EditorViewModel,
    home: String?,
    settings: PlatformSettings,
    onTitleChange: (String) -> Unit,
) {
    val titleChange by rememberUpdatedState(onTitleChange)
    LaunchedEffect(viewModel, home) {
        viewModel.state
            .map { it.windowTitle(home) }
            .distinctUntilChanged()
            .collect { titleChange(it) }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The theme and text scale to draw; pass them to JagFxTheme once it takes them.
    @Suppress("UNUSED_VARIABLE")
    val effectiveTheme = resolveTheme(state.theme, settings.highContrast)

    @Suppress("UNUSED_VARIABLE")
    val effectiveTextScale = resolveTextScale(state.textScalePercent, settings.textScalePercent)
    JagFxTheme {
        CompositionLocalProvider(LocalStrings provides rememberStrings()) {
            EditorScreen(viewModel)
        }
    }
}
