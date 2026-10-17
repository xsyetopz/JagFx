package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.theme.LocalContentColor

/** One line of [text] in [style], in the content color unless [color] is given. */
@Composable
internal fun JagText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = JagFxTheme.typography.body,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    val resolved = if (color != Color.Unspecified) color else LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = resolved, textAlign = textAlign),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        softWrap = false,
    )
}

/** A dim bold uppercase label, the Scala JagFX caption above a field or a section. */
@Composable
internal fun JagLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = JagFxTheme.colors.textDim,
) {
    JagText(text.uppercase(), modifier, color, JagFxTheme.typography.label)
}
