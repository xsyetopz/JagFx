package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Look of editor: dark [Classic], [Light], or [HighContrast] black, white, and yellow. */
internal enum class JagFxAppearance { Classic, Light, HighContrast }

/**
 * Editor's colors: flat graphite panels, 1 px borders, and one accent that marks what is
 * active, selected, or focused.
 * Plots draw their own colors from [SynthPalette]. [border] separates panels; [controlBorder]
 * outlines controls at 3:1 contrast or more.
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
    val controlBorder: Color,
    val warning: Color,
) {
    companion object {
        /** Classic appearance: dark grey chrome, green for what is active. */
        val Classic: JagFxColors =
            JagFxColors(
                root = Color(0xFF121212),
                header = Color(0xFF161616),
                panel = Color(0xFF1E1E1E),
                footer = Color(0xFF1A1A1A),
                cell = Color(0xFF202020),
                cellSelected = Color(0xFF252525),
                button = Color(0xFF2A2A2A),
                buttonHover = Color(0xFF383838),
                buttonActive = Color(0xFF1F4D2B),
                input = Color(0xFF050505),
                text = Color(0xFFF0F0F0),
                textDim = Color(0xFFB8B8B8),
                textSubtle = Color(0xFFA4A4A4),
                textButton = Color(0xFFD0D0D0),
                accent = Color(0xFF66D17A),
                border = Color(0xFF333333),
                controlBorder = Color(0xFF8E8E8E),
                warning = Color(0xFFE08FBB),
            )

        /** Light appearance: same structure with lightness inverted. */
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
                textDim = Color(0xFF454545),
                textSubtle = Color(0xFF575757),
                textButton = Color(0xFF222222),
                accent = Color(0xFF075078),
                border = Color(0xFFBDBDBD),
                controlBorder = Color(0xFF6A6A6A),
                warning = Color(0xFF8F3568),
            )

        /** Black and white with yellow for what is active; every text pair reaches 7:1. */
        val HighContrast: JagFxColors =
            JagFxColors(
                root = Color(0xFF000000),
                header = Color(0xFF000000),
                panel = Color(0xFF000000),
                footer = Color(0xFF000000),
                cell = Color(0xFF000000),
                cellSelected = Color(0xFF1A1A1A),
                button = Color(0xFF000000),
                buttonHover = Color(0xFF333333),
                buttonActive = Color(0xFF1A1A1A),
                input = Color(0xFF000000),
                text = Color(0xFFFFFFFF),
                textDim = Color(0xFFE6E6E6),
                textSubtle = Color(0xFFD0D0D0),
                textButton = Color(0xFFFFFFFF),
                accent = Color(0xFFFFFF00),
                border = Color(0xFF8A8A8A),
                controlBorder = Color(0xFFFFFFFF),
                warning = Color(0xFFFF9FD0),
            )

        /** Colors of [appearance]. */
        fun of(appearance: JagFxAppearance): JagFxColors =
            when (appearance) {
                JagFxAppearance.Classic -> Classic
                JagFxAppearance.Light -> Light
                JagFxAppearance.HighContrast -> HighContrast
            }
    }
}

/** Plot colors of [appearance]. */
internal fun SynthPalette.Companion.of(appearance: JagFxAppearance): SynthPalette =
    when (appearance) {
        JagFxAppearance.Classic -> Classic
        JagFxAppearance.Light -> Light
        JagFxAppearance.HighContrast -> HighContrast
    }

/** Material 3 type scale with every size multiplied by [scale], so scaled text still fits. */
internal fun scaledTypography(scale: Float): Typography {
    fun TextStyle.scaled() = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled(),
    )
}

/** Sizes and spacing for desktop with pointer; every control is at least [minTarget] square. */
@Immutable
internal data class JagFxDimens(
    val minTarget: Dp = 24.dp,
    val headerHeight: Dp = 36.dp,
    val controlHeight: Dp = 24.dp,
    val cellHeaderHeight: Dp = 28.dp,
    val stripWidth: Dp = 160.dp,
    val voiceChipWidth: Dp = 28.dp,
    val voiceChipHeight: Dp = 24.dp,
    val buttonMinWidth: Dp = 24.dp,
    val iconSize: Dp = 12.dp,
    val focusRing: Dp = 2.dp,
    val radius: Dp = 2.dp,
    val border: Dp = 1.dp,
    val spaceXs: Dp = 1.dp,
    val spaceSm: Dp = 2.dp,
    val spaceMd: Dp = 4.dp,
    val spaceLg: Dp = 6.dp,
    val spaceXl: Dp = 8.dp,
) {
    /** These sizes with text-sized ones multiplied by [scale], so scaled text still fits. */
    fun scaled(scale: Float): JagFxDimens =
        copy(
            headerHeight = headerHeight * scale,
            controlHeight = controlHeight * scale,
            cellHeaderHeight = cellHeaderHeight * scale,
            stripWidth = stripWidth * scale,
            voiceChipWidth = voiceChipWidth * scale,
            voiceChipHeight = voiceChipHeight * scale,
        )
}

/** Opacity of disabled control. */
internal const val DISABLED_ALPHA = 0.4f

private val LocalColors = staticCompositionLocalOf { JagFxColors.Classic }
private val LocalPalette = staticCompositionLocalOf { SynthPalette.Classic }
private val LocalDimens = staticCompositionLocalOf { JagFxDimens() }

/**
 * Provides editor's colors, plot palette, type, and sizes to [content].
 * [appearance] follows system dark setting by default; [textScale], from 1 to 2, multiplies
 * type and text-sized dimensions.
 */
@Composable
internal fun JagFxTheme(
    appearance: JagFxAppearance =
        if (isSystemInDarkTheme()) JagFxAppearance.Classic else JagFxAppearance.Light,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = JagFxColors.of(appearance)
    val typography = remember(textScale) { scaledTypography(textScale) }
    val dimens = remember(textScale) { JagFxDimens().scaled(textScale) }
    MaterialTheme(colorScheme = jagFxColorScheme(appearance), typography = typography) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides
                RippleConfiguration(color = colors.accent),
            LocalMinimumInteractiveComponentSize provides dimens.minTarget,
        ) {
            JagFxProviders(colors, appearance, dimens, content)
        }
    }
}

@Composable
private fun JagFxProviders(
    colors: JagFxColors,
    appearance: JagFxAppearance,
    dimens: JagFxDimens,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalPalette provides SynthPalette.of(appearance),
        LocalDimens provides dimens,
        LocalContentColor provides colors.text,
        LocalTextSelectionColors provides
            TextSelectionColors(
                handleColor = colors.accent,
                backgroundColor = colors.accent.copy(alpha = 0.4f),
            ),
        content = content,
    )
}

/** Reads current theme values. */
internal object JagFxTheme {
    val colors: JagFxColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current

    val palette: SynthPalette
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current

    val dimens: JagFxDimens
        @Composable @ReadOnlyComposable
        get() = LocalDimens.current
}
