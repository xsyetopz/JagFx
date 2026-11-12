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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.semantics.role
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
 * A stock Material button at least 24 dp tall. An [active] button is tonal-filled and bold, and
 * the rest are outlined, so color is not the only sign. [tooltip] shows in a stock tooltip.
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
    val dimens = JagFxTheme.dimens
    val padding = PaddingValues(horizontal = dimens.spaceLg)
    val semantic =
        Modifier
            .height(dimens.controlHeight)
            .semantics(mergeDescendants = true) {
                description?.let { contentDescription = it }
                this.role = role
                stateOf(role, active)
            }
    Tip(tooltip, modifier) {
        if (active) {
            FilledTonalButton(
                onClick,
                semantic,
                enabled,
                contentPadding = padding,
                content = content,
            )
        } else {
            OutlinedButton(onClick, semantic, enabled, contentPadding = padding, content = content)
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

/** A stock Material icon button that draws [icon]; [tooltip] names it. */
@Composable
internal fun JagIconButton(
    icon: IconPainter,
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val dimens = JagFxTheme.dimens
    val size = Modifier.size(dimens.controlHeight).semantics { contentDescription = tooltip }
    Tip(tooltip, modifier) {
        if (active) {
            FilledTonalIconButton(onClick, size, enabled) { PainterIcon(icon) }
        } else {
            OutlinedIconButton(onClick, size, enabled) { PainterIcon(icon) }
        }
    }
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

/** A stock Material check box; [tooltip] shows in a stock tooltip and is its accessible name. */
@Composable
internal fun JagCheck(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tooltip: String? = null,
) {
    Tip(tooltip, modifier) {
        Checkbox(
            checked,
            onCheckedChange,
            Modifier.semantics { tooltip?.let { contentDescription = it } },
            enabled,
        )
    }
}

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
