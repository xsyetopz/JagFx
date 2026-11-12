package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.JagToggleGroup

private val HarmonicBoxWidth = 120.dp

/**
 * The bottom bar: the Tones with Copy and Reset Active, the five harmonic boxes, the Reverb box,
 * and the Mode switch.
 */
@Composable
internal fun BottomBar(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    Row(
        modifier.background(JagFxTheme.colors.footer).padding(dimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
            JagLabel(strings[UiString.TonesLabel])
            VoiceChips(state, actions)
            StripButtons(
                Button(
                    strings[UiString.CopyLabel],
                    strings[UiString.TooltipCopyVoice],
                    state.isCopyMode,
                ) { actions.dispatch(SlotIntent.CopyVoice) },
                Button(strings[UiString.ResetActive], strings[UiString.TooltipResetVoice], false) {
                    actions.dispatch(SlotIntent.ResetVoice)
                },
            )
        }
        for (row in partialRows(state.voice)) {
            HarmonicBox(row, actions, Modifier.width(HarmonicBoxWidth))
        }
        ReverbBox(state, actions, Modifier.width(HarmonicBoxWidth))
        ModeSwitch(view, actions)
    }
}

@Composable
private fun ModeSwitch(
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val labels =
        mapOf(
            RackMode.Main to UiString.ModeMain,
            RackMode.Filter to UiString.ModeFilter,
            RackMode.Both to UiString.ModeBoth,
        )
    Column(verticalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd)) {
        JagLabel(strings[UiString.ModeLabel])
        JagToggleGroup(
            RackMode.entries,
            view.mode,
            { mode -> actions.updateView { it.copy(mode = mode) } },
            tooltip = { strings[labels.getValue(it)] },
        ) { mode ->
            JagText(strings[labels.getValue(mode)], style = JagFxTheme.typography.label)
        }
    }
}
