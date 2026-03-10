using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;

namespace JagFx.Desktop.Controls;

public class WaveformCanvas : Control
{
    public static readonly StyledProperty<float[]?> SamplesProperty =
        AvaloniaProperty.Register<WaveformCanvas, float[]?>(nameof(Samples));

    public static readonly StyledProperty<double> PlaybackPositionProperty =
        AvaloniaProperty.Register<WaveformCanvas, double>(nameof(PlaybackPosition));

    public static readonly StyledProperty<int> ZoomLevelProperty =
        AvaloniaProperty.Register<WaveformCanvas, int>(nameof(ZoomLevel), 1);

    public float[]? Samples
    {
        get => GetValue(SamplesProperty);
        set => SetValue(SamplesProperty, value);
    }

    public double PlaybackPosition
    {
        get => GetValue(PlaybackPositionProperty);
        set => SetValue(PlaybackPositionProperty, value);
    }

    public int ZoomLevel
    {
        get => GetValue(ZoomLevelProperty);
        set => SetValue(ZoomLevelProperty, value);
    }

    static WaveformCanvas()
    {
        AffectsRender<WaveformCanvas>(SamplesProperty, PlaybackPositionProperty, ZoomLevelProperty);
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        // Midline
        var yMid = ThemeColors.Snap(h / 2);
        context.DrawLine(ThemeColors.MidPen, new Point(0, yMid), new Point(w, yMid));

        var samples = Samples;
        if (samples is null || samples.Length == 0) return;

        using var clip = context.PushClip(new Rect(0, 0, w, h));

        var cy = h / 2;
        var scale = h * 0.45;
        var effectiveW = w * ZoomLevel;
        var step = Math.Max(1.0, (double)samples.Length / effectiveW);

        Point? prev = null;
        for (double i = 0; i < samples.Length && i / step < w; i += step)
        {
            var x = i / step;
            var y = cy - samples[(int)i] * scale;
            var pt = new Point(x, Math.Clamp(y, 0, h));

            if (prev.HasValue)
                context.DrawLine(ThemeColors.WaveformPen, prev.Value, pt);

            prev = pt;
        }

        // Playback position marker
        var pos = PlaybackPosition;
        if (pos > 0 && pos <= 1)
        {
            var px = pos * w;
            var markerPen = new Pen(Brushes.White, 1);
            context.DrawLine(markerPen, new Point(px, 0), new Point(px, h));
        }
    }
}
