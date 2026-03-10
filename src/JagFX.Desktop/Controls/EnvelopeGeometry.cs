using Avalonia;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls;

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

    public static EnvelopeGeometry Compute(EnvelopeViewModel env, double canvasWidth, double canvasHeight, int zoomLevel = 1)
    {
        var segments = env.Segments;
        var plotW = (canvasWidth - Padding * 2) * zoomLevel;
        var plotH = canvasHeight - Padding * 2;

        if (segments.Count == 0)
            return new EnvelopeGeometry([], plotW, plotH);

        var totalDuration = segments.Sum(s => s.Duration);
        if (totalDuration <= 0) totalDuration = 1;

        var minLevel = Math.Min(env.StartValue, segments.Min(s => s.TargetLevel));
        var maxLevel = Math.Max(env.StartValue, segments.Max(s => s.TargetLevel));
        var range = (double)(maxLevel - minLevel);
        if (range <= 0) range = 1;

        var points = new Point[segments.Count + 1];
        double xAccum = 0;

        // Start point -- minLevel maps to bottom, maxLevel maps to top
        var startY = Padding + plotH - ((env.StartValue - minLevel) / range) * plotH;
        points[0] = new Point(Padding, Math.Clamp(startY, Padding, Padding + plotH));

        for (var i = 0; i < segments.Count; i++)
        {
            xAccum += segments[i].Duration;
            var x = Padding + xAccum / totalDuration * plotW;
            var y = Padding + plotH - ((segments[i].TargetLevel - minLevel) / range) * plotH;
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
    /// Adjusts the duration of the segment at segmentIndex based on the canvas X position,
    /// compensating the next segment to keep total duration constant.
    /// </summary>
    public static void AdjustDuration(double canvasX, double canvasWidth, int segmentIndex,
        EnvelopeViewModel env, double totalDuration, int zoomLevel = 1)
    {
        var plotW = (canvasWidth - Padding * 2) * zoomLevel;
        if (plotW <= 0 || totalDuration <= 0) return;

        var segments = env.Segments;

        // Cumulative time up to the segment before the dragged one
        double prevTime = 0;
        for (var i = 0; i < segmentIndex; i++)
            prevTime += segments[i].Duration;

        // Convert X position to time
        var newTime = (canvasX - Padding) / plotW * totalDuration;

        // New duration = time at this point - time at previous point
        var newDur = (int)Math.Clamp(newTime - prevTime, 1, totalDuration);

        var oldDur = segments[segmentIndex].Duration;
        var delta = newDur - oldDur;

        if (segmentIndex + 1 < segments.Count)
        {
            var nextDur = segments[segmentIndex + 1].Duration - delta;
            if (nextDur < 1) return;
            segments[segmentIndex].Duration = newDur;
            segments[segmentIndex + 1].Duration = nextDur;
        }
        else
        {
            segments[segmentIndex].Duration = newDur;
        }
    }

    /// <summary>
    /// Converts a canvas Y position to a peak level value for the current envelope scaling.
    /// </summary>
    public static int YToPeakLevel(double canvasY, double canvasHeight, EnvelopeViewModel env)
    {
        var minLevel = Math.Min(env.StartValue, env.Segments.Min(s => s.TargetLevel));
        var maxLevel = Math.Max(env.StartValue, env.Segments.Max(s => s.TargetLevel));
        var range = (double)(maxLevel - minLevel);
        if (range <= 0) range = 1;
        return YToPeakLevel(canvasY, canvasHeight, minLevel, range);
    }

    /// <summary>
    /// Converts a canvas Y position to a peak level using pre-computed min/range.
    /// Use during drag operations to avoid feedback loops from changing range.
    /// </summary>
    public static int YToPeakLevel(double canvasY, double canvasHeight, double minLevel, double range)
    {
        if (range <= 0) range = 1;
        var plotH = canvasHeight - Padding * 2;
        var normalizedY = 1.0 - (canvasY - Padding) / plotH;
        return Math.Clamp((int)(normalizedY * range + minLevel), -65535, 65535);
    }
}
