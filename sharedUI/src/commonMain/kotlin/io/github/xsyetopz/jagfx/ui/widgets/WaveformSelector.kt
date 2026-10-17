package io.github.xsyetopz.jagfx.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.xsyetopz.jagfx.core.model.Waveform

/**
 * Joined icon buttons, one per [Waveform.selectable] entry, each with its [name] as the tooltip;
 * the [selected] one is active.
 * An undefined waveform ([Waveform.isUndefined]) has no button, so none is active for it.
 */
@Composable
internal fun WaveformSelector(
    selected: Waveform,
    onSelect: (Waveform) -> Unit,
    name: (Waveform) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    JagToggleGroup(
        options = Waveform.selectable,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        enabled = enabled,
        tooltip = name,
    ) { PainterIcon(Icons.waveform(it)) }
}
