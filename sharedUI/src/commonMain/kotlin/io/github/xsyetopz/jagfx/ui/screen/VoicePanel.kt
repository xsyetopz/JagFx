package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.editor.EditorIntent
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.PartialIntent
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.editor.VoiceIntent
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.SynthPalette
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagBarField
import io.github.xsyetopz.jagfx.ui.widgets.JagCheck
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.ScrubScale

/** Ten units of `PitchOffset` are one semitone. */
private const val DETUNE_SCRUB_LIMIT = 240f
private const val PERCENT_MAX = 100f
private const val DELAY_SCRUB_MAX = 2000f
private const val EMPTY_ALPHA = 0.45f
private const val DETUNE_SHARE = 1.5f

private val EdgeGrab = 6.dp
private val HarmonicIndexWidth = 30.dp
private val USmart16 = 0..SynthLimits.USMART16_MAX

/** The stored [range] of a footer bar field, the [scale] of its bar, and the [unit] of a value. */
private class FieldSpec(
    val range: IntRange,
    val scale: ScrubScale,
    val unit: (Int) -> String,
)

private val DetuneSpec =
    FieldSpec(
        SynthLimits.SMART16_MIN..SynthLimits.SMART16_MAX,
        ScrubScale(-DETUNE_SCRUB_LIMIT..DETUNE_SCRUB_LIMIT, defaultValue = 0f),
        ::semitones,
    )
private val LevelSpec =
    FieldSpec(
        1..SynthLimits.USMART16_MAX,
        ScrubScale(1f..PERCENT_MAX, defaultValue = PERCENT_MAX),
    ) {
        "%"
    }
private val DelaySpec =
    FieldSpec(USmart16, ScrubScale(0f..DELAY_SCRUB_MAX, defaultValue = 0f)) { "ms" }
private val FeedbackSpec =
    FieldSpec(USmart16, ScrubScale(0f..PERCENT_MAX, defaultValue = 0f)) { "%" }

/**
 * The time span that the voice bars share, and [onFreeze], which holds a span during a drag and
 * releases it with `null`.
 */
private class VoiceTimeline(
    val spanMs: Int,
    val onFreeze: (Int?) -> Unit,
)

/**
 * The ten voices as chips, each with a bar of its start and length on a shared time axis, and the
 * on, copy and reset tools of the selected voice.
 * A click selects a voice, or pastes into it in copy mode; dragging a bar moves the voice, and
 * dragging its end changes the length.
 */
@Composable
internal fun VoiceChips(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    val voice = state.voice
    var frozen by remember { mutableStateOf<Int?>(null) }
    val timeline = VoiceTimeline(frozen ?: timelineSpan(state.patch)) { frozen = it }
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (index in 0 until SynthLimits.VOICE_SLOTS) {
            VoiceTile(
                index,
                state,
                timeline,
                actions,
                Modifier.width(dimens.voiceChipWidth).height(dimens.voiceChipHeight),
            )
        }
        JagIconButton(
            Icons.Power,
            strings[UiString.MenuVoiceEnabled],
            { actions.dispatch(SlotIntent.SetVoiceEnabled(state.selectedVoice, voice == null)) },
            active = voice != null,
        )
        JagIconButton(
            Icons.Copy,
            strings[UiString.TooltipCopyVoice],
            { actions.dispatch(SlotIntent.CopyVoice) },
            enabled = voice != null,
            active = state.isCopyMode,
        )
        JagIconButton(
            Icons.Reset,
            strings[UiString.TooltipResetVoice],
            { actions.dispatch(SlotIntent.ResetVoice) },
            enabled = voice != null,
        )
    }
}

/** The chip of voice [index]: its number over its bar; dimmed when the slot is empty. */
@Composable
private fun VoiceTile(
    index: Int,
    state: EditorState,
    timeline: VoiceTimeline,
    actions: EditorActions,
    modifier: Modifier,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val voice = state.patch.voices[index]
    val selected = index == state.selectedVoice
    val shape = RoundedCornerShape(dimens.radius)
    val barColor = if (selected) SynthPalette.Output else colors.textDim
    Column(
        modifier
            .clip(shape)
            .background(if (selected) colors.cellSelected else colors.button)
            .border(dimens.border, if (selected) colors.accent else colors.border, shape)
            .clickable { actions.dispatch(SlotIntent.SelectVoice(index)) }
            .voiceDrag(index, state, timeline, actions)
            .alpha(if (voice == null) EMPTY_ALPHA else 1f)
            .padding(horizontal = dimens.spaceSm, vertical = dimens.spaceXs),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        JagText("${index + 1}", color = if (selected) colors.accent else colors.textDim)
        Canvas(Modifier.fillMaxWidth().height(dimens.spaceSm)) {
            drawRect(colors.input)
            if (voice != null) {
                val span = timeline.spanMs.toFloat()
                val start = size.width * voice.offset.value / span
                val length = size.width * voice.duration.value / span
                drawRect(barColor, Offset(start, 0f), Size(length.coerceAtLeast(1f), size.height))
            }
        }
    }
}

