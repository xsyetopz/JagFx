package io.github.xsyetopz.jagfx.ui.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.xsyetopz.jagfx.ui.EditorApp
import io.github.xsyetopz.jagfx.ui.editor.AudioOutput
import io.github.xsyetopz.jagfx.ui.editor.EditorViewModel
import io.github.xsyetopz.jagfx.ui.editor.PlatformSettings
import io.github.xsyetopz.jagfx.ui.editor.Preferences
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.platform.DesktopPatchFiles
import io.github.xsyetopz.jagfx.ui.screen.LocalNativeMenu
import java.io.File

internal val isMacOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

/**
 * Editor window with its menu bar. On macOS title is document name, and close
 * button and proxy icon show unsaved changes and open file, as in other document apps;
 * elsewhere title also carries path and `*` for unsaved changes.
 * Paths under [home] show `~`.
 * [hidden] window never takes focus and opens behind other windows, so script can capture it
 * while user keeps working. [openPath] names `.synth` file to open at start.
 */
@Composable
fun ApplicationScope.JagFxWindow(
    audio: AudioOutput,
    preferences: Preferences,
    settings: PlatformSettings,
    home: String?,
    icon: Painter?,
    hidden: Boolean = false,
    openPath: String? = null,
) {
    var title by remember { mutableStateOf("JagFx") }
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
        title = title,
        icon = icon,
        focusable = !hidden,
    ) {
        val files = remember { DesktopPatchFiles(owner = { window }) }
        val viewModel =
            viewModel {
                EditorViewModel(
                    files,
                    audio,
                    preferences = preferences,
                    settings = settings,
                )
            }
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            if (hidden) window.toBack()
            openPath?.let { viewModel.dispatch(RequestIntent.OpenPath(it)) }
        }
        EditorMenuBar(state, viewModel::dispatch)
        if (isMacOs) {
            val dirty = state.sample?.isDirty ?: state.isDirty
            val path = state.sample?.path ?: state.filePath
            val name = state.sample?.name ?: state.fileName
            LaunchedEffect(dirty, path, name) {
                title = name
                window.rootPane.putClientProperty("Window.documentModified", dirty)
                window.rootPane.putClientProperty("Window.documentFile", path?.let(::File))
            }
        }
        CompositionLocalProvider(LocalNativeMenu provides true) {
            EditorApp(viewModel, home, settings) { if (!isMacOs) title = it }
        }
    }
}
