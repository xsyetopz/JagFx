package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.ui.editor.StatusHint
import io.github.xsyetopz.jagfx.ui.i18n.LocalStrings
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagText

/** One-line message of the last file or edit outcome. */
@Composable
internal fun StatusBar(
    hint: StatusHint?,
    modifier: Modifier = Modifier,
) {
    val dimens = JagFxTheme.dimens
    Box(
        modifier
            .background(JagFxTheme.colors.footer)
            .padding(horizontal = dimens.spaceXl, vertical = dimens.spaceSm),
    ) {
        JagText(statusText(hint, LocalStrings.current), color = JagFxTheme.colors.textDim)
    }
}
