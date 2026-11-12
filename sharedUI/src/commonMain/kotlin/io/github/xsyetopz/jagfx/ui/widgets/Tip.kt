package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay

/** How long pointer rests on control before its tooltip shows. */
internal const val TOOLTIP_HOVER_DELAY_MS = 500L

/** How long tooltip stays after pointer leaves, so pointer can cross onto it. */
internal const val TOOLTIP_GRACE_MS = 300L

/**
 * Shows [text] in stock Material tooltip above [content] while pointer rests on it, while
 * pointer is over tooltip itself, and while [content] has keyboard focus (WCAG 1.4.13:
 * tooltip is hoverable, persistent, and dismissible). Escape closes it until pointer and focus
 * have left and come back. Null [text] shows no tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Tip(
    text: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (text == null) {
        Box(modifier, propagateMinConstraints = true) { content() }
        return
    }
    val state = rememberTooltipState(isPersistent = true)
    val anchorSource = remember { MutableInteractionSource() }
    val tooltipSource = remember { MutableInteractionSource() }
    val hovered by anchorSource.collectIsHoveredAsState()
    val overTooltip by tooltipSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    var escaped by remember { mutableStateOf(false) }
    val wanted = hovered || overTooltip || focused
    LaunchedEffect(wanted, escaped, focused) {
        if (wanted && !escaped) {
            if (!focused && !overTooltip) delay(TOOLTIP_HOVER_DELAY_MS)
            state.show()
        } else {
            delay(TOOLTIP_GRACE_MS)
            state.dismiss()
            if (!wanted) escaped = false
        }
    }
    // TooltipBox applies its modifier below its own root, so a weight in [modifier] would never
    // reach the parent Row; this Box carries it and hands the size it gets down to the anchor.
    Box(modifier, propagateMinConstraints = true) {
        Anchor(
            text,
            state,
            anchorSource,
            tooltipSource,
            { escaped = true },
            { focused = it },
            content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Anchor(
    text: String,
    state: TooltipState,
    anchorSource: MutableInteractionSource,
    tooltipSource: MutableInteractionSource,
    onEscape: () -> Unit,
    onFocus: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip(Modifier.hoverable(tooltipSource)) { Text(text) } },
        state = state,
        modifier =
            Modifier
                .onPreviewKeyEvent {
                    val close =
                        state.isVisible && it.key == Key.Escape && it.type == KeyEventType.KeyDown
                    if (close) {
                        onEscape()
                        state.dismiss()
                    }
                    close
                }.onFocusChanged { onFocus(it.hasFocus) }
                .hoverable(anchorSource),
        enableUserInput = false,
        content = content,
    )
}
