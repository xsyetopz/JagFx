using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;

namespace JagFX.Desktop.Controls;

public class WaveformCanvas : Control
{
    public static readonly StyledProperty<float[]?> SamplesProperty =
        AvaloniaProperty.Register<WaveformCanvas, float[]?>(nameof(Samples));

    public float[]? Samples
    {
        get => GetValue(SamplesProperty);
        set => SetValue(SamplesProperty, value);
    }

    static WaveformCanvas()
    {
        AffectsRender<WaveformCanvas>(SamplesProperty);
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        // Midline
        context.DrawLine(ThemeColors.MidPen, new Point(0, h / 2), new Point(w, h / 2));

        // Grid
        context.DrawLine(ThemeColors.GridPen, new Point(0, h * 0.25), new Point(w, h * 0.25));
        context.DrawLine(ThemeColors.GridPen, new Point(0, h * 0.75), new Point(w, h * 0.75));

        var samples = Samples;
        if (samples is null || samples.Length == 0) return;

        var cy = h / 2;
        var scale = h * 0.45;
        var step = Math.Max(1.0, (double)samples.Length / w);

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
    }
}
