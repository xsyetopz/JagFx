@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.xsyetopz.jagfx.ui.platform

import io.github.xsyetopz.jagfx.ui.editor.OpenedFile
import io.github.xsyetopz.jagfx.ui.editor.PatchFileException
import io.github.xsyetopz.jagfx.ui.editor.PatchFileKind
import io.github.xsyetopz.jagfx.ui.editor.PatchFiles
import io.github.xsyetopz.jagfx.ui.editor.SavedFile
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val REVOKE_DELAY_MS = 1000
private const val NO_PATHS = "The browser has no file paths"

/**
 * Browser file access. Opening uses a file `<input>`; saving downloads a Blob, so a saved file
 * has no path and the next save asks again. Files have no paths, so [read] and [write] are
 * never reached through the editor.
 */
class BrowserPatchFiles : PatchFiles {
    override suspend fun pickAndRead(kind: PatchFileKind): OpenedFile? {
        val file = pickFile(".${kind.extension}") ?: return null
        return OpenedFile(file.name, null, readBytes(file))
    }

    override suspend fun read(path: String): ByteArray = throw PatchFileException(NO_PATHS)

    override suspend fun write(
        path: String,
        bytes: ByteArray,
    ): Unit = throw PatchFileException(NO_PATHS)

    override suspend fun pickAndWrite(
        suggestedName: String,
        bytes: ByteArray,
        kind: PatchFileKind,
    ): SavedFile {
        val blob =
            Blob(jsArrayOf(bytes.toInt8Array()), BlobPropertyBag(type = "application/octet-stream"))
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = suggestedName
        anchor.click()
        // Some browsers drop a download whose URL is revoked during the click.
        window.setTimeout({
            URL.revokeObjectURL(url)
            null
        }, REVOKE_DELAY_MS)
        return SavedFile(suggestedName, null)
    }
}

private suspend fun pickFile(accept: String): File? =
    suspendCancellableCoroutine { continuation ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = accept
        input.addEventListener("change", {
            if (continuation.isActive) continuation.resume(input.files?.get(0))
        })
        input.addEventListener("cancel", { if (continuation.isActive) continuation.resume(null) })
        input.click()
    }

private suspend fun readBytes(file: File): ByteArray =
    suspendCancellableCoroutine { continuation ->
        val reader = FileReader()
        reader.onload =
            { continuation.resume(Int8Array(reader.result as ArrayBuffer).toByteArray()) }
        reader.onerror = {
            continuation.resumeWithException(PatchFileException("Cannot read ${file.name}"))
        }
        continuation.invokeOnCancellation { reader.abort() }
        reader.readAsArrayBuffer(file)
    }

private fun jsArrayOf(vararg items: JsAny?): JsArray<JsAny?> =
    JsArray<JsAny?>().also { array -> items.forEachIndexed { i, item -> array[i] = item } }
