using Avalonia;
using Avalonia.Media;

namespace JagFx.Desktop.Controls.Canvases;

internal static class AnalysisGridRenderer
{
    internal const double ReferenceWidth = 448.0;
    internal const double ReferenceHeight = 476.0;
    internal const double AnalysisHorizontalStep = 22.0;
    private const double HorizontalLineStep = 4.0;
    private const int SecondaryLinesPerPrimary = 3;
    private const double TransparentToFillBandWidth = 3.0;
    private const double FillToTransparentBandWidth = 4.0;

    private static readonly IPen FrequencyLayerOnePen = new Pen(
        new SolidColorBrush(ThemeColors.AnalysisGridMajorBoundary).ToImmutable(),
        1
    ).ToImmutable();

    private static readonly IPen FrequencyLayerTwoPen = new Pen(
        new SolidColorBrush(ThemeColors.AnalysisGridProminentStruct).ToImmutable(),
        1
    ).ToImmutable();

    private static readonly IPen FrequencyLayerThreePen = new Pen(
        new SolidColorBrush(ThemeColors.AnalysisGridMicroStep).ToImmutable(),
        1
    ).ToImmutable();

    private static readonly IPen FrequencyLayerFourPen = new Pen(
        new SolidColorBrush(ThemeColors.AnalysisGridDimAnchor).ToImmutable(),
        2
    ).ToImmutable();

    public static void Draw(
        DrawingContext context,
        Rect bounds,
        bool includeVertical,
        int zoomLevel = 1
    )
    {
        if (bounds.Width < 1 || bounds.Height < 1)
        {
            return;
        }

        if (includeVertical)
        {
            DrawFrequencyGrid(context, bounds, zoomLevel);
            return;
        }

        DrawTransitionGrid(context, bounds);
    }

    private static void DrawTransitionGrid(DrawingContext context, Rect bounds)
    {
        var lineIndex = 0;
        for (var y = bounds.Top; y <= bounds.Bottom; y += HorizontalLineStep)
        {
            DrawHorizontalLineRaw(context, bounds, y, HorizontalPenFor(lineIndex));
            lineIndex++;
        }
    }

    private static IPen HorizontalPenFor(int lineIndex) =>
        (lineIndex + 1) % (SecondaryLinesPerPrimary + 1) == 0
            ? ThemeColors.AnalysisGridHorizontalPrimaryPen
            : ThemeColors.AnalysisGridHorizontalSecondaryPen;

    private static void DrawFrequencyGrid(DrawingContext context, Rect bounds, int zoomLevel)
    {
        DrawFrequencyLayerFive(context, bounds, zoomLevel);
        DrawFrequencyLayerSix(context, bounds, zoomLevel);
        DrawFrequencyLayerThree(context, bounds, zoomLevel);
        DrawFrequencyLayerFour(context, bounds, zoomLevel);
        DrawFrequencyLayerOne(context, bounds, zoomLevel);
        DrawFrequencyLayerTwo(context, bounds);
    }

    private static void DrawFrequencyLayerOne(DrawingContext context, Rect bounds, int zoomLevel)
    {
        var step = bounds.Height * AnalysisHorizontalStep / ReferenceHeight;
        for (var y = bounds.Top; y <= bounds.Bottom; y += step)
        {
            DrawHorizontalLineRaw(context, bounds, y, FrequencyLayerOnePen);
        }

        DrawSemitoneLines(
            context,
            bounds,
            FilterFrequencyScale.IsOctaveSemitone,
            FrequencyLayerOnePen,
            zoomLevel
        );
    }

    private static void DrawFrequencyLayerTwo(DrawingContext context, Rect bounds)
    {
        var step = bounds.Height * AnalysisHorizontalStep / ReferenceHeight;
        for (var y = bounds.Top + step / 2.0; y <= bounds.Bottom; y += step)
        {
            DrawHorizontalLineRaw(context, bounds, y, FrequencyLayerTwoPen);
        }
    }

    private static void DrawFrequencyLayerThree(
        DrawingContext context,
        Rect bounds,
        int zoomLevel
    ) => DrawSemitoneLines(context, bounds, _ => true, FrequencyLayerThreePen, zoomLevel);

    private static void DrawFrequencyLayerFour(DrawingContext context, Rect bounds, int zoomLevel)
    {
        DrawSemitoneLines(
            context,
            bounds,
            FilterFrequencyScale.IsHarmonicAnchorSemitone,
            FrequencyLayerFourPen,
            zoomLevel
        );
    }

