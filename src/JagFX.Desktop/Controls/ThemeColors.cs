using Avalonia.Media;

namespace JagFx.Desktop.Controls;

/// <summary>
/// Centralized canvas drawing colors used across all custom controls.
/// </summary>
public static class ThemeColors
{
    public static readonly Color CanvasBackground = Color.Parse("#101010");
    public static readonly Color GridLineFaint = Color.Parse("#2a2a2a");
    public static readonly Color GridLine = Color.Parse("#1a1a1a");
    public static readonly Color MidLine = Color.Parse("#cccccc");
    public static readonly Color Accent = Color.Parse("#009E73");
    public static readonly Color FilterLine = Color.Parse("#0072B2");

    public static readonly IBrush CanvasBackgroundBrush = new SolidColorBrush(CanvasBackground).ToImmutable();
    public static readonly IBrush AccentBrush = new SolidColorBrush(Accent).ToImmutable();
    public static readonly IBrush FilterBrush = new SolidColorBrush(FilterLine).ToImmutable();

    public static readonly IPen GridFaintPen = new Pen(new SolidColorBrush(GridLineFaint).ToImmutable(), 1).ToImmutable();
    public static readonly IPen GridPen = new Pen(new SolidColorBrush(GridLine).ToImmutable(), 1).ToImmutable();
    public static readonly IPen MidPen = new Pen(new SolidColorBrush(MidLine).ToImmutable(), 1).ToImmutable();
    public static readonly IPen AccentPen2 = new Pen(AccentBrush, 2).ToImmutable();
    public static readonly IPen AccentPen1 = new Pen(AccentBrush, 1).ToImmutable();
    public static readonly IPen FilterPen = new Pen(FilterBrush, 1.5).ToImmutable();
    public static readonly IPen DimmedFilterPen = new Pen(new SolidColorBrush(Color.FromArgb(128, 0, 114, 178)).ToImmutable(), 1.5).ToImmutable();
    public static readonly IPen WaveformPen = new Pen(AccentBrush, 1).ToImmutable();
    public static readonly IPen MarkerPen = new Pen(Brushes.White, 1).ToImmutable();

    public static double Snap(double v) => Math.Floor(v) + 0.5;
}
