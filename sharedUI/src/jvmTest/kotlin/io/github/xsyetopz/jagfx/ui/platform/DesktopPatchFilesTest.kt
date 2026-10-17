package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.PatchFileException
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopPatchFilesTest {
    private val dir: Path = Files.createTempDirectory("jagfx-files")
    private val files = DesktopPatchFiles()

    @AfterTest
    fun tearDown() {
        dir.toFile().deleteRecursively()
    }

    @Test
    fun writeReplacesFileAndLeavesNoTemporaryFile() =
        runTest {
            val path = dir.resolve("a.synth").toString()
            files.write(path, byteArrayOf(1, 2, 3))
            files.write(path, byteArrayOf(4))

            assertContentEquals(byteArrayOf(4), files.read(path))
            assertEquals(listOf("a.synth"), dir.listDirectoryEntries().map { it.name })
        }

    @Test
    fun readOfMissingFileThrowsPatchFileException() =
        runTest {
            val path = dir.resolve("missing.synth").toString()

            val error = assertFailsWith<PatchFileException> { files.read(path) }
            assertTrue(error.message!!.startsWith("No such file"))
        }

    @Test
    fun writeIntoMissingDirectoryThrowsPatchFileException() =
        runTest {
            assertFailsWith<PatchFileException> {
                files.write(dir.resolve("none/a.synth").toString(), byteArrayOf())
            }
        }
}
