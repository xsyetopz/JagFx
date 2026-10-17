package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Filter
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.core.model.Voice
import io.github.xsyetopz.jagfx.core.model.Waveform
import io.github.xsyetopz.jagfx.ui.canvas.FilterFrequencyScale
import io.github.xsyetopz.jagfx.ui.canvas.PoleRef
import io.github.xsyetopz.jagfx.ui.canvas.coefficient
import io.github.xsyetopz.jagfx.ui.canvas.poles
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.FilterIntent
import io.github.xsyetopz.jagfx.ui.editor.RequestIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagBarField
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagCheck
import io.github.xsyetopz.jagfx.ui.widgets.JagField
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.ScrubScale
import io.github.xsyetopz.jagfx.ui.widgets.WaveformSelector
import kotlin.math.roundToInt

private val U16 = 0..SynthLimits.U16_MAX
private val AnyInt = Int.MIN_VALUE..Int.MAX_VALUE
private val PointNumberWidth = 18.dp
private val LabelWidth = 52.dp
private val GainScale = ScrubScale(0f..SynthLimits.U16_MAX.toFloat())

/** Unity gain in dB per raw unit, as the engine applies it. */
private const val GAIN_DB_PER_UNIT = 0.00305f

/** The cells that edit the filter, so the inspector shows the filter for them. */
private val FilterCells =
    setOf(SignalChainSlot.Filter, SignalChainSlot.PoleZero, SignalChainSlot.Bode)

/** The waveform names, in [Waveform.selectable] order. */
private val WaveformNames =
    listOf(
        UiString.WaveOff,
        UiString.WaveSquare,
        UiString.WaveSine,
        UiString.WaveSawtooth,
        UiString.WaveNoise,
    )

/** Channel `0` feeds forward (zeros); channel `1` feeds back (poles). */
private val ChannelNames = listOf("A", "B")

/**
 * Every stored value of the selected cell ([EditorState.selectedTarget]), as the Scala JagFX
 * inspector: the waveform, start and end, and every point of an envelope as exact u16 numbers;
 * the counts, gains, and selected pole of the filter.
 */
@Composable
internal fun Inspector(
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val dimens = JagFxTheme.dimens
    val slot = state.selectedTarget
    Column(
        modifier
            .background(JagFxTheme.colors.panel)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceLg),
    ) {
        JagText(
            LocalStrings.current[titleOf(slot)].uppercase(),
            style = JagFxTheme.typography.heading,
        )
        val voice = state.voice ?: return@Column
        if (slot.isEnvelope) EnvelopeSection(slot, voice, view, actions)
        if (slot in FilterCells) FilterSection(voice.filter, view, actions)
        Box(Modifier.fillMaxWidth().height(dimens.border).background(JagFxTheme.colors.border))
        VoiceSection(state, actions)
    }
}

/** The waveform, start and end, and points of the envelope in [slot], or a way to add it. */
@Composable
private fun ColumnScope.EnvelopeSection(
    slot: SignalChainSlot,
    voice: Voice,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val envelope = envelopeOf(voice, slot)
    if (envelope == null) {
        val pairFirst = pairFirstOf(slot) ?: return
        JagButton(
            strings[UiString.AddEnvelope],
            { actions.dispatch(EnvelopeIntent.SetWaveform(pairFirst, Waveform.Square)) },
            Modifier.fillMaxWidth(),
        )
        return
    }
    if (slot.hasWaveform) {
        WaveformSelector(
            envelope.waveform,
            { actions.dispatch(EnvelopeIntent.SetWaveform(slot, it)) },
            { strings[WaveformNames[Waveform.selectable.indexOf(it)]] },
            Modifier.fillMaxWidth(),
        )
        if (envelope.waveform.isUndefined) {
            JagText(
                "${strings[UiString.WaveUnused]} #${envelope.waveform.id}",
                color = JagFxTheme.colors.warning,
            )
        }
    }
    if (slot.range != null) {
        LabeledRow(strings[UiString.StartLabel]) {
            JagField(envelope.startValue, {
                actions.dispatch(EnvelopeIntent.SetStartValue(slot, it))
            }, AnyInt, it)
        }
        LabeledRow(strings[UiString.EndLabel]) {
            JagField(
                envelope.endValue,
                { actions.dispatch(EnvelopeIntent.SetEndValue(slot, it)) },
                AnyInt,
                it,
            )
        }
    }
    PointTable(slot, envelope, voice.duration.value, view, actions)
}

/**
 * Every point of [envelope] in [slot] with its position and level as stored, a way to remove it,
 * and a way to add one at the end; a click on a point number selects the point in the cell.
 * The selected point also shows its position in ms of a voice [durationMs] long.
 */
