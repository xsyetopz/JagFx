package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.LoopIntent
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.editor.VoiceIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagButton

private val U16Ms = 0..SynthLimits.U16_MAX

/**
 * The voice strip, as the original's left strip: the oscillator, the timing and loop fields of the
 * selected tone, the transport, Load and Save, and True Wave.
 */
@Composable
internal fun VoiceStrip(
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    StripColumn(modifier) {
        WaveformAndHzFields(SignalChainSlot.Pitch, state, actions)
        TimingFields(state, actions)
        Transport(state, actions)
        StripField(
            strings[UiString.LoopsLabel],
            strings[UiString.LoopsLabel],
            state.loopCount,
            { actions.dispatch(ViewIntent.SetLoopCount(it)) },
            "",
            range = 0..EditorState.MAX_LOOP_COUNT,
        )
        StripButtons(
            Button(
                strings[UiString.MenuStop],
                null,
                false,
            ) { actions.dispatch(RequestIntent.Stop) },
        )
        StripButtons(
            Button(
                strings[UiString.LoadLabel],
                null,
                false,
            ) { actions.dispatch(RequestIntent.Open) },
            Button(
                strings[UiString.MenuSave],
                null,
                false,
            ) { actions.dispatch(RequestIntent.Save) },
        )
        StripButtons(
            Button(
                strings[UiString.TrueWave],
                strings[UiString.TooltipTrueWave],
                state.trueWave,
            ) { actions.dispatch(ViewIntent.SetTrueWave(!state.trueWave)) },
        )
    }
}

/** Len and Pos of the selected tone, and the loop start L1 and end L2 of the patch. */
@Composable
private fun TimingFields(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val voice = state.voice
    val loop = state.patch.loop
    StripField(
        strings[UiString.LabelLength],
        strings[UiString.DurationLabel],
        voice?.duration?.value ?: 0,
        { actions.dispatch(VoiceIntent.SetDuration(it)) },
        "ms",
        voice != null,
        U16Ms,
    )
    StripField(
        strings[UiString.LabelPosition],
        strings[UiString.OffsetLabel],
        voice?.offset?.value ?: 0,
        { actions.dispatch(VoiceIntent.SetOffset(it)) },
        "ms",
        voice != null,
        U16Ms,
    )
    StripField(
        strings[UiString.LabelLoopStart],
        strings[UiString.HeaderLoopStart],
        loop.begin.value,
        { actions.dispatch(LoopIntent.SetLoopBegin(it)) },
        "ms",
        range = U16Ms,
    )
    StripField(
        strings[UiString.LabelLoopEnd],
        strings[UiString.HeaderLoopEnd],
        loop.end.value,
        { actions.dispatch(LoopIntent.SetLoopEnd(it)) },
        "ms",
        range = U16Ms,
    )
}

/** Play, Play All, Loop, and Loop All; the button of the running playback is active. */
@Composable
private fun Transport(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val labels =
        mapOf(
            PlayAction.Play to (UiString.MenuPlay to UiString.TooltipPlayTone),
            PlayAction.PlayAll to (UiString.PlayAll to UiString.TooltipPlayAll),
            PlayAction.Loop to (UiString.LoopTone to UiString.TooltipLoopTone),
            PlayAction.LoopAll to (UiString.LoopAll to UiString.TooltipLoopAll),
        )
    val buttons =
        PlayAction.entries.map { action ->
            val (label, tooltip) = labels.getValue(action)
            val running =
                state.isPlaying &&
                    state.playSingleVoice == action.singleTone &&
                    state.isLooping == action.looping
            Button(strings[label], strings[tooltip], running) {
                action.intents().forEach(actions.dispatch)
            }
        }
    for (pair in buttons.chunked(2)) StripButtons(*pair.toTypedArray())
}

/** A text button of a strip: [label], an optional [tooltip], [active] state, and [onClick]. */
internal class Button(
    val label: String,
    val tooltip: String?,
    val active: Boolean,
    val onClick: () -> Unit,
)

/** A row of equally wide text [buttons]; each is named by its label for assistive technology. */
@Composable
internal fun StripButtons(vararg buttons: Button) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
    ) {
        for (button in buttons) {
            JagButton(
                button.label,
                button.onClick,
                Modifier
                    .weight(
                        1f,
                    ).semantics { contentDescription = button.tooltip ?: button.label },
                active = button.active,
                tooltip = button.tooltip,
            )
        }
    }
}
