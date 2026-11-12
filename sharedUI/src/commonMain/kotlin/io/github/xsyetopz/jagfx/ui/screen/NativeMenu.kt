package io.github.xsyetopz.jagfx.ui.screen

import androidx.compose.runtime.compositionLocalOf

/**
 * `true` when the platform shows the editor's menus itself, as the desktop window's menu bar does.
 * Then the editor leaves out the Space shortcut, which the menu bar already binds. `false` in the
 * browser.
 */
internal val LocalNativeMenu = compositionLocalOf { false }
