package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks
import io.github.xsyetopz.jagfx.ui.theme.DISABLED_ALPHA
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Integer in [text] clamped to [range], or null when [text] is not integer. */
internal fun parseClampedInt(
    text: String,
    range: IntRange,
): Int? {
    val parsed = text.trim().toLongOrNull() ?: return null
    return parsed.coerceIn(range.first.toLong(), range.last.toLong()).toInt()
}

/**
 * Input box at least 24 dp high for integer in [range], right-aligned on near-black field with
 * 2 dp accent ring while focused.
 * Enter or focus loss commits through [onValueCommit]; out-of-range input is clamped, and text
 * that is not number reverts to [value].
 * Non-empty [unit] shows dim after number.
 */
@Composable
internal fun JagField(
    value: Int,
    onValueCommit: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unit: String = "",
) {
    JagTextField(
        value.toString(),
        { edited ->
            val parsed = parseClampedInt(edited, range)
            if (parsed != null && parsed != value) onValueCommit(parsed)
            (parsed ?: value).toString()
        },
        modifier,
        enabled,
        unit,
    )
}

/**
 * [JagField] for number with [decimals] digits after point, clamped to [range].
 * [onValueCommit] runs only when committed text differs from shown one.
 */
@Composable
internal fun JagDecimalField(
    value: Double,
    onValueCommit: (Double) -> Unit,
    range: ClosedFloatingPointRange<Double>,
    decimals: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unit: String = "",
) {
    val shown = formatDecimal(value, decimals)
    JagTextField(
        shown,
        { edited ->
            val parsed =
                edited
                    .trim()
                    .replace(',', '.')
                    .toDoubleOrNull()
                    ?.coerceIn(range)
            val text = parsed?.let { formatDecimal(it, decimals) } ?: shown
            if (parsed != null && text != shown) onValueCommit(parsed)
            text
        },
        modifier,
        enabled,
        unit,
    )
}

/** [value] with [decimals] digits after point. */
internal fun formatDecimal(
    value: Double,
    decimals: Int,
): String {
    val scale = 10.0.pow(decimals).toLong()
    val scaled = (abs(value) * scale).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    if (decimals == 0) return "$sign$scaled"
    return "$sign${scaled / scale}.${(scaled % scale).toString().padStart(decimals, '0')}"
}

/** Input box of fields: shows [value], and [onCommit] maps edited text to text to show. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JagTextField(
    value: String,
    onCommit: (String) -> String,
    modifier: Modifier,
    enabled: Boolean,
    unit: String,
) {
    val dimens = JagFxTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }

    fun commit() {
        text = onCommit(text)
    }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier =
            modifier
                .defaultMinSize(minWidth = dimens.minTarget, minHeight = dimens.controlHeight)
                .onFocusChanged {
                    val lost = focused && !it.isFocused
                    focused = it.isFocused
                    if (lost) commit()
                }.onPreviewKeyEvent {
                    val isEnter = it.key == Key.Enter || it.key == Key.NumPadEnter
                    if (isEnter && it.type == KeyEventType.KeyDown) {
                        commit()
                        true
                    } else {
                        isEnter
                    }
                },
        enabled = enabled,
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        interactionSource = interaction,
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = text,
                innerTextField = inner,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                suffix =
                    if (unit.isEmpty()) {
                        null
                    } else {
                        { JagText(unit, style = MaterialTheme.typography.labelSmall) }
                    },
                contentPadding = PaddingValues(horizontal = dimens.spaceMd),
            )
        },
    )
}

/**
 * [JagField] with stock Material slider under it, Scala JagFX value control.
 * Slider covers [scale] range; dragging it or using its keys sets value through
 * [onValueChange], and wheel over it and double click do too. Drag is one undo step
 * bracketed by [gestures].
 * Typing accepts any value in [range], which can be wider than slider.
 * Field shows stored value divided by 10 to [decimals], and [label] sits left of it.
 */
@Composable
internal fun JagBarField(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    scale: ScrubScale,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unit: String = "",
    gestures: GestureCallbacks = GestureCallbacks(),
    label: String = "",
    decimals: Int = 0,
) {
    val scrub = rememberUpdatedState(ScrubState(value, scale, onValueChange, gestures))
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect {
            when (it) {
                is DragInteraction.Start -> scrub.value.gestures.onStart()
                is DragInteraction.Stop, is DragInteraction.Cancel -> scrub.value.gestures.onEnd()
            }
        }
    }
    val dimens = JagFxTheme.dimens
    val factor = 10.0.pow(decimals)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JagLabel(label, Modifier.weight(1f))
            JagDecimalField(
                value / factor,
                { onValueChange((it * factor).roundToInt()) },
                range.first / factor..range.last / factor,
                decimals,
                Modifier.width(dimens.fieldWidth),
                enabled,
                unit,
            )
        }
        Slider(
            value = value.toFloat().coerceIn(scale.range),
            onValueChange = { onValueChange(it.roundToInt()) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(JagFxTheme.dimens.minTarget)
                    .then(if (enabled) Modifier.scrub(scrub) else Modifier),
            enabled = enabled,
            valueRange = scale.range,
            interactionSource = interaction,
        )
    }
}
