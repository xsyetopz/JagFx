using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Media;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Controls;

public class PoleZeroCanvas : Control
{
    public static readonly StyledProperty<FilterViewModel?> FilterProperty =
        AvaloniaProperty.Register<PoleZeroCanvas, FilterViewModel?>(nameof(Filter));

    public static readonly StyledProperty<int> ZoomLevelProperty =
        AvaloniaProperty.Register<PoleZeroCanvas, int>(nameof(ZoomLevel), 1);

    public FilterViewModel? Filter
    {
        get => GetValue(FilterProperty);
        set => SetValue(FilterProperty, value);
    }

    public int ZoomLevel
    {
        get => GetValue(ZoomLevelProperty);
        set => SetValue(ZoomLevelProperty, value);
    }

    private FilterViewModel? _subscribedFilter;
    private (int Channel, int Phase, int Index)? _dragTarget;
    private bool _isDragging;

    static PoleZeroCanvas()
    {
        AffectsRender<PoleZeroCanvas>(FilterProperty, ZoomLevelProperty);
    }

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property == FilterProperty)
        {
            UnsubscribeFilter();
            if (change.NewValue is FilterViewModel f)
                SubscribeFilter(f);
        }
    }

    private void SubscribeFilter(FilterViewModel f)
    {
        _subscribedFilter = f;
        f.PropertyChanged += OnFilterChanged;
    }

    private void UnsubscribeFilter()
    {
        if (_subscribedFilter is null) return;
        _subscribedFilter.PropertyChanged -= OnFilterChanged;
        _subscribedFilter = null;
    }

    private void OnFilterChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        var filter = Filter;
        if (filter is null || !filter.HasFilter) return;
        if (filter.PolePhase.IsDefault || filter.PoleMagnitude.IsDefault) return;

        var pos = e.GetPosition(this);
        var (cx, cy, radius) = GetCircleParams();
        if (radius <= 0) return;

        const double hitThreshold = 10;
        double bestDist = hitThreshold;
        (int Channel, int Phase, int Index)? best = null;

        for (var phase = 0; phase < 2; phase++)
        {
            for (var channel = 0; channel < 2 && channel < filter.PolePhase.Length; channel++)
            {
                var poleCount = channel == 0 ? filter.PoleCount0 : filter.PoleCount1;
                if (filter.PolePhase[channel].IsDefault) continue;
                if (phase >= filter.PolePhase[channel].Length) continue;
                if (filter.PolePhase[channel][phase].IsDefault) continue;

                for (var p = 0; p < poleCount && p < filter.PolePhase[channel][phase].Length; p++)
                {
                    var phaseAngle = filter.PolePhase[channel][phase][p] / 65536.0 * 2 * Math.PI;
                    var magnitude = filter.PoleMagnitude[channel][phase][p] / 65536.0;
                    var px = cx + Math.Cos(phaseAngle) * magnitude * radius;
                    var py = cy - Math.Sin(phaseAngle) * magnitude * radius;

                    var dist = Math.Sqrt((pos.X - px) * (pos.X - px) + (pos.Y - py) * (pos.Y - py));
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        best = (channel, phase, p);
                    }
                }
            }
        }

        if (best.HasValue)
        {
            _dragTarget = best;
            _isDragging = true;
            e.Handled = true;
        }
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);
        if (!_isDragging || _dragTarget is null) return;

        var filter = Filter;
        if (filter is null) return;

        var pos = e.GetPosition(this);
        var (cx, cy, radius) = GetCircleParams();
        if (radius <= 0) return;

        var dx = pos.X - cx;
        var dy = -(pos.Y - cy);
        var magnitude = Math.Sqrt(dx * dx + dy * dy) / radius;
        magnitude = Math.Clamp(magnitude, 0, 1);
        var phaseAngle = Math.Atan2(dy, dx);
        if (phaseAngle < 0) phaseAngle += 2 * Math.PI;

        var newPhase = (int)(phaseAngle / (2 * Math.PI) * 65536);
        var newMagnitude = (int)(magnitude * 65536);
        newPhase = Math.Clamp(newPhase, 0, 65535);
        newMagnitude = Math.Clamp(newMagnitude, 0, 65536);

        var (channel, phase, index) = _dragTarget.Value;
        filter.UpdatePole(channel, phase, index, newPhase, newMagnitude);
        e.Handled = true;
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);
        if (_isDragging)
        {
            _dragTarget = null;
            _isDragging = false;
            e.Handled = true;
        }
    }

    private (double Cx, double Cy, double Radius) GetCircleParams()
    {
        var w = Bounds.Width;
        var h = Bounds.Height;
        var cx = w / 2;
        var cy = h / 2;
        var radius = Math.Min(cx, cy) - 8;
        return (cx, cy, radius);
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
        context.DrawEllipse(null, ThemeColors.GridFaintPen, new Point(cx, cy), radius, radius);

        // Axes
        var snappedCx = ThemeColors.Snap(cx);
        var snappedCy = ThemeColors.Snap(cy);
        context.DrawLine(ThemeColors.GridFaintPen, new Point(cx - radius, snappedCy), new Point(cx + radius, snappedCy));
        context.DrawLine(ThemeColors.GridFaintPen, new Point(snappedCx, cy - radius), new Point(snappedCx, cy + radius));

        var filter = Filter;
        if (filter is null || !filter.HasFilter) return;
        if (filter.PolePhase.IsDefault || filter.PoleMagnitude.IsDefault) return;

        const double s = 3;

        // Draw Phase-1 (dimmed, behind) then Phase-0 (full color, on top)
        for (var phase = 1; phase >= 0; phase--)
        {
            var pen = phase == 0 ? ThemeColors.FilterPen : ThemeColors.DimmedFilterPen;

            for (var channel = 0; channel < 2 && channel < filter.PolePhase.Length; channel++)
            {
                var poleCount = channel == 0 ? filter.PoleCount0 : filter.PoleCount1;
                if (filter.PolePhase[channel].IsDefault) continue;
                if (phase >= filter.PolePhase[channel].Length) continue;
                if (filter.PolePhase[channel][phase].IsDefault) continue;

                for (var p = 0; p < poleCount && p < filter.PolePhase[channel][phase].Length; p++)
                {
                    var phaseAngle = filter.PolePhase[channel][phase][p] / 65536.0 * 2 * Math.PI;
                    var magnitude = filter.PoleMagnitude[channel][phase][p] / 65536.0;
                    var px = cx + Math.Cos(phaseAngle) * magnitude * radius;
                    var py = cy - Math.Sin(phaseAngle) * magnitude * radius;

                    if (phase == 0)
                    {
                        // × marker for Phase-0
                        context.DrawLine(pen, new Point(px - s, py - s), new Point(px + s, py + s));
                        context.DrawLine(pen, new Point(px - s, py + s), new Point(px + s, py - s));
                    }
                    else
                    {
                        // ○ marker for Phase-1
                        context.DrawEllipse(null, pen, new Point(px, py), s, s);
                    }
                }
            }
        }
    }
}