    private static void DrawFrequencyLayerFive(DrawingContext context, Rect bounds, int zoomLevel)
    {
        foreach (var x in AccidentalBandPositions(bounds, zoomLevel))
        {
            DrawGradientBand(
                context,
                bounds,
                x - TransparentToFillBandWidth,
                TransparentToFillBandWidth,
                ThemeColors.AnalysisGridMajorBand,
                BandGradientDirection.TransparentToFill
            );
        }
    }

    private static void DrawFrequencyLayerSix(DrawingContext context, Rect bounds, int zoomLevel)
    {
        foreach (var x in AccidentalBandPositions(bounds, zoomLevel))
        {
            DrawGradientBand(
                context,
                bounds,
                x,
                FillToTransparentBandWidth,
                ThemeColors.AnalysisGridProminentBand,
                BandGradientDirection.FillToTransparent
            );
        }
    }

    private static void DrawHorizontalLineRaw(
        DrawingContext context,
        Rect bounds,
        double y,
        IPen pen
    )
    {
        var snappedY = ThemeColors.Snap(y);
        context.DrawLine(pen, new Point(bounds.Left, snappedY), new Point(bounds.Right, snappedY));
    }

    private static void DrawSemitoneLines(
        DrawingContext context,
        Rect bounds,
        Func<int, bool> shouldDraw,
        IPen pen,
        int zoomLevel
    )
    {
        for (
            var semitone = FilterFrequencyScale.FirstVisibleSemitone();
            semitone <= FilterFrequencyScale.LastVisibleSemitone();
            semitone++
        )
        {
            if (!shouldDraw(semitone))
            {
                continue;
            }

            var frequency = FilterFrequencyScale.SemitoneToFrequency(semitone);
            if (
                frequency
                is < FilterFrequencyScale.MinimumFrequencyHz
                    or > FilterFrequencyScale.MaximumFrequencyHz
            )
            {
                continue;
            }

            DrawVerticalLine(context, bounds, FrequencyToX(bounds, frequency, zoomLevel), pen);
        }
    }

    private static IEnumerable<double> AccidentalBandPositions(Rect bounds, int zoomLevel)
    {
        for (
            var semitone = FilterFrequencyScale.FirstVisibleSemitone();
            semitone <= FilterFrequencyScale.LastVisibleSemitone();
            semitone++
        )
        {
            if (!FilterFrequencyScale.IsAccidentalSemitone(semitone))
            {
                continue;
            }

            var frequency = FilterFrequencyScale.SemitoneToFrequency(semitone);
            if (
                frequency
                is < FilterFrequencyScale.MinimumFrequencyHz
                    or > FilterFrequencyScale.MaximumFrequencyHz
            )
            {
                continue;
            }

            yield return FrequencyToX(bounds, frequency, zoomLevel);
        }
    }

    private static double FrequencyToX(Rect bounds, double frequency, int zoomLevel) =>
        bounds.Left
        + bounds.Width
            * Math.Max(1, zoomLevel)
            * FilterFrequencyScale.FrequencyToNormalizedX(frequency);

    private static void DrawVerticalLine(DrawingContext context, Rect bounds, double x, IPen pen)
    {
        var snappedX = ThemeColors.Snap(x);
        context.DrawLine(pen, new Point(snappedX, bounds.Top), new Point(snappedX, bounds.Bottom));
    }

    private static void DrawGradientBand(
        DrawingContext context,
        Rect bounds,
        double x,
        double width,
        Color color,
        BandGradientDirection direction
    )
    {
        var columns = Math.Max(1, (int)Math.Round(width));
        var left = Math.Round(x);

        for (var column = 0; column < columns; column++)
        {
            var opacity =
                direction == BandGradientDirection.FillToTransparent
                    ? (columns - column) / (double)columns
                    : (column + 1) / (double)columns;

            var alpha = (byte)Math.Round(color.A * opacity);
            if (alpha == 0)
            {
                continue;
            }

            var brush = new SolidColorBrush(
                Color.FromArgb(alpha, color.R, color.G, color.B)
            ).ToImmutable();
            context.FillRectangle(
                brush,
                new Rect(Math.Round(left + column), bounds.Top, 1, bounds.Height)
            );
        }
    }

    private enum BandGradientDirection
    {
        FillToTransparent,
        TransparentToFill,
    }
}
