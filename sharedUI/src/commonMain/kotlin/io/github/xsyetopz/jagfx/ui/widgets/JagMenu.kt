package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/**
 * Stock Material dropdown menu at [offset] from top left of its parent; click outside or
 * Escape calls [onDismiss].
 * Fill it with [JagMenuItem]s.
 */
@Composable
internal fun JagMenu(
    onDismiss: () -> Unit,
    offset: IntOffset = IntOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    val at = with(LocalDensity.current) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
    DropdownMenu(expanded = true, onDismissRequest = onDismiss, offset = at, content = content)
}

/** One row of [JagMenu]; [active] row is bold and reports itself selected. */
@Composable
internal fun JagMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    icon: IconPainter? = null,
) {
    val dimens = JagFxTheme.dimens
    val style = MaterialTheme.typography.bodySmall
    DropdownMenuItem(
        text = {
            JagText(text, style = if (active) style.copy(fontWeight = FontWeight.Bold) else style)
        },
        onClick = onClick,
        modifier =
            Modifier
                .height(dimens.controlHeight + dimens.spaceMd)
                .semantics { selected = active },
        leadingIcon = icon?.let { { PainterIcon(it) } },
        enabled = enabled,
    )
}
