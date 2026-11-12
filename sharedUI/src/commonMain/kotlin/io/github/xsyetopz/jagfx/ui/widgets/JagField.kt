package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import io.github.xsyetopz.jagfx.ui.canvas.GestureCallbacks
import io.github.xsyetopz.jagfx.ui.theme.DISABLED_ALPHA
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** The integer in [text] clamped to [range], or null when [text] is not an integer. */
internal fun parseClampedInt(
    text: String,
    range: IntRange,
): Int? {
    val parsed = text.trim().toLongOrNull() ?: return null
    return parsed.coerceIn(range.first.toLong(), range.last.toLong()).toInt()
}

/**
 * An input box at least 24 dp high for an integer in [range], right-aligned on a near-black field with
 * a 2 dp accent ring while focused.
 * Enter or focus loss commits through [onValueCommit]; out-of-range input is clamped, and text
 * that is not a number reverts to [value].
 * A non-empty [unit] shows dim after the number.
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
 * A [JagField] for a number with [decimals] digits after the point, clamped to [range].
 * [onValueCommit] runs only when the committed text differs from the shown one.
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

/** [value] with [decimals] digits after the point. */
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

/** The input box of the fields: shows [value], and [onCommit] maps the edited text to the text to show. */
@Composable
private fun JagTextField(
    value: String,
    onCommit: (String) -> String,
    modifier: Modifier,
    enabled: Boolean,
    unit: String,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)
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
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(colors.input, shape)
                .border(dimens.border, colors.controlBorder, shape)
                .focusRing(focused, colors.accent, dimens.focusRing, dimens.radius)
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
        textStyle = JagFxTheme.typography.body.copy(color = colors.text, textAlign = TextAlign.End),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.padding(horizontal = dimens.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { inner() }
                if (unit.isNotEmpty()) {
                    JagText(unit, color = colors.textDim, style = JagFxTheme.typography.label)
                }
            }
        },
    )
}

/**
 * A [JagField] with a 4 dp bar under it in a 24 dp hit area, the Scala JagFX value control.
 * The bar fills to the position of [value] in the [scale] range; dragging it, the wheel over it,
 * and a double click set the value through [onValueChange], each one undo step bracketed by
 * [gestures].
 * Typing accepts any value in [range], which can be wider than the bar.
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
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val scrub = rememberUpdatedState(ScrubState(value, scale, onValueChange, gestures))
    val fraction = ScrubMath.fraction(value.toFloat(), scale.range)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
        JagField(value, onValueChange, range, Modifier.fillMaxWidth(), enabled, unit)
        Box(
            Modifier
                .fillMaxWidth()
                .height(dimens.minTarget)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .then(if (enabled) Modifier.scrub(scrub) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(dimens.barHeight)
                    .background(colors.root)
                    .border(dimens.border, colors.controlBorder),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(colors.accent))
            }
        }
    }
}
