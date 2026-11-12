package io.github.xsyetopz.jagfx.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
import io.github.xsyetopz.jagfx.ui.editor.ThemePreference
import io.github.xsyetopz.jagfx.ui.editor.resolveTextScale
import io.github.xsyetopz.jagfx.ui.editor.resolveTheme
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.rememberStrings
import io.github.xsyetopz.jagfx.ui.screen.EditorScreen
import io.github.xsyetopz.jagfx.ui.theme.JagFxAppearance
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.LocalReducedMotion
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
    val viewModel =
        viewModel { EditorViewModel(files, audio, preferences = preferences, settings = settings) }
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

    val appearance =
        when (resolveTheme(state.theme, settings.highContrast)) {
            ThemePreference.System -> {
                if (isSystemInDarkTheme()) JagFxAppearance.Classic else JagFxAppearance.Light
            }

            ThemePreference.Classic -> {
                JagFxAppearance.Classic
            }

            ThemePreference.Light -> {
                JagFxAppearance.Light
            }

            ThemePreference.HighContrast -> {
                JagFxAppearance.HighContrast
            }
        }
    val textScale = resolveTextScale(state.textScalePercent, settings.textScalePercent) / PERCENT
    JagFxTheme(appearance, textScale) {
        CompositionLocalProvider(
            LocalStrings provides rememberStrings(),
            LocalReducedMotion provides settings.reducedMotion,
        ) {
            EditorScreen(viewModel)
        }
    }
}

private const val PERCENT = 100f
