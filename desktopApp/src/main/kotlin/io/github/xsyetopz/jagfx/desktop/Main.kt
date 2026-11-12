package io.github.xsyetopz.jagfx.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.application
import io.github.xsyetopz.jagfx.ui.desktop.JagFxWindow
import io.github.xsyetopz.jagfx.ui.platform.DesktopPlatformSettings
import io.github.xsyetopz.jagfx.ui.platform.DesktopPreferences
import io.github.xsyetopz.jagfx.ui.platform.JavaSoundOutput
import javax.imageio.ImageIO

private const val ICON_RESOURCE = "/jagfx-icon.png"

private val isMacOs = System.getProperty("os.name").orEmpty().startsWith("Mac")

fun main(args: Array<String>) {
    val hidden = System.getenv("JAGFX_HIDDEN") == "1"
    if (isMacOs) {
        // Before AWT starts: the menus go to the screen menu bar, the app menu takes the app name,
        // and the title bar follows the system appearance.
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "JagFx")
        System.setProperty("apple.awt.application.appearance", "system")
        // A UI element app has no Dock icon or menu bar and does not take activation from the user.
        if (hidden) System.setProperty("apple.awt.UIElement", "true")
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
    val preferences = DesktopPreferences()
    val settings = DesktopPlatformSettings()
    val home: String? = System.getProperty("user.home")
    application {
        JagFxWindow(
            audio = audio,
            preferences = preferences,
            settings = settings,
            home = home,
            icon = icon,
            hidden = hidden,
            openPath = args.firstOrNull(),
        )
    }
}
