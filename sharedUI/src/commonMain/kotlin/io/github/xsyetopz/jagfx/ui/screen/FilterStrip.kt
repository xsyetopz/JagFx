package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.canvas.FilterFrequencyScale
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.canvas.coefficient
import io.github.xsyetopz.jagfx.ui.canvas.poles
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.FilterIntent
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagBarField
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.JagToggleGroup
import io.github.xsyetopz.jagfx.ui.widgets.ScrubScale
import kotlin.math.roundToInt

private val U16 = 0..SynthLimits.U16_MAX
private val GainScale = ScrubScale(0f..SynthLimits.U16_MAX.toFloat())

/** Unity gain in dB per raw unit, as the engine applies it. */
private const val GAIN_DB_PER_UNIT = 0.00305f

/** Channel `0` feeds forward (zeros); channel `1` feeds back (poles). */
private val ChannelNames = listOf("A", "B")

/**
 * The strip left of the filter column: Off and On, Import Filter, Export Filter, the zero and
 * pole counts, the unity gains, Snap for the pole and zero graph, and the selected pole.
 */
@Composable
internal fun FilterStrip(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val voice = state.voice
    val filter = voice?.filter
    StripColumn(modifier) {
        JagToggleGroup(
            listOf(false, true),
            filter != null,
            { actions.dispatch(FilterIntent.SetEnabled(it)) },
            enabled = voice != null,
            tooltip = { strings[if (it) UiString.FilterOn else UiString.FilterOff] },
        ) { on ->
            JagText(
                strings[if (on) UiString.OnLabel else UiString.OffLabel],
                style = JagFxTheme.typography.label,
            )
        }
        StripButtons(
            Button(strings[UiString.ImportFilter], strings[UiString.TooltipImportFilter], false) {
                actions.dispatch(RequestIntent.ImportFilter)
            },
        )
        StripButtons(
            Button(strings[UiString.ExportFilter], strings[UiString.TooltipExportFilter], false) {
                actions.dispatch(RequestIntent.ExportFilter)
            },
        )
        if (filter != null) {
            FilterFields(filter, view, actions)
        }
    }
}

@Composable
private fun FilterFields(
    filter: Filter,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    listOf(UiString.ZerosLabel, UiString.PolesLabel).forEachIndexed { channel, label ->
        StripField(
            strings[label],
            strings[label],
            filter.poles(channel).size,
            { actions.dispatch(FilterIntent.SetPoleCount(channel, it)) },
            "",
            range = 0..SynthLimits.MAX_POLES,
        )
    }
    listOf(
        UiString.GainALabel to filter.unityGain0,
        UiString.GainBLabel to filter.unityGain1,
    ).forEachIndexed { index, (label, gain) ->
        StripRow(strings[label], strings[label]) {
            JagBarField(
                gain,
                { actions.dispatch(FilterIntent.SetUnityGain(index, it)) },
                U16,
                GainScale,
                it,
                unit = "${(gain * GAIN_DB_PER_UNIT).roundToInt()}dB",
                gestures = actions.gestureCallbacks(),
            )
        }
    }
    StripButtons(
        Button(
            strings[UiString.SnapLabel],
            strings[UiString.TooltipSnapGrid],
            view.canvas(SignalChainSlot.PoleZero).snap,
        ) { actions.updateCanvas(SignalChainSlot.PoleZero) { it.copy(snap = !it.snap) } },
    )
    val ref = view.selectedPole?.takeIf { it.pole in filter.poles(it.channel).indices }
    if (ref != null) PoleFields(ref, filter, actions)
}

/** The frequency in Hz and resonance in dB of the coefficient at [ref], and the raw fields that edit it. */
@Composable
private fun PoleFields(
    ref: PoleRef,
    filter: Filter,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val coefficient = filter.coefficient(ref)
    val phaseName = strings[if (ref.phaseIndex == 0) UiString.StartLabel else UiString.EndLabel]
    JagText(
        "${ChannelNames[ref.channel]}${ref.pole + 1} $phaseName",
        color = JagFxTheme.colors.text,
        style = JagFxTheme.typography.label,
    )
    val hz = FilterFrequencyScale.rawPhaseToFrequency(coefficient.phase).roundToInt()
    StripField(
        strings[UiString.PoleAngleLabel],
        strings[UiString.PoleAngleLabel],
        coefficient.phase,
        { actions.setCoefficient(ref, it, coefficient.magnitude) },
        "",
        range = U16,
    )
    JagText("$hz Hz", color = JagFxTheme.colors.textDim)
    StripField(
        strings[UiString.PoleMagnitudeLabel],
        strings[UiString.PoleMagnitudeLabel],
        coefficient.magnitude,
        { actions.setCoefficient(ref, coefficient.phase, it) },
        "",
        range = U16,
    )
    JagText(
        "${magnitudeToDb(coefficient.magnitude).roundToInt()} dB",
        color = JagFxTheme.colors.textDim,
    )
}
