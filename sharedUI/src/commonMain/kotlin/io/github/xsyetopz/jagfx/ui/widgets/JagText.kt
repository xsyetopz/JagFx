package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme

/**
 * [text] in [style], in the content color unless [color] is given: one line cut with an ellipsis,
 * or wrapped over as many lines as it needs when [wrap] is set.
 */
@Composable
internal fun JagText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    textAlign: TextAlign = TextAlign.Unspecified,
    wrap: Boolean = false,
) {
    val resolved = if (color != Color.Unspecified) color else LocalContentColor.current
    Text(
        text = text,
        modifier = modifier,
        color = resolved,
        textAlign = textAlign,
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = if (wrap) Int.MAX_VALUE else 1,
        softWrap = wrap,
    )
}

/** A dim bold uppercase label, the Scala JagFX caption above a field or a section. */
@Composable
internal fun JagLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = JagFxTheme.colors.textDim,
) {
    JagText(text.uppercase(), modifier, color, MaterialTheme.typography.labelSmall)
}
