package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.model.Patch
import io.github.xsyetopz.jagfx.ui.i18n.Strings
import io.github.xsyetopz.jagfx.ui.i18n.UiLocale
import io.github.xsyetopz.jagfx.ui.screen.emptyPromptKey
import io.github.xsyetopz.jagfx.ui.screen.pairFirstOf
import io.github.xsyetopz.jagfx.ui.screen.statusText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentStatusTest {
    @Test
    fun newPatchHasToneOneOn() {
        val state = EditorState().after(DocumentIntent.NewPatch)
        assertEquals(DefaultVoice, state.voice)
        assertEquals(0, state.selectedVoice)
        assertFalse(state.isDirty)
        assertFalse(state.canUndo)
        assertEquals(StatusHint.NewPatch, state.statusHint)
        assertEquals(Patch.Empty.withVoice(0, DefaultVoice), state.patch)
    }

    @Test
    fun newPatchDropsOpenFileAndHistory() {
        val edited =
            EditorState(fileName = "bell", filePath = "/sfx/bell.synth")
                .after(SlotIntent.SetVoiceEnabled(3, true))
        val state = edited.after(DocumentIntent.NewPatch)
        assertEquals(EditorState.DEFAULT_FILE_NAME, state.fileName)
        assertNull(state.filePath)
        assertNull(state.patch.voices[3])
        assertFalse(state.canUndo)
    }

    @Test
    fun editUndoAndRedoEachReportThemselves() {
        val edited = EditorState().after(SlotIntent.SetVoiceEnabled(0, true))
        assertEquals(StatusHint.Edited, edited.statusHint)
        val undone = edited.after(HistoryIntent.Undo)
        assertEquals(StatusHint.Undone, undone.statusHint)
        assertEquals(StatusHint.Redone, undone.after(HistoryIntent.Redo).statusHint)
    }

    @Test
    fun undoWithNothingToUndoKeepsHint() {
        val state = EditorState(statusHint = StatusHint.NewPatch).after(HistoryIntent.Undo)
        assertEquals(StatusHint.NewPatch, state.statusHint)
    }

    @Test
    fun everyActionHintHasTextInEveryLanguage() {
        val hints =
            listOf(StatusHint.NewPatch, StatusHint.Edited, StatusHint.Undone, StatusHint.Redone)
        for (locale in UiLocale.entries) {
            val strings = Strings(locale)
            for (hint in hints) assertTrue(statusText(hint, strings).isNotBlank(), "$locale $hint")
        }
    }

    @Test
    fun exactlyModuleStartSlotsHaveEmptyPrompt() {
        for (slot in SignalChainSlot.entries) {
            val prompt = emptyPromptKey(slot)
            val startsModule = pairFirstOf(slot) == slot
            assertEquals(startsModule, prompt != null, slot.name)
        }
        val keys = SignalChainSlot.entries.mapNotNull(::emptyPromptKey)
        assertEquals(keys.toSet().size, keys.size)
        assertNotNull(keys.firstOrNull())
    }

    @Test
    fun fileKindsDescribeToneAndFilterAsJagFxFiles() {
        assertTrue(PatchFileKind.Tone.description.startsWith("JagFx"))
        assertTrue(PatchFileKind.Filter.description.startsWith("JagFx"))
    }
}
