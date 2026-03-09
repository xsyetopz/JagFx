using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Controls;

public class PoleZeroCanvas : Control
{
    public static readonly StyledProperty<FilterViewModel?> FilterProperty =
        AvaloniaProperty.Register<PoleZeroCanvas, FilterViewModel?>(nameof(Filter));

    public FilterViewModel? Filter
    {
        get => GetValue(FilterProperty);
        set => SetValue(FilterProperty, value);
    }

    static PoleZeroCanvas()
    {
        AffectsRender<PoleZeroCanvas>(FilterProperty);
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        var cx = w / 2;
        var cy = h / 2;
        var radius = Math.Min(cx, cy) - 8;

        // Unit circle
        context.DrawEllipse(null, ThemeColors.MidPen, new Point(cx, cy), radius, radius);

        // Axes
        context.DrawLine(ThemeColors.GridPen, new Point(cx - radius, cy), new Point(cx + radius, cy));
        context.DrawLine(ThemeColors.GridPen, new Point(cx, cy - radius), new Point(cx, cy + radius));

        var filter = Filter;
        if (filter is null || !filter.HasFilter) return;
        if (filter.PolePhase.IsDefault || filter.PoleMagnitude.IsDefault) return;

        var polePen = ThemeColors.FilterPen;

        for (var channel = 0; channel < 2 && channel < filter.PolePhase.Length; channel++)
        {
            var poleCount = channel == 0 ? filter.PoleCount0 : filter.PoleCount1;
            if (filter.PolePhase[channel].IsDefault) continue;

            for (var p = 0; p < poleCount; p++)
            {
                if (filter.PolePhase[channel][0].IsDefault || p >= filter.PolePhase[channel][0].Length)
                    continue;

                var phase = filter.PolePhase[channel][0][p] / 65536.0 * 2 * Math.PI;
                var magnitude = filter.PoleMagnitude[channel][0][p] / 65536.0;

                var px = cx + Math.Cos(phase) * magnitude * radius;
                var py = cy - Math.Sin(phase) * magnitude * radius;

                // Draw X marker for pole
                const double s = 3;
                context.DrawLine(polePen, new Point(px - s, py - s), new Point(px + s, py + s));
                context.DrawLine(polePen, new Point(px - s, py + s), new Point(px + s, py - s));
            }
        }
    }
}
