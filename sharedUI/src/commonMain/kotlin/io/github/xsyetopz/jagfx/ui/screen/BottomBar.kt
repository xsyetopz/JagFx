package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.RackMode
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/**
 * Bottom bar: voices with Copy voice and Reset voice under them, five harmonic boxes
 * sharing free width, and Echo box as wide as its fields, all at one height.
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
        modifier
            .height(IntrinsicSize.Max)
            .background(JagFxTheme.colors.footer)
            .padding(dimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXl),
        verticalAlignment = Alignment.Top,
    ) {
        // Column is as wide as its widest row, two buttons; chips fill it.
        Column(
            Modifier.width(IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
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
        }
        for (row in partialRows(state.voice)) {
            HarmonicBox(row, actions, Modifier.weight(1f).fillMaxHeight())
        }
        ReverbBox(state, actions, Modifier.width(IntrinsicSize.Max).fillMaxHeight())
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
