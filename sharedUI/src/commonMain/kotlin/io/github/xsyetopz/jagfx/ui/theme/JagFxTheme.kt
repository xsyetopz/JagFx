package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The editor's colors, after the Scala JagFX: flat graphite panels, 1 px borders, and one accent
 * that marks what is active or selected.
 * The plots draw their own colors from [SynthPalette] in both appearances.
 */
@Immutable
internal data class JagFxColors(
    val root: Color,
    val header: Color,
    val panel: Color,
    val footer: Color,
    val cell: Color,
    val cellSelected: Color,
    val button: Color,
    val buttonHover: Color,
    val buttonActive: Color,
    val input: Color,
    val text: Color,
    val textDim: Color,
    val textSubtle: Color,
    val textButton: Color,
    val accent: Color,
    val border: Color,
    val warning: Color,
) {
    companion object {
        /** The dark appearance, the Scala JagFX palette. */
        val Dark: JagFxColors =
            JagFxColors(
                root = Color(0xFF121212),
                header = Color(0xFF161616),
                panel = Color(0xFF1E1E1E),
                footer = Color(0xFF1A1A1A),
                cell = Color(0xFF202020),
                cellSelected = Color(0xFF252525),
                button = Color(0xFF2A2A2A),
                buttonHover = Color(0xFF383838),
                buttonActive = Color(0xFF222222),
                input = Color(0xFF050505),
                text = Color(0xFFF0F0F0),
                textDim = Color(0xFF888888),
                textSubtle = Color(0xFF555555),
                textButton = Color(0xFFBBBBBB),
                accent = Color(0xFF33BBEE),
                border = Color(0xFF333333),
                warning = Color(0xFFCC79A7),
            )

        /** The light appearance: the same structure with the lightness inverted. */
        val Light: JagFxColors =
            JagFxColors(
                root = Color(0xFFE6E6E6),
                header = Color(0xFFDCDCDC),
                panel = Color(0xFFEFEFEF),
                footer = Color(0xFFE2E2E2),
                cell = Color(0xFFF7F7F7),
                cellSelected = Color(0xFFFFFFFF),
                button = Color(0xFFD4D4D4),
                buttonHover = Color(0xFFC4C4C4),
                buttonActive = Color(0xFFE8E8E8),
                input = Color(0xFFFFFFFF),
                text = Color(0xFF121212),
                textDim = Color(0xFF5C5C5C),
                textSubtle = Color(0xFF9A9A9A),
                textButton = Color(0xFF333333),
                accent = Color(0xFF1386B8),
                border = Color(0xFFBDBDBD),
                warning = Color(0xFFA8457E),
            )
    }
}

/** Monospace type at the Scala JagFX sizes: [body] 10, [label] 9 bold, [heading] 12 bold. */
@Immutable
internal data class JagFxTypography(
    val body: TextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
    val label: TextStyle =
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold),
    val heading: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        ),
)

/** Sizes and spacing for a desktop with a pointer. */
@Immutable
internal data class JagFxDimens(
    val headerHeight: Dp = 36.dp,
    val controlHeight: Dp = 18.dp,
    val cellHeaderHeight: Dp = 20.dp,
    val inspectorWidth: Dp = 240.dp,
    val voiceChipWidth: Dp = 28.dp,
    val voiceChipHeight: Dp = 24.dp,
    val fieldWidth: Dp = 44.dp,
    val buttonMinWidth: Dp = 18.dp,
    val iconSize: Dp = 12.dp,
    val barHeight: Dp = 4.dp,
    val radius: Dp = 2.dp,
    val border: Dp = 1.dp,
    val spaceXs: Dp = 1.dp,
    val spaceSm: Dp = 2.dp,
    val spaceMd: Dp = 4.dp,
    val spaceLg: Dp = 6.dp,
    val spaceXl: Dp = 8.dp,
)

/** Opacity of a disabled control. */
internal const val DISABLED_ALPHA = 0.4f

private val LocalColors = staticCompositionLocalOf { JagFxColors.Dark }
private val LocalTypography = staticCompositionLocalOf { JagFxTypography() }
private val LocalDimens = staticCompositionLocalOf { JagFxDimens() }

/** The color of text and icons at this point of the tree. */
internal val LocalContentColor = compositionLocalOf { JagFxColors.Dark.text }

/**
 * Provides the editor's colors, type, and sizes to [content].
 * [darkTheme] picks the dark or light colors; it follows the system appearance by default.
 */
@Composable
internal fun JagFxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) JagFxColors.Dark else JagFxColors.Light
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides JagFxTypography(),
        LocalDimens provides JagFxDimens(),
        LocalContentColor provides colors.text,
        LocalTextSelectionColors provides
            TextSelectionColors(
                handleColor = colors.accent,
                backgroundColor = colors.accent.copy(alpha = 0.4f),
            ),
        content = content,
    )
}

/** Reads the current theme values. */
internal object JagFxTheme {
    val colors: JagFxColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current

    val typography: JagFxTypography
        @Composable @ReadOnlyComposable
        get() = LocalTypography.current

    val dimens: JagFxDimens
        @Composable @ReadOnlyComposable
        get() = LocalDimens.current
}
