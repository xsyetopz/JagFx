using Avalonia;
using Avalonia.Media;
using JagFx.Domain.Models;

namespace JagFx.Desktop.Controls;

/// <summary>
/// Centralized canvas drawing colors used across all custom controls.
/// </summary>
public static class ThemeColors
{
    public static readonly Color CanvasBackground = Color.Parse("#000000");
    public static readonly Color GridLineFaint = Color.Parse("#333333");
    public static readonly Color GridLine = Color.Parse("#1A1A1A");
    public static readonly Color UnitCircle = Color.Parse("#666666");
    public static readonly Color MidLine = Color.Parse("#808080");

    private const double AnalysisGridHorizontalPrimaryOpacity = 0.12;
    private const double AnalysisGridHorizontalSecondaryOpacity = 0.065;
    private const double AnalysisGridMajorBoundaryOpacity = 0.13;
    private const double AnalysisGridProminentStructOpacity = 0.06;
    private const double AnalysisGridStandardDividerOpacity = 0.055;
    private const double AnalysisGridDimAnchorOpacity = 0.045;
    private const double AnalysisGridMicroStepOpacity = 0.026;
    private const double AnalysisGridMajorBandOpacity = 0.055;
    private const double AnalysisGridProminentBandOpacity = 0.045;
    private const double AnalysisGridStandardBandOpacity = 0.04;
    private const double AnalysisGridDimBandOpacity = 0.03;
    public static readonly Color AnalysisGridMajorBoundary = WhiteWithOpacity(
        AnalysisGridMajorBoundaryOpacity
    );
    public static readonly Color AnalysisGridProminentStruct = WhiteWithOpacity(
        AnalysisGridProminentStructOpacity
    );
    public static readonly Color AnalysisGridStandardDivider = WhiteWithOpacity(
        AnalysisGridStandardDividerOpacity
    );
    public static readonly Color AnalysisGridDimAnchor = WhiteWithOpacity(
        AnalysisGridDimAnchorOpacity
    );
    public static readonly Color AnalysisGridMicroStep = WhiteWithOpacity(
        AnalysisGridMicroStepOpacity
    );
    public static readonly Color AnalysisGridMajorBand = WhiteWithOpacity(
        AnalysisGridMajorBandOpacity
    );
    public static readonly Color AnalysisGridProminentBand = WhiteWithOpacity(
        AnalysisGridProminentBandOpacity
    );
    public static readonly Color AnalysisGridStandardBand = WhiteWithOpacity(
        AnalysisGridStandardBandOpacity
    );
    public static readonly Color AnalysisGridDimBand = WhiteWithOpacity(AnalysisGridDimBandOpacity);
    public static readonly Color AnalysisGridHorizontalPrimary = WhiteWithOpacity(
        AnalysisGridHorizontalPrimaryOpacity
    );
    public static readonly Color AnalysisGridHorizontalSecondary = WhiteWithOpacity(
        AnalysisGridHorizontalSecondaryOpacity
    );
    public static readonly Color PoleAxisA = Color.Parse("#F0E442");
    public static readonly Color PoleAxisB = Color.Parse("#009E73");
    public static readonly Color Accent = Color.Parse("#009E73");
    public static readonly Color AccentSecondary = Color.Parse("#0072B2");
    public static readonly Color VoiceInactive = Color.Parse("#666666");
    public static readonly Color FilterLine = AccentSecondary;
    public static readonly Color EnvelopeLine = Color.Parse("#F0E442");
    public static readonly Color CellBorder = Color.Parse("#4D4D4D");
    public static readonly Color KnobTrack = Color.Parse("#4D4D4D");
    public static readonly Color KnobBody = Color.Parse("#0D0D0D");
    public static readonly Color KnobBorder = Color.Parse("#666666");
    public static readonly Color KnobLabel = Color.Parse("#CCCCCC");

