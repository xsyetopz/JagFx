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
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton

private val U16Ms = 0..SynthLimits.U16_MAX

/**
 * Voice strip: oscillator, timing and loop fields of selected voice, transport, Load
 * and Save, and Live preview.
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
                strings[UiString.LivePreview],
                strings[UiString.TooltipLivePreview],
                state.trueWave,
            ) { actions.dispatch(ViewIntent.SetTrueWave(!state.trueWave)) },
        )
    }
}

/** Length and Offset of selected voice, and loop start and end of patch. */
@Composable
private fun TimingFields(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val voice = state.voice
    val loop = state.patch.loop
    StripField(
        strings[UiString.DurationLabel],
        strings[UiString.DurationLabel],
        voice?.duration?.value ?: 0,
        { actions.dispatch(VoiceIntent.SetDuration(it)) },
        "ms",
        voice != null,
        U16Ms,
    )
    StripField(
        strings[UiString.OffsetLabel],
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

/**
 * Play, Loop, and Stop as icons for selected voice, over Play All and Loop All; button
 * of running playback is active.
 */
@Composable
private fun Transport(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current

    fun running(action: PlayAction) =
        state.isPlaying &&
            state.playSingleVoice == action.singleTone &&
            state.isLooping == action.looping

    fun start(action: PlayAction) = action.intents().forEach(actions.dispatch)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
    ) {
        JagIconButton(
            Icons.Play,
            strings[UiString.TooltipPlayTone],
            { start(PlayAction.Play) },
            Modifier.weight(1f),
            active = running(PlayAction.Play),
            fill = true,
        )
        JagIconButton(
            Icons.Loop,
            strings[UiString.TooltipLoopTone],
            { start(PlayAction.Loop) },
            Modifier.weight(1f),
            active = running(PlayAction.Loop),
            fill = true,
        )
        JagIconButton(
            Icons.Stop,
            strings[UiString.TooltipStop],
            { actions.dispatch(RequestIntent.Stop) },
            Modifier.weight(1f),
            fill = true,
        )
    }
    StripButtons(
        Button(
            strings[UiString.PlayAll],
            strings[UiString.TooltipPlayAll],
            running(PlayAction.PlayAll),
        ) { start(PlayAction.PlayAll) },
        Button(
            strings[UiString.LoopAll],
            strings[UiString.TooltipLoopAll],
            running(PlayAction.LoopAll),
        ) { start(PlayAction.LoopAll) },
    )
}

/** Text button of strip: [label], optional [tooltip], [active] state, and [onClick]. */
internal class Button(
    val label: String,
    val tooltip: String?,
    val active: Boolean,
    val onClick: () -> Unit,
)

/** Row of equally wide text [buttons]; each is named by its label for assistive technology. */
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
                fill = true,
            )
        }
    }
}
