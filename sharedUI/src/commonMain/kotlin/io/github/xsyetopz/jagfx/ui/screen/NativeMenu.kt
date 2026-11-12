package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.compositionLocalOf

/**
 * `true` when the platform shows the editor's menus itself, as the desktop window's menu bar does.
 * Then the title bar leaves out its File menu, Undo, and Redo, and the editor leaves out the Space
 * shortcut, which the menu bar already has. `false` in the browser.
 */
internal val LocalNativeMenu = compositionLocalOf { false }
