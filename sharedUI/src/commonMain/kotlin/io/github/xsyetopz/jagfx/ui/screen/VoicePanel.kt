package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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

private val EdgeGrab = 6.dp
private val BoxLabelWidth = 32.dp
private val USmart16 = 0..SynthLimits.USMART16_MAX

/** Stored [range] of footer bar field, [scale] of its bar, and [unit] of value. */
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
 * Time span that voice bars share, and [onFreeze], which holds span during drag and
 * releases it with `null`.
 */
private class VoiceTimeline(
    val spanMs: Int,
    val onFreeze: (Int?) -> Unit,
)

/**
 * Ten voices as chips, each with bar of its start and length on shared time axis, and
 * on switch of selected voice.
 * Click selects voice, or pastes into it in copy mode; dragging bar moves voice, and
 * dragging its end changes length.
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
    }
}

/** Chip of voice [index]: its number over its bar; dimmed when slot is empty. */
@Composable
private fun VoiceTile(
    index: Int,
    state: EditorState,
    timeline: VoiceTimeline,
    actions: EditorActions,
    modifier: Modifier,
) {
    val colors = JagFxTheme.colors
    val palette = JagFxTheme.palette
    val dimens = JagFxTheme.dimens
    val voice = state.patch.voices[index]
    val strings = LocalStrings.current
    val selected = index == state.selectedVoice
    val shape = RoundedCornerShape(dimens.radius)
    val barColor = if (selected) palette.output else colors.textDim
    Column(
        modifier
            .clip(shape)
            .background(if (selected) colors.cellSelected else colors.button)
            .border(dimens.border, if (selected) colors.accent else colors.border, shape)
            .semantics(mergeDescendants = true) {
                contentDescription = strings.format(UiString.VoiceNumber, index + 1)
                this.selected = selected
            }.clickable(role = Role.RadioButton) { actions.dispatch(SlotIntent.SelectVoice(index)) }
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
 * Drags bar of voice [index]: from within [EdgeGrab] of its end length changes, elsewhere
 * start.
 * Span stays frozen during drag, so bar follows pointer.
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
 * Harmonic box of [row] in bottom bar: its on switch and number, and Semi, Vol, and Del.
 * Its fields are disabled while harmonic is off.
 */
@Composable
internal fun HarmonicBox(
    row: PartialRow,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val partial = row.partial
    val i = row.index
    val on = partial != null
    val name = "${strings[UiString.HarmonicsTitle]} ${i + 1}"
    BoxColumn(modifier) {
        Row(
            Modifier.height(JagFxTheme.dimens.controlHeight),
            horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JagCheck(
                on,
                { actions.dispatch(PartialIntent.TogglePartial(i)) },
                tooltip = strings[UiString.TooltipPartialActive],
            )
            JagText("${i + 1}", color = JagFxTheme.colors.textDim)
        }
        BoxField(
            strings[UiString.PitchLabel],
            name,
            partial?.pitchOffset ?: 0,
            DetuneSpec,
            on,
            actions,
        ) {
            PartialIntent.SetPitchOffset(i, it)
        }
        BoxField(
            strings[UiString.LevelLabel],
            name,
            partial?.amplitude?.value ?: 0,
            LevelSpec,
            on,
            actions,
        ) {
            PartialIntent.SetAmplitude(i, it)
        }
        BoxField(
            strings[UiString.DelayLabel],
            name,
            partial?.delay?.value ?: 0,
            DelaySpec,
            on,
            actions,
        ) {
            PartialIntent.SetDelay(i, it)
        }
    }
}

/** Echo box of bottom bar: Feedback is echo feedback and Delay its delay. */
@Composable
internal fun ReverbBox(
    state: EditorState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val voice = state.voice
    val name = strings[UiString.EchoTitle]
    BoxColumn(modifier) {
        JagLabel(name)
        BoxField(
            strings[UiString.FeedbackLabel],
            name,
            voice?.echo?.feedback?.value ?: 0,
            FeedbackSpec,
            voice != null,
            actions,
        ) {
            VoiceIntent.SetEchoFeedback(it)
        }
        BoxField(
            strings[UiString.DelayLabel],
            name,
            voice?.echo?.delay?.value ?: 0,
            DelaySpec,
            voice != null,
            actions,
        ) {
            VoiceIntent.SetEchoDelay(it)
        }
    }
}

@Composable
private fun BoxColumn(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val dimens = JagFxTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)
    Column(
        modifier
            .border(dimens.border, JagFxTheme.colors.border, shape)
            .padding(dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) { content() }
}

/** [label] and bar field in box; [box] is box name that completes its accessible name. */
@Composable
private fun BoxField(
    label: String,
    box: String,
    value: Int,
    spec: FieldSpec,
    enabled: Boolean,
    actions: EditorActions,
    intent: (Int) -> EditorIntent,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagLabel(label, Modifier.width(BoxLabelWidth))
        PartialField(
            value,
            spec,
            enabled,
            actions,
            Modifier.weight(1f).semantics { contentDescription = "$box $label" },
            intent,
        )
    }
}

/** Bar field that dispatches intent that [intent] makes of each new value. */
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
