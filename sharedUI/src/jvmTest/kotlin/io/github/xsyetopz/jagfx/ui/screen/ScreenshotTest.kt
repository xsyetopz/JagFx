package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.ui.editor.EditorState
import io.github.xsyetopz.jagfx.ui.editor.RackMode
import io.github.xsyetopz.jagfx.ui.editor.ResultIntent
import io.github.xsyetopz.jagfx.ui.editor.SlotIntent
import io.github.xsyetopz.jagfx.ui.editor.ViewIntent
import io.github.xsyetopz.jagfx.ui.editor.reduce
import io.github.xsyetopz.jagfx.ui.theme.JagFxAppearance
import io.github.xsyetopz.jagfx.ui.theme.JagFxTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders the editor offscreen to PNG files for the README, in Main and Both modes at 1280x800
 * and 1920x1080. Runs only when `JAGFX_SCREENSHOT_DIR` names the output folder, so the normal
 * test run writes nothing.
 */
class ScreenshotTest {
    @Test
    fun rendersEditorScreenshots() {
        val outDir = System.getenv("JAGFX_SCREENSHOT_DIR")?.let(::File) ?: return
        outDir.mkdirs()
        val file = File(REFERENCE_SYNTH)
        val patch = SynthCodec.decode(file.readBytes()).patch
        val voice = patch.voices.indexOfFirst { it?.filter != null }.coerceAtLeast(0)
        val loaded =
            listOf(
                ResultIntent.Loaded(file.name, file.path, patch),
                SlotIntent.SelectVoice(voice),
                ResultIntent.PreviewRendered(Renderer.render(patch)),
            ).fold(EditorState(), ::reduce)
        for (mode in listOf(RackMode.Main, RackMode.Both)) {
            val state = reduce(loaded, ViewIntent.SetMode(mode))
            for ((width, height) in Sizes) {
                val name = "editor-${mode.name.lowercase()}-${width}x$height.png"
                ImageComposeScene(width, height, Density(1f)) {
                    JagFxTheme(JagFxAppearance.Classic) {
                        EditorContent(state, ScreenViewState(), EditorActions({}) {})
                    }
                }.use { scene ->
                    val png = scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes
                    File(outDir, name).writeBytes(png)
                }
            }
        }
    }

    private companion object {
        const val REFERENCE_SYNTH = "../references/synths/cow_death.synth"
        val Sizes = listOf(1280 to 800, 1920 to 1080)
    }
}
