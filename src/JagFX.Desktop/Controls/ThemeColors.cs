using Avalonia.Media;

namespace JagFX.Desktop.Controls;

/// <summary>
/// Centralized canvas drawing colors used across all custom controls.
/// </summary>
public static class ThemeColors
{
    public static readonly Color CanvasBackground = Color.Parse("#0e0e0e");
    public static readonly Color GridLine = Color.Parse("#1a1a1a");
    public static readonly Color MidLine = Color.Parse("#252525");
    public static readonly Color Accent = Color.Parse("#4db8d4");
    public static readonly Color FilterLine = Color.Parse("#8888d4");

    public static readonly IBrush CanvasBackgroundBrush = new SolidColorBrush(CanvasBackground).ToImmutable();
    public static readonly IBrush AccentBrush = new SolidColorBrush(Accent).ToImmutable();
    public static readonly IBrush FilterBrush = new SolidColorBrush(FilterLine).ToImmutable();

    public static readonly IPen GridPen = new Pen(new SolidColorBrush(GridLine).ToImmutable(), 1).ToImmutable();
    public static readonly IPen MidPen = new Pen(new SolidColorBrush(MidLine).ToImmutable(), 1).ToImmutable();
    public static readonly IPen AccentPen2 = new Pen(AccentBrush, 2).ToImmutable();
    public static readonly IPen FilterPen = new Pen(FilterBrush, 1.5).ToImmutable();
    public static readonly IPen WaveformPen = new Pen(AccentBrush, 1).ToImmutable();
}
