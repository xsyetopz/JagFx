package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.InvalidPatchException
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.format.SynthFormatException

/** Imports and exports `.tone` and `.filter` files for the selected voice, reporting with [dispatch]. */
internal class ToneFilterTransfer(
    private val files: PatchFiles,
    private val state: () -> EditorState,
    private val dispatch: (EditorIntent) -> Unit,
) {
    /** Reads a `.tone` file into the selected slot. */
    suspend fun importTone() {
        val slot = state().selectedVoice
        val decoded = pickAndDecode(PatchFileKind.Tone) { SynthCodec.decodeTone(it).voice }
        decoded?.let { dispatch(SlotIntent.SetVoice(slot, it.value)) }
    }

    /** Reads a `.filter` file into the filter of the selected voice. */
    suspend fun importFilter() {
        val decoded = pickAndDecode(PatchFileKind.Filter) { SynthCodec.decodeFilter(it).filter }
        decoded?.let { dispatch(FilterIntent.SetFilter(it.value)) }
    }

    suspend fun exportTone() {
        val state = state()
        val voice = state.voice ?: return
        val name = "${state.fileName}-${state.selectedVoice + 1}"
        export(name, PatchFileKind.Tone) { SynthCodec.encodeTone(voice) }
    }

    suspend fun exportFilter() {
        val state = state()
        val filter = state.voice?.filter ?: return
        val name = "${state.fileName}-${state.selectedVoice + 1}"
        export(name, PatchFileKind.Filter) { SynthCodec.encodeFilter(filter) }
    }

    /**
     * Asks for a file of [kind] and decodes it, or reports the failure and returns `null`. A
     * cancelled picker also returns `null`. The result wraps the value because a decoded filter
     * can be `null`.
     */
    private suspend fun <T> pickAndDecode(
        kind: PatchFileKind,
        decode: (ByteArray) -> T,
    ): Decoded<T>? {
        var name = "${state().fileName}.${kind.extension}"
        return try {
            val file = files.pickAndRead(kind) ?: return null
            name = file.name
            Decoded(decode(file.bytes))
        } catch (e: PatchFileException) {
            dispatch(ResultIntent.LoadFailed(name, e.message.orEmpty()))
            null
        } catch (e: SynthFormatException) {
            dispatch(ResultIntent.LoadFailed(name, e.message.orEmpty()))
            null
        }
    }

    private suspend fun export(
        baseName: String,
        kind: PatchFileKind,
        encode: () -> ByteArray,
    ) {
        val shownName = "$baseName.${kind.extension}"
        val result =
            try {
                files
                    .pickAndWrite(
                        shownName,
                        encode(),
                        kind,
                    )?.let { ResultIntent.Exported(it.name) }
            } catch (e: PatchFileException) {
                ResultIntent.SaveFailed(shownName, e.message.orEmpty())
            } catch (e: InvalidPatchException) {
                ResultIntent.SaveFailed(shownName, e.message.orEmpty())
            }
        result?.let(dispatch)
    }

    private class Decoded<T>(
        val value: T,
    )
}
