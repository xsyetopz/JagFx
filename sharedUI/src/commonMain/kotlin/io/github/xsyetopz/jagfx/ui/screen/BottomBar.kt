package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.RackMode
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.JagToggleGroup

/**
 * The bottom bar: the Tones with Copy, Reset Active, and the Mode switch under them, then the five
 * harmonic boxes and the Reverb box sharing the rest of the width.
 */
@Composable
internal fun BottomBar(
    state: EditorState,
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
        // The buttons and the Mode switch fill their rows, so the column takes the width of the
        // tone chips. The Mode switch sits here, in the room the taller boxes leave, so the boxes
        // get the width their fields need at 1280 dp.
        Column(
            Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
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
            ModeSwitch(state, actions)
        }
        for (row in partialRows(state.voice)) {
            HarmonicBox(row, actions, Modifier.weight(1f))
        }
        ReverbBox(state, actions, Modifier.weight(1f))
    }
}

/** The menu and button label of this rack mode. */
internal val RackMode.label: UiString
    get() =
        when (this) {
            RackMode.Main -> UiString.ModeMain
            RackMode.Filter -> UiString.ModeFilter
            RackMode.Both -> UiString.ModeBoth
        }

@Composable
private fun ModeSwitch(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd)) {
        JagLabel(strings[UiString.ModeLabel])
        JagToggleGroup(
            RackMode.entries,
            state.mode,
            { mode -> actions.dispatch(ViewIntent.SetMode(mode)) },
            tooltip = { strings[it.label] },
        ) { mode ->
            JagText(strings[mode.label], style = MaterialTheme.typography.labelSmall)
        }
    }
}
