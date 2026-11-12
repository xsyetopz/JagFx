package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.FilterIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.WaveformSelector

/** Share of full strength at which rack shows while selected tone is off. */
private const val DIMMED_ALPHA = 0.35f

/** What turning on module that starts at [pairFirst] does, or `null` for slot that starts no module. */
internal fun emptyPromptKey(pairFirst: SignalChainSlot): UiString? =
    when (pairFirst) {
        SignalChainSlot.VibratoRate -> UiString.EmptyVibrato
        SignalChainSlot.TremoloRate -> UiString.EmptyTremolo
        SignalChainSlot.GapOff -> UiString.EmptyGate
        else -> null
    }

/**
 * Empty scope for module that is off: what turning it on does, and waveform buttons that
 * create module starting at [pairFirst].
 */
@Composable
internal fun EmptyModulePrompt(
    pairFirst: SignalChainSlot,
    key: UiString,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    PromptBox(modifier.fillMaxSize()) {
        JagText(strings[key], textAlign = TextAlign.Center, wrap = true)
        WaveformSelector(
            Waveform.Off,
            { actions.dispatch(EnvelopeIntent.SetWaveform(pairFirst, it)) },
            { waveformName(strings, it) },
        )
    }
}

/** Empty filter scope: says that filter is off and offers On. */
@Composable
internal fun FilterOffPrompt(
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    PromptBox(modifier.fillMaxSize()) {
        JagText(strings[UiString.EmptyFilter], textAlign = TextAlign.Center, wrap = true)
        JagButton(strings[UiString.OnLabel], { actions.dispatch(FilterIntent.SetEnabled(true)) })
    }
}

/**
 * Rack dimmed while selected tone is off, with prompt that names tone and offers
 * Turn on. [content] is rack.
 */
@Composable
internal fun ToneOffOverlay(
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (state.voice != null) {
        Box(modifier) { content() }
        return
    }
    val strings = LocalStrings.current
    Box(modifier) {
        Box(Modifier.fillMaxSize().alpha(DIMMED_ALPHA)) { content() }
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            PromptBox {
                JagText(
                    strings.format(UiString.ToneOffTitle, state.selectedVoice + 1),
                    style = MaterialTheme.typography.titleSmall,
                )
                JagText(strings[UiString.ToneOffHint], textAlign = TextAlign.Center, wrap = true)
                JagButton(
                    strings[UiString.TurnOn],
                    { actions.dispatch(SlotIntent.SetVoiceEnabled(state.selectedVoice, true)) },
                )
            }
        }
    }
}

@Composable
private fun PromptBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Caller sizes box: empty scopes fill their cell, tone-off card wraps its text.
    val dimens = JagFxTheme.dimens
    Box(modifier.background(JagFxTheme.palette.scope), Alignment.Center) {
        Column(
            Modifier.padding(dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}
