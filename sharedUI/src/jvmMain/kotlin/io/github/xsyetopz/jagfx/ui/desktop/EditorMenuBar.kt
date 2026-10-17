package io.github.xsyetopz.jagfx.ui.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.editor.EditorIntent
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.HistoryIntent
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.i18n.rememberStrings

/** Number keys for voices 1 to 10, as on the number row: 1 to 9, then 0. */
private val VoiceKeys =
    listOf(
        Key.One,
        Key.Two,
        Key.Three,
        Key.Four,
        Key.Five,
        Key.Six,
        Key.Seven,
        Key.Eight,
        Key.Nine,
        Key.Zero,
    )

/** A shortcut with Command on macOS and Ctrl elsewhere. */
private fun command(
    key: Key,
    shift: Boolean = false,
): KeyShortcut = KeyShortcut(key, ctrl = !isMacOs, meta = isMacOs, shift = shift)

/**
 * The menu bar of the editor window: on macOS the screen menu bar, elsewhere a bar in the window.
 * Every toolbar command is here, with the platform shortcuts.
 */
@Composable
internal fun FrameWindowScope.EditorMenuBar(
    state: EditorState,
    dispatch: (EditorIntent) -> Unit,
) {
    val strings = rememberStrings()
    MenuBar {
        Menu(strings[UiString.MenuFile]) { FileItems(state, strings, dispatch) }
        Menu(strings[UiString.MenuEdit]) {
            val editable = state.sample == null
            Item(
                strings[UiString.MenuUndo],
                enabled = editable && state.canUndo,
                shortcut = command(Key.Z),
            ) {
                dispatch(HistoryIntent.Undo)
            }
            Item(
                strings[UiString.MenuRedo],
                enabled = editable && state.canRedo,
                shortcut = command(Key.Z, shift = true),
            ) {
                dispatch(HistoryIntent.Redo)
            }
        }
        Menu(strings[UiString.MenuPlayback]) { PlaybackItems(state, strings, dispatch) }
        Menu(strings[UiString.MenuVoice], enabled = state.sample == null) {
            VoiceItems(state, strings, dispatch)
        }
    }
}

@Composable
private fun MenuScope.FileItems(
    state: EditorState,
    strings: Strings,
    dispatch: (EditorIntent) -> Unit,
) {
    val patch = state.sample == null
    Item(strings[UiString.MenuOpen], shortcut = command(Key.O)) { dispatch(RequestIntent.Open) }
    Item(strings[UiString.MenuOpenSample]) { dispatch(RequestIntent.OpenSample) }
    Separator()
    Item(strings[UiString.MenuSave], shortcut = command(Key.S)) {
        dispatch(if (patch) RequestIntent.Save else RequestIntent.SaveSample)
    }
    Item(strings[UiString.MenuSaveAs], shortcut = command(Key.S, shift = true)) {
        dispatch(if (patch) RequestIntent.SaveAs else RequestIntent.SaveSampleAs)
    }
    Separator()
    Item(strings[UiString.MenuImportTone], enabled = patch) { dispatch(RequestIntent.ImportTone) }
    Item(strings[UiString.MenuExportTone], enabled = patch && state.voice != null) {
        dispatch(RequestIntent.ExportTone)
    }
    Item(strings[UiString.MenuImportFilter], enabled = patch && state.voice != null) {
        dispatch(RequestIntent.ImportFilter)
    }
    Item(strings[UiString.MenuExportFilter], enabled = patch && state.voice?.filter != null) {
        dispatch(RequestIntent.ExportFilter)
    }
}

@Composable
private fun MenuScope.PlaybackItems(
    state: EditorState,
    strings: Strings,
    dispatch: (EditorIntent) -> Unit,
) {
    Item(
        strings[if (state.isPlaying) UiString.MenuStop else UiString.MenuPlay],
        shortcut = KeyShortcut(Key.Spacebar),
    ) { dispatch(RequestIntent.TogglePlay) }
    Item(strings[UiString.MenuStop], enabled = state.isPlaying, shortcut = command(Key.Period)) {
        dispatch(RequestIntent.Stop)
    }
    Separator()
    CheckboxItem(strings[UiString.MenuLoop], state.isLooping, shortcut = command(Key.L)) {
        dispatch(ViewIntent.SetLooping(it))
    }
    CheckboxItem(
        strings[UiString.MenuSoloVoice],
        state.playSingleVoice,
        enabled =
            state.sample == null,
    ) {
        dispatch(ViewIntent.SetPlaySingleVoice(it))
    }
}

@Composable
private fun MenuScope.VoiceItems(
    state: EditorState,
    strings: Strings,
    dispatch: (EditorIntent) -> Unit,
) {
    for (slot in 0 until SynthLimits.VOICE_SLOTS) {
        RadioButtonItem(
            strings.format(UiString.VoiceNumber, slot + 1),
            selected = slot == state.selectedVoice,
            shortcut = VoiceKeys.getOrNull(slot)?.let { command(it) },
        ) { dispatch(SlotIntent.SelectVoice(slot)) }
    }
    Separator()
    CheckboxItem(strings[UiString.MenuVoiceEnabled], state.voice != null) {
        dispatch(SlotIntent.SetVoiceEnabled(state.selectedVoice, it))
    }
    Item(strings[UiString.MenuCopyVoice], enabled = state.voice != null) {
        dispatch(SlotIntent.CopyVoice)
    }
    Item(strings[UiString.MenuResetVoice], enabled = state.voice != null) {
        dispatch(SlotIntent.ResetVoice)
    }
}
