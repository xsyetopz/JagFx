package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.ValueUnit
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.JagToggleGroup
import io.github.xsyetopz.jagfx.ui.widgets.WaveformSelector
import kotlin.math.roundToInt

private const val HZ_DECIMALS = 1
private const val GAP_DECIMALS = 2
private val GapMsRange =
    ValueUnit.Ms.fromRaw(Int.MIN_VALUE.toDouble())..ValueUnit.Ms.fromRaw(Int.MAX_VALUE.toDouble())

/**
 * Waveform buttons, Min, Max, and read-only average of envelope in [slot], in Hz.
 * Without envelope fields are disabled.
 */
@Composable
internal fun WaveformAndHzFields(
    slot: SignalChainSlot,
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val envelope = envelopeOf(state.voice, slot)
    val title = strings[titleOf(slot)]
    WaveformSelector(
        envelope?.waveform ?: Waveform.Off,
        { actions.dispatch(EnvelopeIntent.SetWaveform(slot, it)) },
        { waveformName(strings, it) },
        Modifier,
        enabled = state.voice != null,
    )
    val min = envelope?.startValue ?: 0
    val max = envelope?.endValue ?: 0
    StripField(
        strings[UiString.StartLabel],
        "$title ${strings[UiString.StartLabel]} Hz",
        min,
        { actions.dispatch(EnvelopeIntent.SetStartValue(slot, it)) },
        "Hz",
        enabled = envelope != null,
    )
    StripField(
        strings[UiString.EndLabel],
        "$title ${strings[UiString.EndLabel]} Hz",
        max,
        { actions.dispatch(EnvelopeIntent.SetEndValue(slot, it)) },
        "Hz",
        enabled = envelope != null,
    )
    StripReadout(
        strings[UiString.LabelAverage],
        "$title ${strings[UiString.LabelAverage]}",
        averageIn(ValueUnit.Hz, min, max),
        "Hz",
        HZ_DECIMALS,
    )
}

/**
 * Module strip of Freq or Amp row: waveform, Min, Max, and Av of rate envelope
 * [rate] ([SignalChainSlot.VibratoRate] or [SignalChainSlot.TremoloRate]) of that row's LFO.
 */
@Composable
internal fun LfoStrip(
    rate: SignalChainSlot,
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    StripColumn(modifier) { WaveformAndHzFields(rate, state, actions) }
}

/**
 * Gate strip of Output row: Off and On, and Min Gap, Max Gap, and Av Gap in ms of
 * Gap off envelope.
 */
@Composable
internal fun GateStrip(
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val voice = state.voice
    val envelope = envelopeOf(voice, SignalChainSlot.GapOff)
    val title = strings[titleOf(SignalChainSlot.GapOff)]
    StripColumn(modifier) {
        JagToggleGroup(
            listOf(false, true),
            envelope != null,
            {
                val waveform = if (it) Waveform.Square else Waveform.Off
                actions.dispatch(EnvelopeIntent.SetWaveform(SignalChainSlot.GapOff, waveform))
            },
            enabled = voice != null,
            tooltip = { strings[if (it) UiString.OnLabel else UiString.OffLabel] },
        ) { on ->
            JagText(
                strings[if (on) UiString.OnLabel else UiString.OffLabel],
                style = MaterialTheme.typography.labelSmall,
            )
        }
        val min = envelope?.startValue ?: 0
        val max = envelope?.endValue ?: 0
        StripMsField(
            strings[UiString.StartLabel],
            "$title ${strings[UiString.StartLabel]} ms",
            ValueUnit.Ms.fromRaw(min.toDouble()),
            { actions.dispatch(EnvelopeIntent.SetStartValue(SignalChainSlot.GapOff, gapRaw(it))) },
            GapMsRange,
            envelope != null,
        )
        StripMsField(
            strings[UiString.EndLabel],
            "$title ${strings[UiString.EndLabel]} ms",
            ValueUnit.Ms.fromRaw(max.toDouble()),
            { actions.dispatch(EnvelopeIntent.SetEndValue(SignalChainSlot.GapOff, gapRaw(it))) },
            GapMsRange,
            envelope != null,
        )
        StripReadout(
            strings[UiString.LabelAverage],
            "$title ${strings[UiString.LabelAverage]}",
            averageIn(ValueUnit.Ms, min, max),
            "ms",
            GAP_DECIMALS,
        )
    }
}

private fun gapRaw(ms: Double): Int = ValueUnit.Ms.toRaw(ms).roundToInt()
