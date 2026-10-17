package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.OpenedFile
import io.github.xsyetopz.jagfx.ui.editor.PatchFileException
import io.github.xsyetopz.jagfx.ui.editor.PatchFileKind
import io.github.xsyetopz.jagfx.ui.editor.PatchFiles
import io.github.xsyetopz.jagfx.ui.editor.SavedFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.name

/**
 * Desktop file access: native AWT file dialogs owned by [owner], and `java.nio` reads and
 * writes on [io]. Dialogs open on the Swing event thread through [Dispatchers.Main].
 */
class DesktopPatchFiles(
    private val owner: () -> Frame? = { null },
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PatchFiles {
    override suspend fun pickAndRead(kind: PatchFileKind): OpenedFile? {
        val file =
            showDialog(FileDialog.LOAD, "Open .${kind.extension} file", kind, suggestedName = null)
                ?: return null
        return OpenedFile(file.name, file.path, read(file.path))
    }

    override suspend fun read(path: String): ByteArray = io { Files.readAllBytes(toPath(path)) }

    /** Writes to a temporary sibling and moves it over [path], so a failed write keeps the old file. */
    override suspend fun write(
        path: String,
        bytes: ByteArray,
    ): Unit =
        io {
            val target = toPath(path).toAbsolutePath()
            val temp = Files.createTempFile(target.parent, ".${target.name}", ".tmp")
            try {
                Files.write(temp, bytes)
                try {
                    Files.move(
                        temp,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temp)
            }
        }

    override suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile? {
        val picked =
            showDialog(FileDialog.SAVE, "Save .${kind.extension} file", kind, suggestedName)
                ?: return null
        val file =
            if (picked.name.contains(
                    '.',
                )
            ) {
                picked
            } else {
                File("${picked.path}.${kind.extension}")
            }
        write(file.path, bytes)
        return SavedFile(file.name, file.path)
    }

    private suspend fun showDialog(
        mode: Int,
        title: String,
        kind: PatchFileKind,
        suggestedName: String?,
    ): File? =
        withContext(Dispatchers.Main) {
            val extension = ".${kind.extension}"
            val dialog = FileDialog(owner(), title, mode)
            dialog.setFilenameFilter {
                _,
                name,
                ->
                name.endsWith(extension, ignoreCase = true)
            }
            // Windows ignores the filter; a wildcard file name filters there instead.
            dialog.file = suggestedName ?: "*$extension"
            dialog.isVisible = true
            val name = dialog.file
            val directory = dialog.directory
            dialog.dispose()
            if (name == null || directory == null) null else File(directory, name).absoluteFile
        }

    private suspend fun <T> io(block: () -> T): T =
        withContext(io) {
            try {
                block()
            } catch (e: IOException) {
                throw PatchFileException(describe(e), e)
            } catch (e: SecurityException) {
                throw PatchFileException(e.message ?: e.toString(), e)
            }
        }

    private fun toPath(path: String): Path =
        try {
            Paths.get(path)
        } catch (e: InvalidPathException) {
            throw PatchFileException(e.message ?: path, e)
        }
}

private fun describe(e: IOException): String =
    when (e) {
        is NoSuchFileException -> "No such file: ${e.file}"
        is AccessDeniedException -> "Access denied: ${e.file}"
        else -> e.message ?: e.toString()
    }
