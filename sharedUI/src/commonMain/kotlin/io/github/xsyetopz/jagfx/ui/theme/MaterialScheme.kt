package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Material 3 [ColorScheme] of [appearance], derived from [JagFxColors]: surface is panel,
 * background root, primary accent, outline control border, and onSurface text.
 * [JagFxAppearance.HighContrast] gets its own scheme, built from high-contrast colors.
 */
internal fun jagFxColorScheme(appearance: JagFxAppearance): ColorScheme {
    val c = JagFxColors.of(appearance)
    return if (appearance == JagFxAppearance.Light) {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.root,
            primaryContainer = c.buttonActive,
            onPrimaryContainer = c.text,
            secondary = c.accent,
            onSecondary = c.root,
            background = c.root,
            onBackground = c.text,
            surface = c.panel,
            onSurface = c.text,
            surfaceVariant = c.button,
            onSurfaceVariant = c.textButton,
            outline = c.controlBorder,
            outlineVariant = c.border,
            error = c.warning,
            onError = c.root,
        )
    } else {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.root,
            primaryContainer = c.buttonActive,
            onPrimaryContainer = c.text,
            secondary = c.accent,
            onSecondary = c.root,
            background = c.root,
            onBackground = c.text,
            surface = c.panel,
            onSurface = c.text,
            surfaceVariant = c.button,
            onSurfaceVariant = c.textButton,
            outline = c.controlBorder,
            outlineVariant = c.border,
            error = c.warning,
            onError = c.root,
        )
    }
}
