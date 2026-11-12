package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import io.github.xsyetopz.jagfx.ui.widgets.JagDecimalField
import io.github.xsyetopz.jagfx.ui.widgets.JagField
import io.github.xsyetopz.jagfx.ui.widgets.JagLabel
import io.github.xsyetopz.jagfx.ui.widgets.JagText
import io.github.xsyetopz.jagfx.ui.widgets.formatDecimal

private val LabelWidth = 64.dp
private val AnyInt = Int.MIN_VALUE..Int.MAX_VALUE

/** Scrolling side strip of editor, as wide as [JagFxTheme] says. */
@Composable
internal fun StripColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = JagFxTheme.dimens
    Column(
        modifier
            .width(dimens.stripWidth)
            .background(JagFxTheme.colors.panel)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        content = content,
    )
}

/** [label] left of control; [field] gets modifier that fills row and names control. */
@Composable
internal fun StripRow(
    label: String,
    description: String,
    field: @Composable (Modifier) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JagFxTheme.dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JagLabel(label, Modifier.width(LabelWidth))
        field(Modifier.weight(1f).semantics { contentDescription = description })
    }
}

/** Integer field with [label] and [unit]; [description] is its accessible name. */
@Composable
internal fun StripField(
    label: String,
    description: String,
    value: Int,
    onCommit: (Int) -> Unit,
    unit: String,
    enabled: Boolean = true,
    range: IntRange = AnyInt,
) {
    StripRow(label, description) { JagField(value, onCommit, range, it, enabled, unit) }
}

/** Field in ms with two decimals, for gate gaps that last few ms. */
@Composable
internal fun StripMsField(
    label: String,
    description: String,
    value: Double,
    onCommit: (Double) -> Unit,
    range: ClosedFloatingPointRange<Double>,
    enabled: Boolean,
) {
    StripRow(label, description) {
        JagDecimalField(value, onCommit, range, GAP_DECIMALS, it, enabled, "ms")
    }
}

/** Read-only [value] with [label] and [unit], such as average of Min and Max. */
@Composable
internal fun StripReadout(
    label: String,
    description: String,
    value: Double,
    unit: String,
    decimals: Int,
) {
    val colors = JagFxTheme.colors
    val dimens = JagFxTheme.dimens
    StripRow(label, "$description ${formatDecimal(value, decimals)} $unit") {
        Row(
            it
                .defaultMinSize(minHeight = dimens.controlHeight)
                .border(dimens.border, colors.border)
                .padding(horizontal = dimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JagText(formatDecimal(value, decimals), color = colors.textDim)
            JagText(unit, color = colors.textDim, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private const val GAP_DECIMALS = 2
