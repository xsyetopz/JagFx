package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.HistoryIntent
import io.github.xsyetopz.jagfx.ui.editor.LoopIntent
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.editor.VoiceIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagField
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagMenu
import io.github.xsyetopz.jagfx.ui.widgets.JagMenuItem
import io.github.xsyetopz.jagfx.ui.widgets.JagText

private val TimeFieldWidth = 64.dp
private val CountFieldWidth = 36.dp
private val U16Ms = 0..SynthLimits.U16_MAX

/** Whether the properties panel is [open], and [onToggle], which opens or closes it. */
internal class PanelToggle(
    val open: Boolean,
    val onToggle: () -> Unit,
)

/**
 * The header bar: transport, the voices, the timing of the selected voice, the patch loop, the
 * file name, history, file commands, and the properties panel switch.
 */
@Composable
internal fun HeaderBar(
    state: EditorState,
    actions: EditorActions,
    panel: PanelToggle,
    modifier: Modifier = Modifier,
) {
    val dimens = JagFxTheme.dimens
    Row(
        modifier
            .height(dimens.headerHeight)
            .background(JagFxTheme.colors.header)
            .padding(horizontal = dimens.spaceXl),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl * 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportGroup(state, actions)
        VoiceChips(state, actions)
        VoiceTimeGroup(state, actions)
        LoopGroup(state, actions)
        JagText(state.fileName, Modifier.weight(1f), color = JagFxTheme.colors.textDim)
        HistoryGroup(state, actions)
        FileGroup(state, actions)
        JagIconButton(
            Icons.Panel,
            LocalStrings.current[UiString.TooltipTogglePanel],
            panel.onToggle,
            active = panel.open,
        )
    }
}

/** A row of header controls with the gap between controls of one group. */
@Composable
private fun Group(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun TransportGroup(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    Group {
        JagIconButton(Icons.Play, strings[UiString.TooltipPlay], {
            actions.dispatch(RequestIntent.TogglePlay)
        })
        JagIconButton(
            Icons.Stop,
            strings[UiString.TooltipStop],
            { actions.dispatch(RequestIntent.Stop) },
        )
        JagIconButton(
            Icons.Loop,
            strings[UiString.TooltipLoopPlayback],
            { actions.dispatch(ViewIntent.SetLooping(!state.isLooping)) },
            active = state.isLooping,
        )
        JagIconButton(
            Icons.Solo,
            strings[UiString.TooltipPlaySelectedVoice],
            { actions.dispatch(ViewIntent.SetPlaySingleVoice(!state.playSingleVoice)) },
            active = state.playSingleVoice,
        )
    }
}

/** The length and start of the selected voice. */
@Composable
private fun VoiceTimeGroup(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val voice = state.voice
    Group {
        JagLabel(strings[UiString.DurationLabel])
        JagField(
            voice?.duration?.value ?: 0,
            { actions.dispatch(VoiceIntent.SetDuration(it)) },
            U16Ms,
            Modifier.width(TimeFieldWidth),
            enabled = voice != null,
            unit = "ms",
        )
        JagLabel(strings[UiString.OffsetLabel])
        JagField(
            voice?.offset?.value ?: 0,
            { actions.dispatch(VoiceIntent.SetOffset(it)) },
            U16Ms,
            Modifier.width(TimeFieldWidth),
            enabled = voice != null,
            unit = "ms",
        )
    }
}

/** The repeat count of playback and the loop start and end of the patch. */
@Composable
private fun LoopGroup(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val loop = state.patch.loop
    Group {
        JagLabel(strings[UiString.HeaderLoopCount])
        JagField(
            state.loopCount,
            { actions.dispatch(ViewIntent.SetLoopCount(it)) },
            0..EditorState.MAX_LOOP_COUNT,
            Modifier.width(CountFieldWidth),
        )
        JagLabel(strings[UiString.HeaderLoopStart])
        JagField(
            loop.begin.value,
            { actions.dispatch(LoopIntent.SetLoopBegin(it)) },
            U16Ms,
            Modifier.width(TimeFieldWidth),
            unit = "ms",
        )
        JagLabel(strings[UiString.HeaderLoopEnd])
        JagField(
            loop.end.value,
            { actions.dispatch(LoopIntent.SetLoopEnd(it)) },
            U16Ms,
            Modifier.width(TimeFieldWidth),
            unit = "ms",
        )
    }
}

@Composable
private fun HistoryGroup(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    Group {
        JagIconButton(
            Icons.Undo,
            strings[UiString.ToolbarUndo],
            { actions.dispatch(HistoryIntent.Undo) },
            enabled = state.canUndo,
        )
        JagIconButton(
            Icons.Redo,
            strings[UiString.ToolbarRedo],
            { actions.dispatch(HistoryIntent.Redo) },
            enabled = state.canRedo,
        )
    }
}

/** Open and save, and a menu of the file commands that have no button. */
@Composable
private fun FileGroup(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val items =
        listOf(
            Triple(UiString.ToolbarOpenSample, RequestIntent.OpenSample, true),
            Triple(UiString.ToolbarSaveAs, RequestIntent.SaveAs, true),
            Triple(UiString.ImportTone, RequestIntent.ImportTone, true),
            Triple(UiString.ExportTone, RequestIntent.ExportTone, state.voice != null),
        )
    val menuOffset =
        with(LocalDensity.current) { IntOffset(0, JagFxTheme.dimens.controlHeight.roundToPx()) }
    Group {
        JagIconButton(Icons.Open, strings[UiString.TooltipOpenFile], {
            actions.dispatch(RequestIntent.Open)
        })
        JagIconButton(Icons.Save, strings[UiString.TooltipSaveFile], {
            actions.dispatch(RequestIntent.Save)
        })
        Box {
            JagIconButton(
                Icons.More,
                strings[UiString.MoreActions],
                { expanded = true },
                active = expanded,
            )
            if (expanded) {
                JagMenu({ expanded = false }, menuOffset) {
                    for ((label, intent, enabled) in items) {
                        JagMenuItem(
                            strings[label],
                            {
                                expanded = false
                                actions.dispatch(intent)
                            },
                            enabled = enabled,
                        )
                    }
                }
            }
        }
    }
}
