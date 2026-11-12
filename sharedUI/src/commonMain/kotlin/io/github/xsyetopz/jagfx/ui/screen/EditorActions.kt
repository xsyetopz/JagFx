package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Stable
import io.github.xsyetopz.jagfx.ui.editor.EditorIntent
import io.github.xsyetopz.jagfx.ui.editor.HistoryIntent

/**
 * Callbacks of the editor screen.
 * [dispatch] sends an intent to the ViewModel; [updateView] changes the [ScreenViewState].
 */
@Stable
internal class EditorActions(
    val dispatch: (EditorIntent) -> Unit,
    val updateView: ((ScreenViewState) -> ScreenViewState) -> Unit,
) {
    /**
     * Set by the Alt and arrow keys: the cell that becomes selected next takes the keyboard
     * focus, so the focus follows the selection. Not state; the cell clears it.
     */
    var focusSelectedCell: Boolean = false

    fun beginGesture() = dispatch(HistoryIntent.BeginGesture)

    fun endGesture() = dispatch(HistoryIntent.EndGesture)
}
