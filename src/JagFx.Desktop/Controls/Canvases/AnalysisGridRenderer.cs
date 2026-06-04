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
    private const double MajorBandWidth = 18.0;
    private const double ProminentBandWidth = 12.0;
    private const double StandardBandWidth = 8.0;
    private const double DimBandWidth = 5.0;

    private static readonly GridLine[] StructuralPattern =
    [
        new(GridLineKind.Major, 8),
        new(GridLineKind.Micro, 13),
        new(GridLineKind.Micro, 17),
        new(GridLineKind.Dim, 22),
        new(GridLineKind.Micro, 26),
        new(GridLineKind.Micro, 30),
        new(GridLineKind.Prominent, 35),
        new(GridLineKind.Micro, 40),
        new(GridLineKind.Micro, 45),
        new(GridLineKind.Standard, 50),
        new(GridLineKind.Micro, 55),
        new(GridLineKind.Micro, 60),
        new(GridLineKind.Major, 65),
        new(GridLineKind.Micro, 71),
        new(GridLineKind.Micro, 77),
        new(GridLineKind.Dim, 83),
        new(GridLineKind.Micro, 89),
        new(GridLineKind.Micro, 95),
        new(GridLineKind.Prominent, 101),
        new(GridLineKind.Micro, 107),
        new(GridLineKind.Micro, 114),
        new(GridLineKind.Standard, 121),
        new(GridLineKind.Micro, 128),
        new(GridLineKind.Micro, 135),
        new(GridLineKind.Major, 142),
        new(GridLineKind.Micro, 150),
        new(GridLineKind.Micro, 158),
        new(GridLineKind.Dim, 166),
        new(GridLineKind.Micro, 174),
        new(GridLineKind.Micro, 182),
        new(GridLineKind.Prominent, 191),
        new(GridLineKind.Micro, 200),
        new(GridLineKind.Micro, 209),
        new(GridLineKind.Standard, 218),
        new(GridLineKind.Micro, 227),
        new(GridLineKind.Micro, 236),
        new(GridLineKind.Major, 246),
        new(GridLineKind.Micro, 256),
        new(GridLineKind.Micro, 266),
        new(GridLineKind.Dim, 276),
        new(GridLineKind.Micro, 286),
        new(GridLineKind.Micro, 296),
        new(GridLineKind.Prominent, 307),
        new(GridLineKind.Micro, 318),
        new(GridLineKind.Micro, 329),
        new(GridLineKind.Standard, 340),
        new(GridLineKind.Micro, 351),
        new(GridLineKind.Micro, 362),
        new(GridLineKind.Major, 374),
        new(GridLineKind.Micro, 386),
        new(GridLineKind.Micro, 398),
        new(GridLineKind.Dim, 410),
        new(GridLineKind.Micro, 422),
        new(GridLineKind.Micro, 434),
        new(GridLineKind.Major, 446),
    ];

    public static void Draw(DrawingContext context, Rect bounds, bool includeVertical)
    {
        if (bounds.Width < 1 || bounds.Height < 1)
            return;

        DrawHorizontalLines(context, bounds);

        if (includeVertical)
            DrawVerticalLines(context, bounds);
    }

    private static void DrawHorizontalLines(DrawingContext context, Rect bounds)
    {
        var lineIndex = 0;
        for (var y = bounds.Top; y <= bounds.Bottom; y += HorizontalLineStep)
        {
            var snappedY = ThemeColors.Snap(y);
            context.DrawLine(
                HorizontalPenFor(lineIndex),
                new Point(bounds.Left, snappedY),
                new Point(bounds.Right, snappedY)
            );
            lineIndex++;
        }
    }

    private static IPen HorizontalPenFor(int lineIndex) =>
        (lineIndex + 1) % (SecondaryLinesPerPrimary + 1) == 0
            ? ThemeColors.AnalysisGridHorizontalPrimaryPen
            : ThemeColors.AnalysisGridHorizontalSecondaryPen;

    private static void DrawVerticalLines(DrawingContext context, Rect bounds)
    {
        foreach (var line in StructuralPattern)
        {
            DrawVerticalBand(context, bounds, line);
        }
    }

    private static void DrawVerticalBand(DrawingContext context, Rect bounds, GridLine line)
    {
        var x = bounds.X + bounds.Width * line.X / ReferenceWidth;
        var bandWidth = bounds.Width * BandWidthFor(line.Kind) / ReferenceWidth;
        var brush = BandBrushFor(line.Kind);

        if (brush is not null && bandWidth >= 1)
        {
            context.FillRectangle(
                brush,
                new Rect(ThemeColors.Snap(x - bandWidth / 2), bounds.Top, bandWidth, bounds.Height)
            );
        }

        var snappedX = ThemeColors.Snap(x);
        context.DrawLine(
            PenFor(line.Kind),
            new Point(snappedX, bounds.Top),
            new Point(snappedX, bounds.Bottom)
        );
    }

    private static double BandWidthFor(GridLineKind kind) =>
        kind switch
        {
            GridLineKind.Major => MajorBandWidth,
            GridLineKind.Prominent => ProminentBandWidth,
            GridLineKind.Standard => StandardBandWidth,
            GridLineKind.Dim => DimBandWidth,
            GridLineKind.Micro => 0,
            _ => 0,
        };

    private static IBrush? BandBrushFor(GridLineKind kind) =>
        kind switch
        {
            GridLineKind.Major => ThemeColors.AnalysisGridMajorBandBrush,
            GridLineKind.Prominent => ThemeColors.AnalysisGridProminentBandBrush,
            GridLineKind.Standard => ThemeColors.AnalysisGridStandardBandBrush,
            GridLineKind.Dim => ThemeColors.AnalysisGridDimBandBrush,
            GridLineKind.Micro => null,
            _ => null,
        };

    private static IPen PenFor(GridLineKind kind) =>
        kind switch
        {
            GridLineKind.Major => ThemeColors.AnalysisGridMajorBoundaryPen,
            GridLineKind.Prominent => ThemeColors.AnalysisGridProminentStructPen,
            GridLineKind.Standard => ThemeColors.AnalysisGridStandardDividerPen,
            GridLineKind.Dim => ThemeColors.AnalysisGridDimAnchorPen,
            GridLineKind.Micro => ThemeColors.AnalysisGridMicroStepPen,
            _ => ThemeColors.AnalysisGridMicroStepPen,
        };

    private readonly record struct GridLine(GridLineKind Kind, double X);

    private enum GridLineKind
    {
        Major,
        Prominent,
        Standard,
        Dim,
        Micro,
    }
}
