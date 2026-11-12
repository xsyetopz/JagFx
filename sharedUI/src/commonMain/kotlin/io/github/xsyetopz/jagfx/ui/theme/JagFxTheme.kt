package io.github.xsyetopz.jagfx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The look of the editor: dark [Classic], [Light], or [HighContrast] black, white, and yellow. */
internal enum class JagFxAppearance { Classic, Light, HighContrast }

/**
 * The editor's colors: flat graphite panels, 1 px borders, and one accent that marks what is
 * active, selected, or focused.
 * The plots draw their own colors from [SynthPalette]. [border] separates panels; [controlBorder]
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
        /** The Classic appearance: dark grey chrome, green for what is active. */
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

        /** The colors of [appearance]. */
        fun of(appearance: JagFxAppearance): JagFxColors =
            when (appearance) {
                JagFxAppearance.Classic -> Classic
                JagFxAppearance.Light -> Light
                JagFxAppearance.HighContrast -> HighContrast
            }
    }
}

/** The plot colors of [appearance]. */
internal fun SynthPalette.Companion.of(appearance: JagFxAppearance): SynthPalette =
    when (appearance) {
        JagFxAppearance.Classic -> Classic
        JagFxAppearance.Light -> Light
        JagFxAppearance.HighContrast -> HighContrast
    }

/** Monospace type: [body] 12 sp, [label] 11 sp bold, [heading] 14 sp bold, before text scale. */
@Immutable
internal data class JagFxTypography(
    val body: TextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
    val label: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        ),
    val heading: TextStyle =
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        ),
) {
    /** These styles with every size multiplied by [scale]. */
    fun scaled(scale: Float): JagFxTypography =
        JagFxTypography(
            body = body.copy(fontSize = body.fontSize * scale),
            label = label.copy(fontSize = label.fontSize * scale),
            heading = heading.copy(fontSize = heading.fontSize * scale),
        )
}

/** Sizes and spacing for a desktop with a pointer; every control is at least [minTarget] square. */
@Immutable
internal data class JagFxDimens(
    val minTarget: Dp = 24.dp,
    val headerHeight: Dp = 36.dp,
    val controlHeight: Dp = 24.dp,
    val cellHeaderHeight: Dp = 28.dp,
    val stripWidth: Dp = 160.dp,
    val voiceChipWidth: Dp = 28.dp,
    val voiceChipHeight: Dp = 24.dp,
    val fieldWidth: Dp = 44.dp,
    val buttonMinWidth: Dp = 24.dp,
    val iconSize: Dp = 12.dp,
    val barHeight: Dp = 4.dp,
    val focusRing: Dp = 2.dp,
    val radius: Dp = 2.dp,
    val border: Dp = 1.dp,
    val spaceXs: Dp = 1.dp,
    val spaceSm: Dp = 2.dp,
    val spaceMd: Dp = 4.dp,
    val spaceLg: Dp = 6.dp,
    val spaceXl: Dp = 8.dp,
) {
    /** These sizes with the text-sized ones multiplied by [scale], so scaled text still fits. */
    fun scaled(scale: Float): JagFxDimens =
        copy(
            headerHeight = headerHeight * scale,
            controlHeight = controlHeight * scale,
            cellHeaderHeight = cellHeaderHeight * scale,
            stripWidth = stripWidth * scale,
            voiceChipWidth = voiceChipWidth * scale,
            voiceChipHeight = voiceChipHeight * scale,
            fieldWidth = fieldWidth * scale,
        )
}

/** Opacity of a disabled control. */
internal const val DISABLED_ALPHA = 0.4f

private val LocalColors = staticCompositionLocalOf { JagFxColors.Classic }
private val LocalPalette = staticCompositionLocalOf { SynthPalette.Classic }
private val LocalTypography = staticCompositionLocalOf { JagFxTypography() }
private val LocalDimens = staticCompositionLocalOf { JagFxDimens() }

/** The color of text and icons at this point of the tree. */
internal val LocalContentColor = compositionLocalOf { JagFxColors.Classic.text }

/**
 * Provides the editor's colors, plot palette, type, and sizes to [content].
 * [appearance] follows the system dark setting by default; [textScale], from 1 to 2, multiplies
 * type and the text-sized dimensions.
 */
@Composable
internal fun JagFxTheme(
    appearance: JagFxAppearance =
        if (isSystemInDarkTheme()) JagFxAppearance.Classic else JagFxAppearance.Light,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = JagFxColors.of(appearance)
    val typography = remember(textScale) { JagFxTypography().scaled(textScale) }
    val dimens = remember(textScale) { JagFxDimens().scaled(textScale) }
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalPalette provides SynthPalette.of(appearance),
        LocalTypography provides typography,
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

/** Reads the current theme values. */
internal object JagFxTheme {
    val colors: JagFxColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current

    val palette: SynthPalette
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current

    val typography: JagFxTypography
        @Composable @ReadOnlyComposable
        get() = LocalTypography.current

    val dimens: JagFxDimens
        @Composable @ReadOnlyComposable
        get() = LocalDimens.current
}
