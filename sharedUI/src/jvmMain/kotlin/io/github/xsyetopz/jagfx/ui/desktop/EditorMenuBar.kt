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
import io.github.xsyetopz.jagfx.ui.editor.PanelSide
import io.github.xsyetopz.jagfx.ui.editor.RackMode
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.editor.ThemePreference
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.i18n.rememberStrings
import io.github.xsyetopz.jagfx.ui.screen.PlayAction
import io.github.xsyetopz.jagfx.ui.screen.label

/** Number keys for voices 1 to 10, as on number row: 1 to 9, then 0. */
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

/** Number keys for rack modes Main, Filter and Both, used with command key and Alt. */
private val ModeKeys = listOf(Key.One, Key.Two, Key.Three)

/** Shortcut with Command on macOS and Ctrl elsewhere. */
private fun command(
    key: Key,
    shift: Boolean = false,
    alt: Boolean = false,
): KeyShortcut = KeyShortcut(key, ctrl = !isMacOs, meta = isMacOs, shift = shift, alt = alt)

/**
 * Menu bar of editor window: on macOS screen menu bar, elsewhere bar in window.
 * Every toolbar command is here, with platform shortcuts.
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
        Menu(strings[UiString.MenuView], enabled = state.sample == null) {
            for (side in PanelSide.entries) {
                RadioButtonItem(strings[side.label], state.panelSide == side) {
                    dispatch(ViewIntent.SetPanelSide(side))
                }
            }
            Separator()
            Menu(strings[UiString.MenuTheme]) {
                for (theme in ThemePreference.entries) {
                    RadioButtonItem(strings[theme.label], state.theme == theme) {
                        dispatch(ViewIntent.SetTheme(theme))
                    }
                }
            }
            Menu(strings[UiString.RackViewMenu]) {
                for ((index, mode) in RackMode.entries.withIndex()) {
                    RadioButtonItem(
                        strings[mode.label],
                        state.mode == mode,
                        shortcut = ModeKeys.getOrNull(index)?.let { command(it, alt = true) },
                    ) { dispatch(ViewIntent.SetMode(mode)) }
                }
            }
            Menu(strings[UiString.MenuTextSize]) {
                for (percent in EditorState.TEXT_SCALE_STEPS) {
                    RadioButtonItem("$percent%", state.textScalePercent == percent) {
                        dispatch(ViewIntent.SetTextScale(percent))
                    }
                }
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
    Item(strings[UiString.PlayAll], shortcut = command(Key.P)) {
        PlayAction.PlayAll.intents().forEach(dispatch)
    }
    Item(strings[UiString.LoopAll], shortcut = command(Key.L, shift = true)) {
        PlayAction.LoopAll.intents().forEach(dispatch)
    }
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
