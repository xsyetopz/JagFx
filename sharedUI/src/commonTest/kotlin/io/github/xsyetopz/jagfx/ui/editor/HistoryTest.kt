package io.github.xsyetopz.jagfx.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryTest {
    private val enabled = EditorState().after(SlotIntent.SetVoiceEnabled(0, true))

    @Test
    fun undoAndRedoRestorePatches() {
        val edited = enabled.after(VoiceIntent.SetDuration(500), VoiceIntent.SetDuration(700))

        val undone = edited.after(HistoryIntent.Undo)
        assertEquals(500, undone.voice!!.duration.value)
        val twice = undone.after(HistoryIntent.Undo, HistoryIntent.Undo)
        assertEquals(EditorState().patch, twice.patch)
        assertFalse(twice.canUndo)

        val redone = twice.after(HistoryIntent.Redo, HistoryIntent.Redo, HistoryIntent.Redo)
        assertEquals(edited.patch, redone.patch)
        assertFalse(redone.canRedo)
    }

    @Test
    fun newEditClearsRedo() {
        val state =
            enabled.after(
                VoiceIntent.SetDuration(500),
                HistoryIntent.Undo,
                VoiceIntent.SetOffset(3),
            )
        assertFalse(state.canRedo)
        assertEquals(state.patch, state.after(HistoryIntent.Redo).patch)
    }

    @Test
    fun gestureCoalescesEditsIntoOneUndoStep() {
        val state =
            enabled.after(
                HistoryIntent.BeginGesture,
                VoiceIntent.SetDuration(100),
                HistoryIntent.BeginGesture,
                VoiceIntent.SetDuration(200),
                HistoryIntent.EndGesture,
                VoiceIntent.SetDuration(300),
                HistoryIntent.EndGesture,
            )
        assertEquals(300, state.voice!!.duration.value)
        assertEquals(enabled.patch, state.after(HistoryIntent.Undo).patch)
        assertEquals(state.patch, state.after(HistoryIntent.Undo, HistoryIntent.Redo).patch)
    }

    @Test
    fun undoDoesNothingDuringGesture() {
        val state =
            enabled.after(
                VoiceIntent.SetOffset(9),
                HistoryIntent.BeginGesture,
                VoiceIntent.SetDuration(1),
            )
        assertFalse(state.canUndo)
        assertEquals(state.patch, state.after(HistoryIntent.Undo).patch)
    }

    @Test
    fun gestureWithoutNetChangeRecordsNothing() {
        val before = enabled.history
        val state =
            enabled.after(
                HistoryIntent.BeginGesture,
                VoiceIntent.SetDuration(1),
                VoiceIntent.SetDuration(DefaultVoice.duration.value),
                HistoryIntent.EndGesture,
                HistoryIntent.EndGesture,
            )
        assertEquals(before, state.history)
    }

    @Test
    fun viewChangesNeverEnterHistory() {
        val state =
            enabled.after(
                ViewIntent.SetLooping(true),
                SlotIntent.SelectVoice(4),
                SlotIntent.SelectTarget(SignalChainSlot.Volume),
            )
        assertEquals(enabled.history, state.history)
        assertEquals(enabled.patch, state.patch)
        assertEquals(enabled.isDirty, state.isDirty)
    }

    @Test
    fun historyKeepsAtMostCapacityPatches() {
        val state =
            (1..History.CAPACITY + 50).fold(
                enabled,
            ) { s, ms -> reduce(s, VoiceIntent.SetDuration(ms)) }
        assertEquals(History.CAPACITY, state.history.past.size)
        assertTrue(state.history.past.all { it != state.patch })
    }
}
