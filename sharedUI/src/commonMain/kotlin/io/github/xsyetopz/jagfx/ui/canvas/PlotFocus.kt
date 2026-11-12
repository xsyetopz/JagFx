package io.github.xsyetopz.jagfx.ui.canvas

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.focusRing

/**
 * Makes a plot reachable with Tab and shows the focus ring while it has focus. [onKey] sees the key
 * events of the focused plot; a plot with no keys of its own leaves it as it is.
 */
@Composable
internal fun Modifier.focusablePlot(
    focusRequester: FocusRequester = remember { FocusRequester() },
    onKey: (KeyEvent) -> Boolean = { false },
): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onKeyEvent(onKey)
        .focusRequester(focusRequester)
        .onFocusChanged { focused = it.isFocused }
        .focusable()
        .focusRing(focused, JagFxTheme.colors.accent, JagFxTheme.dimens.focusRing, 0.dp)
}
