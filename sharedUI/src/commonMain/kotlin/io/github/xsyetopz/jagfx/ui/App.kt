package io.github.xsyetopz.jagfx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.xsyetopz.jagfx.ui.editor.AudioOutput
import io.github.xsyetopz.jagfx.ui.editor.EditorViewModel
import io.github.xsyetopz.jagfx.ui.editor.PatchFiles
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.rememberStrings
import io.github.xsyetopz.jagfx.ui.screen.EditorScreen
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The JagFx editor. [files] and [audio] are the platform's file access and audio output.
 * [onTitleChange] receives the window title each time it changes; paths under [home] show `~`.
 */
@Composable
fun App(
    files: PatchFiles = NoFiles,
    audio: AudioOutput = SilentAudio,
    home: String? = null,
    onTitleChange: (String) -> Unit = {},
) {
    val viewModel = viewModel { EditorViewModel(files, audio) }
    EditorApp(viewModel, home, onTitleChange)
}

/** The editor for [viewModel], for platforms that create the view model themselves. */
@Composable
internal fun EditorApp(
    viewModel: EditorViewModel,
    home: String?,
    onTitleChange: (String) -> Unit,
) {
    val titleChange by rememberUpdatedState(onTitleChange)
    LaunchedEffect(viewModel, home) {
        viewModel.state
            .map { it.windowTitle(home) }
            .distinctUntilChanged()
            .collect { titleChange(it) }
    }
    JagFxTheme {
        CompositionLocalProvider(LocalStrings provides rememberStrings()) {
            EditorScreen(viewModel)
        }
    }
}
