package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.input.key.Key
import io.github.xsyetopz.jagfx.ui.canvas.TimeView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorKeysTest {
    @Test
    fun altArrowsMoveOneCellOrOneRow() {
        assertEquals(1, cellKeyOffset(Key.DirectionRight))
        assertEquals(-1, cellKeyOffset(Key.DirectionLeft))
        assertEquals(3, cellKeyOffset(Key.DirectionDown))
        assertEquals(-3, cellKeyOffset(Key.DirectionUp))
        assertNull(cellKeyOffset(Key.PageDown))
    }

    @Test
    fun shiftPageKeysPanAndCommandPlusMinusZoom() {
        assertEquals(
            ViewKeyCommand.PanRight,
            viewKeyCommand(Key.PageDown, shift = true, command = false),
        )
        assertEquals(
            ViewKeyCommand.PanLeft,
            viewKeyCommand(Key.PageUp, shift = true, command = false),
        )
        assertEquals(
            ViewKeyCommand.ZoomIn,
            viewKeyCommand(Key.Equals, shift = false, command = true),
        )
        assertEquals(
            ViewKeyCommand.ZoomIn,
            viewKeyCommand(Key.NumPadAdd, shift = false, command = true),
        )
        assertEquals(
            ViewKeyCommand.ZoomOut,
            viewKeyCommand(Key.Minus, shift = false, command = true),
        )
    }

    @Test
    fun otherModifierMixesStayFree() {
        assertNull(viewKeyCommand(Key.PageDown, shift = false, command = false))
        assertNull(viewKeyCommand(Key.PageDown, shift = true, command = true))
        assertNull(viewKeyCommand(Key.Plus, shift = false, command = false))
        assertNull(viewKeyCommand(Key.Minus, shift = true, command = true))
    }

    @Test
    fun panMovesOneScreenAndStaysInRange() {
        val view = TimeView(zoom = 4, scroll = 0.25f)
        assertEquals(0.5f, view.pannedByScreen(1).scroll)
        assertEquals(0f, view.pannedByScreen(-1).scroll)
        assertEquals(
            0.75f,
            view
                .pannedByScreen(1)
                .pannedByScreen(1)
                .pannedByScreen(1)
                .scroll,
        )
        assertEquals(TimeView.Fit, TimeView.Fit.pannedByScreen(1))
    }

    @Test
    fun escapeStopsOnlyWhilePlaying() {
        assertTrue(stopsPlayback(Key.Escape, playing = true))
        assertFalse(stopsPlayback(Key.Escape, playing = false))
        assertFalse(stopsPlayback(Key.Spacebar, playing = true))
    }

    @Test
    fun spaceTogglesPlaybackOnlyInBrowserAndUnmodified() {
        assertTrue(togglesPlayback(Key.Spacebar, modified = false, nativeMenu = false))
        assertFalse(togglesPlayback(Key.Spacebar, modified = false, nativeMenu = true))
        assertFalse(togglesPlayback(Key.Spacebar, modified = true, nativeMenu = false))
        assertFalse(togglesPlayback(Key.Enter, modified = false, nativeMenu = false))
    }
}
