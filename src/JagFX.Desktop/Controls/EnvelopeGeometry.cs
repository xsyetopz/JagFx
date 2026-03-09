using Avalonia;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Controls;

/// <summary>
/// Computes breakpoint positions for an envelope within a given plot area.
/// Eliminates coordinate calculation duplication across rendering and hit testing.
/// </summary>
public readonly struct EnvelopeGeometry
{
    private const double Padding = 4;
    private const double HitRadius = 8;

    public Point[] Points { get; }
    public double PlotWidth { get; }
    public double PlotHeight { get; }

    private EnvelopeGeometry(Point[] points, double plotWidth, double plotHeight)
    {
        Points = points;
        PlotWidth = plotWidth;
        PlotHeight = plotHeight;
    }

    public static EnvelopeGeometry Compute(EnvelopeViewModel env, double canvasWidth, double canvasHeight)
    {
        var segments = env.Segments;
        var plotW = canvasWidth - Padding * 2;
        var plotH = canvasHeight - Padding * 2;

        if (segments.Count == 0)
            return new EnvelopeGeometry([], plotW, plotH);

        var totalDuration = segments.Sum(s => s.Duration);
        if (totalDuration <= 0) totalDuration = 1;

        var maxPeak = segments.Max(s => Math.Abs(s.TargetLevel));
        if (maxPeak <= 0) maxPeak = 1;

        var points = new Point[segments.Count + 1];
        double xAccum = 0;

        // Start point
        var startY = Padding + plotH - (env.StartValue + maxPeak) / (2.0 * maxPeak) * plotH;
        points[0] = new Point(Padding, Math.Clamp(startY, Padding, Padding + plotH));

        for (var i = 0; i < segments.Count; i++)
        {
            xAccum += segments[i].Duration;
            var x = Padding + xAccum / totalDuration * plotW;
            var y = Padding + plotH - (segments[i].TargetLevel + maxPeak) / (2.0 * maxPeak) * plotH;
            points[i + 1] = new Point(x, Math.Clamp(y, Padding, Padding + plotH));
        }

        return new EnvelopeGeometry(points, plotW, plotH);
    }

    /// <summary>
    /// Returns the segment index (0-based) of the breakpoint closest to the given position,
    /// or -1 if no breakpoint is within hit radius. Skips the start point (index 0 in Points).
    /// </summary>
    public int HitTest(Point pos)
    {
        // Breakpoints are at Points[1..], corresponding to segment indices 0..
        for (var i = 1; i < Points.Length; i++)
        {
            var dx = pos.X - Points[i].X;
            var dy = pos.Y - Points[i].Y;
            if (dx * dx + dy * dy <= HitRadius * HitRadius)
                return i - 1;
        }

        return -1;
    }

    /// <summary>
    /// Converts a canvas Y position to a peak level value for the current envelope scaling.
    /// </summary>
    public static int YToPeakLevel(double canvasY, double canvasHeight, EnvelopeViewModel env)
    {
        var plotH = canvasHeight - Padding * 2;
        var maxPeak = env.Segments.Max(s => Math.Abs(s.TargetLevel));
        if (maxPeak <= 0) maxPeak = 1;

        var normalizedY = 1.0 - (canvasY - Padding) / plotH;
        return Math.Clamp((int)(normalizedY * 2 * maxPeak - maxPeak), -65535, 65535);
    }
}
