package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `true` when the platform asks for less motion ([io.github.xsyetopz.jagfx.ui.editor.PlatformSettings.reducedMotion]).
 * The editor has no animations; [io.github.xsyetopz.jagfx.ui.editor.EditorViewModel] lowers the
 * playhead rate to 1 Hz itself.
 */
internal val LocalReducedMotion = staticCompositionLocalOf { false }
