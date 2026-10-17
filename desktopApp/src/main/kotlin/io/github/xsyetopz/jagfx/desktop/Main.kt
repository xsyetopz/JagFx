package io.github.xsyetopz.jagfx.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.application
import io.github.xsyetopz.jagfx.ui.desktop.JagFxWindow
import io.github.xsyetopz.jagfx.ui.platform.JavaSoundOutput
import javax.imageio.ImageIO

private const val ICON_RESOURCE = "/jagfx-icon.png"

private val isMacOs = System.getProperty("os.name").orEmpty().startsWith("Mac")

fun main() {
    if (isMacOs) {
        // Before AWT starts: the menus go to the screen menu bar, the app menu takes the app name,
        // and the title bar follows the system appearance.
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "JagFx")
        System.setProperty("apple.awt.application.appearance", "system")
    }
    // On macOS the bundle .icns follows the system icon grid; a full-bleed runtime icon would override it.
    val icon =
        if (isMacOs) {
            null
        } else {
            object {}.javaClass.getResourceAsStream(ICON_RESOURCE)?.use { stream ->
                BitmapPainter(ImageIO.read(stream).toComposeImageBitmap())
            }
        }
    val audio = JavaSoundOutput()
    val home: String? = System.getProperty("user.home")
    application {
        JagFxWindow(audio = audio, home = home, icon = icon)
    }
}
