package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `true` when platform asks for less motion ([io.github.xsyetopz.jagfx.ui.editor.PlatformSettings.reducedMotion]).
 * Editor has no animations; [io.github.xsyetopz.jagfx.ui.editor.EditorViewModel] lowers
 * playhead rate to 1 Hz itself.
 */
internal val LocalReducedMotion = staticCompositionLocalOf { false }
