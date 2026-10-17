package io.github.xsyetopz.jagfx.ui.editor

import io.github.xsyetopz.jagfx.core.format.InvalidPatchException
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.format.SynthFormatException
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisCodec
import io.github.xsyetopz.jagfx.core.vorbis.JagVorbisFormatException

/** Opens and saves `.synth` and `.jagvorbis` documents, reporting with [dispatch]. */
internal class PatchFileTasks(
    private val files: PatchFiles,
    private val state: () -> EditorState,
    private val dispatch: (EditorIntent) -> Unit,
) {
    suspend fun open() {
        val file =
            try {
                files.pickAndRead(PatchFileKind.Synth)
            } catch (e: PatchFileException) {
                dispatch(ResultIntent.LoadFailed(state().fileName, e.message.orEmpty()))
                return
            } ?: return
        load(fileNameOf(file.name), file.path, file.bytes)
    }

    private fun load(
        name: String,
        path: String?,
        bytes: ByteArray,
    ) {
        val intent =
            try {
                ResultIntent.Loaded(name, path, SynthCodec.decode(bytes).patch)
            } catch (e: SynthFormatException) {
                ResultIntent.LoadFailed(name, e.message.orEmpty())
            }
        dispatch(intent)
    }

    /** Writes the patch to the current path, or asks for one when [pick] or there is none. */
    suspend fun save(pick: Boolean) {
        val current = state()
        val patch = current.patch
        val currentPath = current.filePath.takeUnless { pick }
        val shownName = "${current.fileName}.synth"
        val shownPath = currentPath ?: shownName
        val result =
            try {
                val bytes = SynthCodec.encode(patch)
                val saved = writeFile(currentPath, shownName, bytes, PatchFileKind.Synth)
                saved?.let { ResultIntent.Saved(it.name, it.path, patch) }
            } catch (e: PatchFileException) {
                ResultIntent.SaveFailed(shownPath, e.message.orEmpty())
            } catch (e: InvalidPatchException) {
                ResultIntent.SaveFailed(shownPath, e.message.orEmpty())
            }
        result?.let(dispatch)
    }

    /** Writes the sample to the current path, or asks for one when [pick] or there is none. */
    suspend fun saveSample(pick: Boolean) {
        val document = state().sample ?: return
        val currentPath = document.path.takeUnless { pick }
        val shownName = "${document.name}.${PatchFileKind.Sample.extension}"
        val shownPath = currentPath ?: shownName
        val result =
            try {
                val bytes = JagVorbisCodec.encode(document.sample)
                val saved = writeFile(currentPath, shownName, bytes, PatchFileKind.Sample)
                saved?.let { ResultIntent.SampleSaved(it.name, it.path, document.sample) }
            } catch (e: PatchFileException) {
                ResultIntent.SaveFailed(shownPath, e.message.orEmpty())
            }
        result?.let(dispatch)
    }

    /** Writes [bytes] to [currentPath], or asks where to save when it is `null`. */
    private suspend fun writeFile(
        currentPath: String?,
        shownName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile? =
        if (currentPath != null) {
            files.write(currentPath, bytes)
            SavedFile(shownName, currentPath)
        } else {
            files.pickAndWrite(shownName, bytes, kind)
        }
}
