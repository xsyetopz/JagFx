package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.Stable
import io.github.xsyetopz.jagfx.ui.editor.EditorIntent
import io.github.xsyetopz.jagfx.ui.editor.HistoryIntent

/**
 * Callbacks of editor screen.
 * [dispatch] sends intent to ViewModel; [updateView] changes [ScreenViewState].
 */
@Stable
internal class EditorActions(
    val dispatch: (EditorIntent) -> Unit,
    val updateView: ((ScreenViewState) -> ScreenViewState) -> Unit,
) {
    /**
     * Set by Alt and arrow keys: cell that becomes selected next takes keyboard
     * focus, so focus follows selection. Not state; cell clears it.
     */
    var focusSelectedCell: Boolean = false

    fun beginGesture() = dispatch(HistoryIntent.BeginGesture)

    fun endGesture() = dispatch(HistoryIntent.EndGesture)
}
