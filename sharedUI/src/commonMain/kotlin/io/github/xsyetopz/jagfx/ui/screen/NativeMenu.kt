package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.compositionLocalOf

/**
 * `true` when platform shows editor's menus itself, as desktop window's menu bar does.
 * Then title bar leaves out its File menu, Undo, and Redo, and editor leaves out Space
 * shortcut, which menu bar already has. `false` in browser.
 */
internal val LocalNativeMenu = compositionLocalOf { false }
