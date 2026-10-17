package io.github.xsyetopz.jagfx.ui.desktop

import androidx.compose.runtime.Composable
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
import io.github.xsyetopz.jagfx.ui.platform.DesktopPatchFiles
import java.io.File

internal val isMacOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

/**
 * The editor window with its menu bar. On macOS the title is the document name, and the close
 * button and the proxy icon show unsaved changes and the open file, as in other document apps;
 * elsewhere the title also carries the path and a `*` for unsaved changes.
 * Paths under [home] show `~`.
 */
@Composable
fun ApplicationScope.JagFxWindow(
    audio: AudioOutput,
    home: String?,
    icon: Painter?,
) {
    var title by remember { mutableStateOf("JagFx") }
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
        title = title,
        icon = icon,
    ) {
        val files = remember { DesktopPatchFiles(owner = { window }) }
        val viewModel = viewModel { EditorViewModel(files, audio) }
        val state by viewModel.state.collectAsStateWithLifecycle()
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
        EditorApp(viewModel, home) { if (!isMacOs) title = it }
    }
}
