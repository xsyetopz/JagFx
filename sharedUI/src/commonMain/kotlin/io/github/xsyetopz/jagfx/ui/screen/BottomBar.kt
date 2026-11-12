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
 * Bottom bar: Tones with Copy, Reset Active, and Mode switch under them, then five
 * harmonic boxes and Reverb box sharing rest of width.
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
        // Buttons and Mode switch fill their rows, so column takes width of
        // tone chips. Mode switch sits here, in room taller boxes leave, so boxes
        // get width their fields need at 1280 dp.
        Column(
            Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            JagLabel(strings[UiString.VoicesLabel])
            VoiceChips(state, actions)
            StripButtons(
                Button(
                    strings[UiString.CopyVoiceLabel],
                    strings[UiString.TooltipCopyVoice],
                    state.isCopyMode,
                ) { actions.dispatch(SlotIntent.CopyVoice) },
                Button(
                    strings[UiString.ResetVoiceLabel],
                    strings[UiString.TooltipResetVoice],
                    false,
                ) {
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

/** Menu and button label of this rack mode. */
internal val RackMode.label: UiString
    get() =
        when (this) {
            RackMode.Main -> UiString.ModeEnvelopes
            RackMode.Filter -> UiString.ModeFilter
            RackMode.Both -> UiString.ModeAll
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
