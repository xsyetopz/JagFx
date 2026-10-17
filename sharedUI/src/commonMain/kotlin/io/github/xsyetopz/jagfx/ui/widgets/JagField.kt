package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/** The integer in [text] clamped to [range], or null when [text] is not an integer. */
internal fun parseClampedInt(
    text: String,
    range: IntRange,
): Int? {
    val parsed = text.trim().toLongOrNull() ?: return null
    return parsed.coerceIn(range.first.toLong(), range.last.toLong()).toInt()
}

/**
 * An 18 dp input box for an integer in [range], right-aligned on a near-black field with an
 * accent border while focused.
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
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)
    var text by remember(value) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }

    fun commit() {
        val parsed = parseClampedInt(text, range)
        if (parsed != null && parsed != value) onValueCommit(parsed)
        text = (parsed ?: value).toString()
    }

    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier =
            modifier
                .height(dimens.controlHeight)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(colors.input, shape)
                .border(dimens.border, if (focused) colors.accent else colors.border, shape)
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
                modifier = Modifier.fillMaxHeight().padding(horizontal = dimens.spaceMd),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { inner() }
                if (unit.isNotEmpty()) {
                    JagText(unit, color = colors.textSubtle, style = JagFxTheme.typography.label)
                }
            }
        },
    )
}

/**
 * A [JagField] with a 4 dp bar under it, the Scala JagFX value control.
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
                .height(dimens.barHeight)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(colors.root)
                .border(dimens.border, colors.border)
                .then(if (enabled) Modifier.scrub(scrub) else Modifier),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(colors.accent))
        }
    }
}
