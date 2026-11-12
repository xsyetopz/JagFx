package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.core.model.Envelope
import io.github.xsyetopz.jagfx.core.model.Segment
import io.github.xsyetopz.jagfx.core.model.SynthLimits
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.EnvelopeIntent
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.i18n.UiString
import io.github.xsyetopz.jagfx.ui.theme.DISABLED_ALPHA
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.Icons
import io.github.xsyetopz.jagfx.ui.widgets.JagButton
import io.github.xsyetopz.jagfx.ui.widgets.JagCheck
import io.github.xsyetopz.jagfx.ui.widgets.JagIconButton
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText

private val IndexWidth = 24.dp

/**
 * Controls of cell of [slot], opened by its `▼`: time-view option, Snap for
 * envelope, and table of its points with editable ms, unit value, raw position, and raw level.
 */
@Composable
internal fun ControlsPanel(
    slot: SignalChainSlot,
    state: EditorState,
    view: ScreenViewState,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val voice = state.voice ?: return
    val envelope = envelopeOf(voice, slot)
    Column(
        modifier
            .background(colors.panel)
            .border(dimens.border, colors.border)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        JagLabel(strings.format(UiString.ControlsPanelTitle, classicTitleOf(slot)))
        if (slot.isTimeDomain) TimeViewOption(slot, view, actions)
        if (envelope != null) {
            SnapToggle(slot, view, actions)
            PointTable(slot, envelope, voice.duration.value, view, actions)
        }
    }
}

/** Linked / This cell choice of time view of [slot]. */
@Composable
private fun TimeViewOption(
    slot: SignalChainSlot,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val linked = view.isLinked(slot)
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagText(strings[UiString.TimeViewLabel], Modifier.weight(1f))
        for (link in listOf(true, false)) {
            val label = strings[if (link) UiString.TimeViewLinked else UiString.TimeViewThisCell]
            JagButton(label, {
                actions.updateView {
                    it.withCanvas(
                        slot,
                        it.canvas(slot).copy(linked = link, timeView = it.timeViewOf(slot)),
                    )
                }
            }, active = linked == link)
        }
    }
}

/** Snap switch of envelope canvas of [slot]; also placed by filter cells. */
@Composable
internal fun SnapToggle(
    slot: SignalChainSlot,
    view: ScreenViewState,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagCheck(
            view.canvas(slot).snap,
            { on -> actions.updateCanvas(slot) { it.copy(snap = on) } },
            tooltip = strings[UiString.TooltipSnapGrid],
        )
        JagText(strings[UiString.TooltipSnapGrid])
    }
}

/** Points of [envelope] in [slot], one row each, with way to add one at end. */
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
    val unit =
        slot.range
            ?.unit
            .suffix()
            .trim()
    val headers =
        listOf(
            strings[UiString.PointColumnTime],
            strings.format(UiString.PointColumnValue, unit.ifEmpty { "-" }),
            strings[UiString.PointColumnPosition],
            strings[UiString.PointColumnLevel],
        )
    Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
        JagLabel("#", Modifier.width(IndexWidth))
        headers.forEach { JagLabel(it, Modifier.weight(1f)) }
        Row(Modifier.width(dimens.buttonMinWidth)) {}
    }
    segments.indices.forEach { index ->
        PointRow(slot, envelope, index, durationMs, index == selected, actions)
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

/** One row of point table: number, four fields, and delete. */
@Composable
private fun PointRow(
    slot: SignalChainSlot,
    envelope: Envelope,
    index: Int,
    durationMs: Int,
    selected: Boolean,
    actions: EditorActions,
) {
    val strings = LocalStrings.current
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    Row(
        Modifier.background(if (selected) colors.cellSelected else colors.panel),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagText(
            "${index + 1}",
            Modifier
                .width(IndexWidth)
                .clickable { actions.updateCanvas(slot) { it.copy(selectedIndex = index) } },
            color = if (selected) colors.accent else colors.textDim,
        )
        for (field in PointField.entries) {
            PointCell(
                fieldText(field, slot, envelope, index, durationMs),
                enabled = !(field == PointField.Time && index == 0),
                check = { editField(field, it, slot, envelope, index, durationMs) },
                onApply = { actions.dispatch(EnvelopeIntent.SetSegment(slot, index, it)) },
                Modifier.weight(1f),
            )
        }
        JagIconButton(Icons.Close, strings[UiString.MenuDeletePoint], {
            actions.dispatch(EnvelopeIntent.RemoveSegment(slot, index))
        }, enabled = envelope.segments.size > 1)
    }
}

/**
 * Text box showing [value]. Enter or focus loss runs [check]: valid entry goes to [onApply],
 * out-of-range one shows allowed range under box and stays, and anything else reverts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointCell(
    value: String,
    enabled: Boolean,
    check: (String) -> FieldEdit,
    onApply: (Segment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    var error by remember(value) { mutableStateOf<FieldEdit.OutOfRange?>(null) }

    fun commit() {
        if (text == value) return
        when (val edit = check(text)) {
            is FieldEdit.Applied -> {
                onApply(edit.segment)
            }

            is FieldEdit.OutOfRange -> {
                error = edit
            }

            FieldEdit.NotANumber -> {
                text = value
            }
        }
    }
    Column(modifier) {
        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                error = null
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minWidth = dimens.minTarget, minHeight = dimens.controlHeight)
                    .onFocusChanged {
                        val lost = focused && !it.isFocused
                        focused = it.isFocused
                        if (lost) commit()
                    }.onPreviewKeyEvent {
                        val isEnter = it.key == Key.Enter || it.key == Key.NumPadEnter
                        if (isEnter && it.type == KeyEventType.KeyDown) commit()
                        isEnter
                    },
            enabled = enabled,
            singleLine = true,
            textStyle =
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interaction,
            decorationBox = { inner ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = text,
                    innerTextField = inner,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interaction,
                    isError = error != null,
                    contentPadding = PaddingValues(horizontal = dimens.spaceMd),
                )
            },
        )
        error?.let {
            JagText(
                strings.format(UiString.RangeError, it.min, it.max),
                color = colors.warning,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
