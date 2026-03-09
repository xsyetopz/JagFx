using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Controls;

public class FrequencyResponseCanvas : Control
{
    public static readonly StyledProperty<FilterViewModel?> FilterProperty =
        AvaloniaProperty.Register<FrequencyResponseCanvas, FilterViewModel?>(nameof(Filter));

    public FilterViewModel? Filter
    {
        get => GetValue(FilterProperty);
        set => SetValue(FilterProperty, value);
    }

    static FrequencyResponseCanvas()
    {
        AffectsRender<FrequencyResponseCanvas>(FilterProperty);
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        // Grid
        for (var i = 1; i < 4; i++)
        {
            context.DrawLine(ThemeColors.GridPen, new Point(0, h * i / 4.0), new Point(w, h * i / 4.0));
            context.DrawLine(ThemeColors.GridPen, new Point(w * i / 4.0, 0), new Point(w * i / 4.0, h));
        }

        // Midline at 0 dB
        context.DrawLine(ThemeColors.MidPen, new Point(0, h / 2), new Point(w, h / 2));

        var filter = Filter;
        if (filter is null || !filter.HasFilter) return;

        // Compute and draw frequency response
        var dBValues = FilterResponseCalculator.ComputeMagnitudeResponse(filter, Math.Max((int)w, 50));
        const double dbMin = -40;
        const double dbMax = 20;
        const double pad = 4;

        Point? prev = null;
        for (var i = 0; i < dBValues.Length; i++)
        {
            var x = pad + (w - 2 * pad) * i / (dBValues.Length - 1);
            var normalized = (dBValues[i] - dbMin) / (dbMax - dbMin);
            var y = h - pad - normalized * (h - 2 * pad);
            y = Math.Clamp(y, pad, h - pad);
            var pt = new Point(x, y);
            if (prev.HasValue)
                context.DrawLine(ThemeColors.FilterPen, prev.Value, pt);
            prev = pt;
        }
    }
}
