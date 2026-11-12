package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `true` when the platform asks for less motion ([io.github.xsyetopz.jagfx.ui.editor.PlatformSettings.reducedMotion]).
 * The editor has no animations; this lowers the playhead rate from 4 Hz to 1 Hz.
 */
internal val LocalReducedMotion = staticCompositionLocalOf { false }