    public static readonly IBrush CanvasBackgroundBrush = new SolidColorBrush(
        CanvasBackground
    ).ToImmutable();
    public static readonly IBrush AccentBrush = new SolidColorBrush(Accent).ToImmutable();
    public static readonly IBrush AccentSecondaryBrush = new SolidColorBrush(
        AccentSecondary
    ).ToImmutable();
    public static readonly IBrush FilterBrush = new SolidColorBrush(FilterLine).ToImmutable();
    public static readonly IBrush EnvelopeLineBrush = new SolidColorBrush(
        EnvelopeLine
    ).ToImmutable();
    public static readonly IBrush VoiceInactiveBrush = new SolidColorBrush(
        VoiceInactive
    ).ToImmutable();
    public static readonly IBrush CellBorderBrush = new SolidColorBrush(CellBorder).ToImmutable();
    public static readonly IBrush KnobTrackBrush = new SolidColorBrush(KnobTrack).ToImmutable();
    public static readonly IBrush KnobBodyBrush = new SolidColorBrush(KnobBody).ToImmutable();
    public static readonly IBrush KnobBorderBrush = new SolidColorBrush(KnobBorder).ToImmutable();
    public static readonly IBrush KnobLabelBrush = new SolidColorBrush(KnobLabel).ToImmutable();
    public static readonly IBrush AnalysisGridMajorBandBrush = new SolidColorBrush(
        AnalysisGridMajorBand
    ).ToImmutable();
    public static readonly IBrush AnalysisGridProminentBandBrush = new SolidColorBrush(
        AnalysisGridProminentBand
    ).ToImmutable();
    public static readonly IBrush AnalysisGridStandardBandBrush = new SolidColorBrush(
        AnalysisGridStandardBand
    ).ToImmutable();
    public static readonly IBrush AnalysisGridDimBandBrush = new SolidColorBrush(
        AnalysisGridDimBand
    ).ToImmutable();
    private static readonly DashStyle DotDash = new([1, 4], 0);
    public static readonly IPen GridFaintPen = new Pen(
        new SolidColorBrush(GridLineFaint).ToImmutable(),
        0.5,
        DotDash
    ).ToImmutable();
    public static readonly IPen GridPen = new Pen(
        new SolidColorBrush(GridLine).ToImmutable(),
        0.5,
        DotDash
    ).ToImmutable();
    public static readonly IPen UnitCirclePen = new Pen(
        new SolidColorBrush(UnitCircle).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen MidPen = new Pen(
        new SolidColorBrush(MidLine).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen AnalysisGridMajorBoundaryPen = new Pen(
        new SolidColorBrush(AnalysisGridMajorBoundary).ToImmutable(),
        2
    ).ToImmutable();
    public static readonly IPen AnalysisGridProminentStructPen = new Pen(
        new SolidColorBrush(AnalysisGridProminentStruct).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen AnalysisGridStandardDividerPen = new Pen(
        new SolidColorBrush(AnalysisGridStandardDivider).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen AnalysisGridDimAnchorPen = new Pen(
        new SolidColorBrush(AnalysisGridDimAnchor).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen AnalysisGridMicroStepPen = new Pen(
        new SolidColorBrush(AnalysisGridMicroStep).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen AnalysisGridHorizontalPrimaryPen = new Pen(
        new SolidColorBrush(AnalysisGridHorizontalPrimary).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen AnalysisGridHorizontalSecondaryPen = new Pen(
        new SolidColorBrush(AnalysisGridHorizontalSecondary).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen PoleAxisAPen = new Pen(
        new SolidColorBrush(
            Color.FromArgb(96, PoleAxisA.R, PoleAxisA.G, PoleAxisA.B)
        ).ToImmutable(),
        2
    ).ToImmutable();
    public static readonly IPen PoleAxisBPen = new Pen(
        new SolidColorBrush(
            Color.FromArgb(96, PoleAxisB.R, PoleAxisB.G, PoleAxisB.B)
        ).ToImmutable(),
        2
    ).ToImmutable();
    public static readonly IPen AccentPen2 = new Pen(AccentBrush, 2).ToImmutable();
    public static readonly IPen AccentPen1 = new Pen(AccentBrush, 1).ToImmutable();
    public static readonly IPen FilterPen = new Pen(FilterBrush, 1).ToImmutable();
    public static readonly IPen DimmedFilterPen = new Pen(
        new SolidColorBrush(Color.FromArgb(128, 0, 114, 178)).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen DimmedEnvelopeLinePen = new Pen(
        new SolidColorBrush(Color.FromArgb(128, 240, 228, 66)).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen EnvelopeLinePen = new Pen(EnvelopeLineBrush, 1).ToImmutable();
    public static readonly IPen AccentPenOnePointFive = new Pen(AccentBrush, 1).ToImmutable();
    public static readonly IPen SectionTracePen = new Pen(
        new SolidColorBrush(Color.Parse("#56B4E9")).ToImmutable(),
        1
    ).ToImmutable();
    public static readonly IPen WaveformPen = new Pen(AccentBrush, 1).ToImmutable();
    public static readonly IPen MarkerPen = new Pen(
        new SolidColorBrush(Color.Parse("#F5F5F5")).ToImmutable(),
        1
    ).ToImmutable();

    public static readonly Color VoiceSlotColor = EnvelopeLine;
    public static readonly Color FilterSlotColor = FilterLine;

    public static Color SlotColor(SignalChainSlot slot) =>
        slot switch
        {
            SignalChainSlot.PoleZero => AccentSecondary,
            SignalChainSlot.Bode => AccentSecondary,
            SignalChainSlot.Filter => AccentSecondary,
            SignalChainSlot.Pitch => EnvelopeLine,
            SignalChainSlot.VibratoRate => EnvelopeLine,
            SignalChainSlot.VibratoDepth => EnvelopeLine,
            SignalChainSlot.Volume => EnvelopeLine,
            SignalChainSlot.TremoloRate => EnvelopeLine,
            SignalChainSlot.TremoloDepth => EnvelopeLine,
            SignalChainSlot.GapOff => EnvelopeLine,
            SignalChainSlot.GapOn => EnvelopeLine,
            SignalChainSlot.Output => EnvelopeLine,
            _ => EnvelopeLine,
        };

    private static Color WhiteWithOpacity(double opacity) =>
        Color.FromArgb(
            (byte)Math.Round(byte.MaxValue * opacity),
            byte.MaxValue,
            byte.MaxValue,
            byte.MaxValue
        );

    public static double Snap(double v) => Math.Floor(v) + 0.5;

    public static Point Snap(Point point) => new(Snap(point.X), Snap(point.Y));
}