@Composable
private fun PointTable(
    slot: SignalChainSlot,
    envelope: Envelope,
    durationMs: Int,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val segments = envelope.segments
    val selected = view.canvas(slot).selectedIndex
    Row(verticalAlignment = Alignment.CenterVertically) {
        JagLabel("${strings[UiString.PointsTitle]} ${segments.size}", Modifier.weight(1f))
        segments.getOrNull(selected)?.let {
            val ms = positionToMs(it.duration.toFloat(), durationMs).roundToInt()
            JagText("${selected + 1}: $ms ms", color = colors.textDim)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
        JagLabel("#", Modifier.width(PointNumberWidth))
        JagLabel(strings[UiString.SegmentDurationLabel], Modifier.weight(1f))
        JagLabel(strings[UiString.SegmentLevelLabel], Modifier.weight(1f))
        Row(Modifier.width(dimens.buttonMinWidth)) {}
    }
    segments.forEachIndexed { index, segment ->
        Row(
            Modifier.background(if (index == selected) colors.cellSelected else colors.panel),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JagText(
                "${index + 1}",
                Modifier
                    .width(PointNumberWidth)
                    .clickable { actions.updateCanvas(slot) { it.copy(selectedIndex = index) } },
                color = if (index == selected) colors.accent else colors.textDim,
            )
            JagField(segment.duration, {
                setSegment(actions, slot, index, segment.copy(duration = it))
            }, U16, Modifier.weight(1f))
            JagField(segment.targetLevel, {
                setSegment(actions, slot, index, segment.copy(targetLevel = it))
            }, U16, Modifier.weight(1f))
            JagIconButton(Icons.Close, strings[UiString.MenuDeletePoint], {
                actions.dispatch(EnvelopeIntent.RemoveSegment(slot, index))
            })
        }
    }
    JagButton(
        strings[UiString.MenuAddPointEnd],
        {
            val last = segments.lastOrNull() ?: Segment(0, 0)
            actions.dispatch(EnvelopeIntent.InsertSegment(slot, segments.size, last))
        },
        Modifier.fillMaxWidth(),
        enabled = segments.size < SynthLimits.MAX_SEGMENTS,
    )
}

private fun setSegment(
    actions: EditorActions,
    slot: SignalChainSlot,
    index: Int,
    segment: Segment,
) = actions.dispatch(EnvelopeIntent.SetSegment(slot, index, segment))

/** The filter's on switch, import and export, zero and pole counts, gains, and selected pole. */
@Composable
private fun FilterSection(
    filter: Filter?,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    JagLabel(strings[UiString.FilterTitle])
    FilterSwitchRow(filter, actions)
    if (filter == null) return
    val counts = listOf(UiString.ZerosLabel, UiString.PolesLabel)
    counts.forEachIndexed { channel, label ->
        LabeledRow(strings[label]) {
            JagField(
                filter.poles(channel).size,
                { actions.dispatch(FilterIntent.SetPoleCount(channel, it)) },
                0..SynthLimits.MAX_POLES,
                it,
            )
        }
    }
    val gains =
        listOf(
            UiString.GainALabel to filter.unityGain0,
            UiString.GainBLabel to filter.unityGain1,
        )
    gains.forEachIndexed { channel, (label, gain) ->
        LabeledRow(strings[label]) {
            JagBarField(
                gain,
                { actions.dispatch(FilterIntent.SetUnityGain(channel, it)) },
                U16,
                GainScale,
                it,
                unit = "${(gain * GAIN_DB_PER_UNIT).roundToInt()}dB",
                gestures = actions.gestureCallbacks(),
            )
        }
    }
    val ref = view.selectedPole?.takeIf { it.pole in filter.poles(it.channel).indices } ?: return
    PoleFields(ref, filter, actions)
}

/** The filter's on switch with its state, and filter import and export. */
@Composable
private fun FilterSwitchRow(
    filter: Filter?,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val state = strings[if (filter != null) UiString.FilterOn else UiString.FilterOff]
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagCheck(
            filter != null,
            { actions.dispatch(FilterIntent.SetEnabled(it)) },
            tooltip = state,
        )
        JagText(state, Modifier.weight(1f))
        JagIconButton(Icons.Import, strings[UiString.ImportFilter], {
            actions.dispatch(RequestIntent.ImportFilter)
        })
        JagIconButton(
            Icons.Export,
            strings[UiString.ExportFilter],
            { actions.dispatch(RequestIntent.ExportFilter) },
            enabled = filter != null,
        )
    }
}

/** The phase and magnitude of the coefficient at [ref], with its frequency and resonance. */
@Composable
private fun PoleFields(
    ref: PoleRef,
    filter: Filter,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val coefficient = filter.coefficient(ref)
    val phaseName = strings[if (ref.phaseIndex == 0) UiString.StartLabel else UiString.EndLabel]
    JagLabel("${ChannelNames[ref.channel]}${ref.pole + 1} $phaseName")
    val hz = FilterFrequencyScale.rawPhaseToFrequency(coefficient.phase).roundToInt()
    LabeledRow(strings[UiString.PoleAngleLabel]) {
        Column {
            JagField(
                coefficient.phase,
                { actions.setCoefficient(ref, it, coefficient.magnitude) },
                U16,
                it,
            )
            JagText("$hz Hz", color = JagFxTheme.colors.textDim)
        }
    }
    val db = magnitudeToDb(coefficient.magnitude).roundToInt()
    LabeledRow(strings[UiString.PoleMagnitudeLabel]) {
        Column {
            JagField(
                coefficient.magnitude,
                { actions.setCoefficient(ref, coefficient.phase, it) },
                U16,
                it,
            )
            JagText("$db dB", color = JagFxTheme.colors.textDim)
        }
    }
}

/** A [label] on the left of [field], which gets the modifier that fills the rest of the row. */
@Composable
internal fun LabeledRow(
    label: String,
    modifier: Modifier = Modifier,
    labelWidth: Dp = LabelWidth,
    field: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagLabel(label, Modifier.width(labelWidth))
        field(Modifier.weight(1f))
    }
}
