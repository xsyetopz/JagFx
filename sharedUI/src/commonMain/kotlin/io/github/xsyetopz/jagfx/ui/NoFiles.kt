package io.github.xsyetopz.jagfx.ui

import io.github.xsyetopz.jagfx.ui.editor.AudioOutput
import io.github.xsyetopz.jagfx.ui.editor.OpenedFile
import io.github.xsyetopz.jagfx.ui.editor.PatchFileException
import io.github.xsyetopz.jagfx.ui.editor.PatchFileKind
import io.github.xsyetopz.jagfx.ui.editor.PatchFiles
import io.github.xsyetopz.jagfx.ui.editor.SavedFile
import kotlinx.coroutines.flow.emptyFlow

/** File access of a platform that has none: pickers cancel and reads and writes fail. */
internal object NoFiles : PatchFiles {
    private const val UNAVAILABLE = "File access is not available on this platform"

    override suspend fun pickAndRead(kind: PatchFileKind): OpenedFile? = null

    override suspend fun read(path: String): ByteArray = throw PatchFileException(UNAVAILABLE)

    override suspend fun write(
        path: String,
        bytes: ByteArray,
    ): Unit = throw PatchFileException(UNAVAILABLE)

    override suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile? = null
}

/** Audio output of a platform that has none: playback ends at once. */
internal val SilentAudio: AudioOutput = AudioOutput { _, _ -> emptyFlow() }
