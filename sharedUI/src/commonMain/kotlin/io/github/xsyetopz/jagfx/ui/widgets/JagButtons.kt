package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
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
 * A flat button at least 24 dp square.
 * An [active] button has accent text, an accent border, and a bar along its bottom edge, so color
 * is not the only sign.
 * A focused button shows a 2 dp accent ring. [tooltip] shows while the pointer rests on it or the
 * button has keyboard focus, and Escape closes it.
 * It acts on release and not on press, and moving off before release cancels it.
 * [description] is the accessible name when the content is a symbol; [role] is the accessible
 * role, and a [Role.Switch], [Role.Checkbox], or [Role.RadioButton] reports [active] as its state.
 */
@Composable
internal fun JagButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    tooltip: String? = null,
    description: String? = null,
    role: Role = Role.Button,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val tooltipState = rememberTooltipState(hovered || focused)
    val shape = RoundedCornerShape(dimens.radius)
    val background =
        when {
            active -> colors.buttonActive
            hovered && enabled -> colors.buttonHover
            else -> colors.button
        }
    JagTooltip(tooltip, tooltipState.visible, modifier) {
        Row(
            modifier =
                Modifier
                    .dismissTooltipOnEscape(tooltipState)
                    .defaultMinSize(
                        minWidth = dimens.buttonMinWidth,
                        minHeight = dimens.controlHeight,
                    ).alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .background(background, shape)
                    .border(
                        dimens.border,
                        if (active) colors.accent else colors.controlBorder,
                        shape,
                    ).activeMark(active, colors.accent, dimens.focusRing)
                    .focusRing(focused, colors.accent, dimens.focusRing, dimens.radius)
                    .hoverable(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = role,
                        onClick = onClick,
                    ).semantics(mergeDescendants = true) {
                        description?.let { contentDescription = it }
                        stateOf(role, active)
                    }.padding(horizontal = dimens.spaceMd),
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
    role: Role = Role.Button,
) {
    JagButton(onClick, modifier, enabled, active, tooltip, role = role) {
        val label = JagFxTheme.typography.label
        JagText(
            text,
            style = if (active) label.copy(fontWeight = FontWeight.ExtraBold) else label,
        )
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
    JagButton(onClick, modifier, enabled, active, tooltip, tooltip) { PainterIcon(icon) }
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
                description = tooltip(option),
                role = Role.RadioButton,
            ) { item(option) }
        }
    }
}

/**
 * A check box with a 24 dp hit area around a 12 dp box: an input-colored box with an accent border
 * and mark when [checked], and a 2 dp accent ring while focused.
 */
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
    val focused by interaction.collectIsFocusedAsState()
    val tooltipState = rememberTooltipState(hovered || focused)
    val shape = RoundedCornerShape(dimens.radius)
    JagTooltip(tooltip, tooltipState.visible, modifier) {
        Box(
            modifier =
                Modifier
                    .dismissTooltipOnEscape(tooltipState)
                    .size(dimens.minTarget)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .hoverable(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        role = Role.Checkbox,
                    ) { onCheckedChange(!checked) }
                    .semantics(mergeDescendants = true) {
                        tooltip?.let { contentDescription = it }
                        toggleableState = ToggleableState(checked)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(dimens.iconSize)
                    .background(colors.input, shape)
                    .border(
                        dimens.border,
                        if (checked) colors.accent else colors.controlBorder,
                        shape,
                    ).focusRing(focused, colors.accent, dimens.focusRing, dimens.radius),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) Box(Modifier.size(CheckMarkSize).background(colors.accent))
            }
        }
    }
}

private val CheckMarkSize = 6.dp

/** The state a control with [role] reports: on or off for toggles, chosen for radio buttons. */
private fun androidx.compose.ui.semantics.SemanticsPropertyReceiver.stateOf(
    role: Role,
    active: Boolean,
) {
    when (role) {
        Role.Switch, Role.Checkbox -> toggleableState = ToggleableState(active)
        Role.RadioButton -> selected = active
    }
}

/**
 * Whether a tooltip shows: while [shown] (hover or keyboard focus) until Escape closes it. It
 * shows again after the pointer or focus leaves and returns.
 */
internal class TooltipState(
    shown: Boolean,
    private val dismissed: androidx.compose.runtime.MutableState<Boolean>,
) {
    val visible: Boolean = shown && !dismissed.value

    fun dismiss() {
        dismissed.value = true
    }
}

@Composable
internal fun rememberTooltipState(shown: Boolean): TooltipState {
    val dismissed = remember(shown) { mutableStateOf(false) }
    return TooltipState(shown, dismissed)
}

/** Closes a showing tooltip on Escape and consumes the key; other Escapes pass on. */
internal fun Modifier.dismissTooltipOnEscape(state: TooltipState): Modifier =
    onKeyEvent {
        val close = state.visible && it.key == Key.Escape && it.type == KeyEventType.KeyDown
        if (close) state.dismiss()
        close
    }

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
                            .border(
                                dimens.border,
                                colors.controlBorder,
                                RoundedCornerShape(dimens.radius),
                            ).padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm),
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
