package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.xsyetopz.jagfx.ui.theme.DISABLED_ALPHA
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.LocalContentColor

/**
 * A popup list at [offset] from the top left of its parent; a click outside or Escape calls
 * [onDismiss].
 * Fill it with [JagMenuItem]s.
 */
@Composable
internal fun JagMenu(
    onDismiss: () -> Unit,
    offset: IntOffset = IntOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val shape = RoundedCornerShape(dimens.radius)
    Popup(
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier =
                Modifier
                    .width(IntrinsicSize.Max)
                    .background(colors.panel, shape)
                    .border(dimens.border, colors.border, shape)
                    .padding(dimens.spaceXs),
            content = content,
        )
    }
}

/** One row of a [JagMenu]; an [active] row shows in the accent color. */
@Composable
internal fun JagMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    icon: IconPainter? = null,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.controlHeight + dimens.spaceMd)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .background(if (hovered && enabled) colors.buttonHover else colors.panel)
                .hoverable(interaction)
                .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
                .padding(horizontal = dimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (active) colors.accent else colors.text,
        ) {
            icon?.let { PainterIcon(it) }
            JagText(text)
        }
    }
}
