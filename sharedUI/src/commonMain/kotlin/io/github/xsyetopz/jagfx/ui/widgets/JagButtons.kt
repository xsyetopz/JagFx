package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import io.github.xsyetopz.jagfx.ui.theme.DISABLED_ALPHA
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.LocalContentColor

/** Distance in pixels between a control and its tooltip. */
private const val TOOLTIP_GAP_PX = 4

/** Draws [icon] in the current content color. */
@Composable
internal fun PainterIcon(
    icon: IconPainter,
    modifier: Modifier = Modifier,
    size: Dp = JagFxTheme.dimens.iconSize,
) {
    val color = LocalContentColor.current
    Canvas(modifier.size(size)) { icon(color) }
}

/**
 * A flat 18 dp button.
 * An [active] button has accent text and an accent border instead of a fill, as in the Scala JagFX.
 * [tooltip] shows while the pointer rests on it.
 */
@Composable
internal fun JagButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    tooltip: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radius)
    val background =
        when {
            active -> colors.buttonActive
            hovered && enabled -> colors.buttonHover
            else -> colors.button
        }
    JagTooltip(tooltip, hovered, modifier) {
        Row(
            modifier =
                Modifier
                    .height(dimens.controlHeight)
                    .defaultMinSize(minWidth = dimens.buttonMinWidth)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .background(background, shape)
                    .border(dimens.border, if (active) colors.accent else colors.border, shape)
                    .hoverable(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ).padding(horizontal = dimens.spaceMd),
            horizontalArrangement =
                Arrangement.spacedBy(
                    dimens.spaceMd,
                    Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (active) colors.accent else colors.textButton,
            ) {
                content()
            }
        }
    }
}

/** A [JagButton] with a [text] label. */
@Composable
internal fun JagButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    tooltip: String? = null,
) {
    JagButton(onClick, modifier, enabled, active, tooltip) {
        JagText(text, style = JagFxTheme.typography.label)
    }
}

/** A square [JagButton] that draws [icon]; [tooltip] names it. */
@Composable
internal fun JagIconButton(
    icon: IconPainter,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    JagButton(onClick, modifier, enabled, active, tooltip) { PainterIcon(icon) }
}

/** A row of joined buttons; the one equal to [selected] is active. */
@Composable
internal fun <T> JagToggleGroup(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tooltip: (T) -> String? = { null },
    item: @Composable RowScope.(T) -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceXs)) {
        options.forEach { option ->
            JagButton(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                active = option == selected,
                tooltip = tooltip(option),
            ) { item(option) }
        }
    }
}

/** A 12 dp check box: an input-colored box with an accent border and mark when [checked]. */
@Composable
internal fun JagCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tooltip: String? = null,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radius)
    JagTooltip(tooltip, hovered, modifier) {
        Box(
            modifier =
                Modifier
                    .size(dimens.iconSize)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .background(colors.input, shape)
                    .border(dimens.border, if (checked) colors.accent else colors.border, shape)
                    .hoverable(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = Role.Checkbox,
                    ) { onCheckedChange(!checked) },
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Box(Modifier.size(CheckMarkSize).background(colors.accent))
        }
    }
}

private val CheckMarkSize = 6.dp

/**
 * Shows [text] in a small box above [content] while [visible]; nothing when [text] is null.
 * [modifier] sizes the anchor, and [content] stretches to its minimum size.
 */
@Composable
internal fun JagTooltip(
    text: String?,
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier, propagateMinConstraints = true) {
        content()
        if (text != null && visible) {
            val colors = JagFxTheme.colors
            val dimens = JagFxTheme.dimens
            Popup(popupPositionProvider = AboveAnchor) {
                JagText(
                    text = text,
                    modifier =
                        Modifier
                            .background(colors.header, RoundedCornerShape(dimens.radius))
                            .border(dimens.border, colors.border, RoundedCornerShape(dimens.radius))
                            .padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm),
                    color = colors.text,
                )
            }
        }
    }
}

/** Places a popup centered above its anchor, or below when there is no room above. */
private object AboveAnchor : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x =
            (anchorBounds.center.x - popupContentSize.width / 2)
                .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = anchorBounds.top - popupContentSize.height - TOOLTIP_GAP_PX
        val y = if (above >= 0) above else anchorBounds.bottom + TOOLTIP_GAP_PX
        return IntOffset(x, y)
    }
}