/**
 * Drags the bar of voice [index]: from within [EdgeGrab] of its end the length changes, elsewhere
 * the start.
 * The span stays frozen during the drag, so the bar follows the pointer.
 */
@Composable
private fun Modifier.voiceDrag(
    index: Int,
    state: EditorState,
    timeline: VoiceTimeline,
    actions: EditorActions,
): Modifier {
    val latestVoice by rememberUpdatedState(state.patch.voices[index])
    val latestSpan by rememberUpdatedState(timeline.spanMs)
    return pointerInput(index) {
        val edge = EdgeGrab.toPx()
        var drag: LaneDrag? = null
        var start = 0 to 0
        var total = 0f
        var msPerPx = 0f

        fun finish() {
            if (drag != null) {
                actions.endGesture()
                timeline.onFreeze(null)
            }
            drag = null
        }
        detectDragGestures(
            onDragStart = start@{ position ->
                val voice = latestVoice ?: return@start
                val width = size.width.toFloat().coerceAtLeast(1f)
                msPerPx = latestSpan / width
                val barStart = voice.offset.value / msPerPx
                val barEnd = (voice.offset.value + voice.duration.value) / msPerPx
                drag = laneDragAt(position.x, barStart, barEnd, edge)
                start = voice.offset.value to voice.duration.value
                total = 0f
                timeline.onFreeze(latestSpan)
                actions.dispatch(SlotIntent.SelectVoice(index))
                actions.beginGesture()
            },
            onDragEnd = ::finish,
            onDragCancel = ::finish,
        ) { change, amount ->
            val mode = drag ?: return@detectDragGestures
            change.consume()
            total += amount.x
            val (offset, duration) = draggedTimes(mode, start.first, start.second, total * msPerPx)
            actions.dispatch(
                if (mode ==
                    LaneDrag.Move
                ) {
                    VoiceIntent.SetOffset(offset)
                } else {
                    VoiceIntent.SetDuration(duration)
                },
            )
        }
    }
}

/**
 * The voice part of the properties panel: a table of the harmonics of the selected voice, one row
 * each with its on switch, detune, level and delay, and the echo.
 */
@Composable
internal fun VoiceSection(
    state: EditorState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    val voice = state.voice ?: return
    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
        JagLabel(strings[UiString.HarmonicsTitle].uppercase())
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Spacer(Modifier.width(HarmonicIndexWidth))
            JagLabel(strings[UiString.DetuneLabel], Modifier.weight(DETUNE_SHARE))
            JagLabel(strings[UiString.LevelLabel], Modifier.weight(1f))
            JagLabel(strings[UiString.DelayLabel], Modifier.weight(1f))
        }
        for (row in partialRows(voice)) HarmonicRow(row, actions)
        JagLabel(strings[UiString.EchoTitle].uppercase())
        val echo = voice.echo
        LabeledRow(strings[UiString.EchoDelayLabel]) { field ->
            PartialField(echo.delay.value, DelaySpec, true, actions, field) {
                VoiceIntent.SetEchoDelay(it)
            }
        }
        LabeledRow(strings[UiString.EchoFeedbackLabel]) { field ->
            PartialField(echo.feedback.value, FeedbackSpec, true, actions, field) {
                VoiceIntent.SetEchoFeedback(it)
            }
        }
    }
}

/** The on switch and number, detune, level and delay of the harmonic in [row]. */
@Composable
private fun HarmonicRow(
    row: PartialRow,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val dimens = JagFxTheme.dimens
    val partial = row.partial
    val i = row.index
    val on = partial != null
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
        Row(
            Modifier.width(HarmonicIndexWidth).height(dimens.controlHeight),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JagCheck(
                on,
                { actions.dispatch(PartialIntent.TogglePartial(i)) },
                tooltip = strings[UiString.TooltipPartialActive],
            )
            JagText("${i + 1}", color = JagFxTheme.colors.textDim)
        }
        PartialField(
            partial?.pitchOffset ?: 0,
            DetuneSpec,
            on,
            actions,
            Modifier.weight(DETUNE_SHARE),
        ) {
            PartialIntent.SetPitchOffset(i, it)
        }
        PartialField(partial?.amplitude?.value ?: 0, LevelSpec, on, actions, Modifier.weight(1f)) {
            PartialIntent.SetAmplitude(i, it)
        }
        PartialField(partial?.delay?.value ?: 0, DelaySpec, on, actions, Modifier.weight(1f)) {
            PartialIntent.SetDelay(i, it)
        }
    }
}

/** A bar field that dispatches the intent that [intent] makes of each new value. */
@Composable
private fun PartialField(
    value: Int,
    spec: FieldSpec,
    enabled: Boolean,
    actions: EditorActions,
    modifier: Modifier = Modifier,
    intent: (Int) -> EditorIntent,
) {
    JagBarField(
        value,
        { actions.dispatch(intent(it)) },
        spec.range,
        spec.scale,
        modifier,
        enabled,
        spec.unit(value),
        actions.gestureCallbacks(),
    )
}
