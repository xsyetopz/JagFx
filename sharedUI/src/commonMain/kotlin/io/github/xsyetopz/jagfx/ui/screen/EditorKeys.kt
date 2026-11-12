package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import io.github.xsyetopz.jagfx.ui.canvas.TimeView
import io.github.xsyetopz.jagfx.ui.canvas.stepZoom
import io.github.xsyetopz.jagfx.ui.editor.SignalChainSlot
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent

/** Cells in one rack row: Alt and Up or Down moves this many cells along the chain. */
private const val CELLS_PER_ROW = 3

/**
 * The offset along [SignalChainSlot.envelopeChain] that Alt with [key] moves the selected cell
 * by: Left and Right by one cell, Up and Down by one rack row; `null` for any other key.
 */
internal fun cellKeyOffset(key: Key): Int? =
    when (key) {
        Key.DirectionLeft -> -1
        Key.DirectionRight -> 1
        Key.DirectionUp -> -CELLS_PER_ROW
        Key.DirectionDown -> CELLS_PER_ROW
        else -> null
    }

/** The keyboard alternatives to wheel zoom and pan on a plot. */
internal enum class ViewKeyCommand { ZoomIn, ZoomOut, PanLeft, PanRight }

private val ZoomInKeys = setOf(Key.Plus, Key.Equals, Key.NumPadAdd)
private val ZoomOutKeys = setOf(Key.Minus, Key.NumPadSubtract)

/**
 * The zoom or pan that [key] asks for. Ctrl or Cmd ([command]) with `+` or `-` zooms one step;
 * Shift with Page Up or Page Down pans one screen left or right. `null` for any other key or
 * modifier mix, so the key stays free for the cell.
 */
internal fun viewKeyCommand(
    key: Key,
    shift: Boolean,
    command: Boolean,
): ViewKeyCommand? =
    when {
        command && !shift && key in ZoomInKeys -> ViewKeyCommand.ZoomIn
        command && !shift && key in ZoomOutKeys -> ViewKeyCommand.ZoomOut
        shift && !command && key == Key.PageUp -> ViewKeyCommand.PanLeft
        shift && !command && key == Key.PageDown -> ViewKeyCommand.PanRight
        else -> null
    }

/** `true` when Escape asks to stop playback: only while [playing], so Escape stays free otherwise. */
internal fun stopsPlayback(
    key: Key,
    playing: Boolean,
): Boolean = playing && key == Key.Escape

/** This view moved one screen ([TimeView.span]) left for a negative [direction], right for a positive one. */
internal fun TimeView.pannedByScreen(direction: Int): TimeView =
    copy(scroll = (scroll + direction * span).coerceIn(0f, 1f - span))

/** The [ViewKeyCommand] of [event] on a key down, or `null`. */
internal fun viewKeyCommandOf(event: KeyEvent): ViewKeyCommand? =
    if (event.type == KeyEventType.KeyDown) {
        viewKeyCommand(event.key, event.isShiftPressed, event.isCtrlPressed || event.isMetaPressed)
    } else {
        null
    }

/**
 * Selects the neighbor of the cell when [event] is Alt with an arrow, and asks the next selected
 * cell to take the keyboard focus. `true` when handled.
 */
internal fun EditorActions.handleCellNavigationKey(event: KeyEvent): Boolean {
    val offset =
        cellKeyOffset(event.key)?.takeIf {
            event.type == KeyEventType.KeyDown &&
                event.isAltPressed &&
                !event.isCtrlPressed &&
                !event.isMetaPressed &&
                !event.isShiftPressed
        } ?: return false
    focusSelectedCell = true
    dispatch(SlotIntent.SelectTargetByOffset(offset))
    return true
}

/** Applies [command] to the plot of [slot]; only time-domain plots pan, as the others scroll in pixels. */
internal fun EditorActions.applyViewKey(
    command: ViewKeyCommand,
    slot: SignalChainSlot,
    view: ScreenViewState,
) {
    val zoom = if (slot.isTimeDomain) view.timeViewOf(slot).zoom else view.canvas(slot).zoom
    when (command) {
        ViewKeyCommand.ZoomIn -> {
            zoomTo(slot, stepZoom(zoom, 1f))
        }

        ViewKeyCommand.ZoomOut -> {
            zoomTo(slot, stepZoom(zoom, -1f))
        }

        ViewKeyCommand.PanLeft -> {
            if (slot.isTimeDomain) {
                updateTimeView(
                    slot,
                ) { it.pannedByScreen(-1) }
            }
        }

        ViewKeyCommand.PanRight -> {
            if (slot.isTimeDomain) {
                updateTimeView(
                    slot,
                ) { it.pannedByScreen(1) }
            }
        }
    }
}
