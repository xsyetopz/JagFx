using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Media;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls.Canvases;

public class PoleZeroCanvas : Control
{
    private const double MaximumRawValue = ushort.MaxValue;
    private const double MagnitudeDbScaleFactor = 0.0015258789;
    private const double GainDbScaleFactor = 0.0030517578;
    private const int MagnitudeSnapDivisions = 32;
    private static readonly IPen SelectionPen = new Pen(ThemeColors.AccentBrush, 1).ToImmutable();
    private static readonly IBrush SelectionBrush = new SolidColorBrush(
        Color.FromArgb(80, 0, 158, 115)
    ).ToImmutable();

    public static readonly StyledProperty<FilterViewModel?> FilterProperty =
        AvaloniaProperty.Register<PoleZeroCanvas, FilterViewModel?>(nameof(Filter));

    public static readonly StyledProperty<int> ZoomLevelProperty = AvaloniaProperty.Register<
        PoleZeroCanvas,
        int
    >(nameof(ZoomLevel), 1);

    public static readonly StyledProperty<bool> IsSnapEnabledProperty = AvaloniaProperty.Register<
        PoleZeroCanvas,
        bool
    >(nameof(IsSnapEnabled));

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

    public bool IsSnapEnabled
    {
        get => GetValue(IsSnapEnabledProperty);
        set => SetValue(IsSnapEnabledProperty, value);
    }

    private FilterViewModel? _subscribedFilter;
    private (int Channel, int Phase, int Index)? _dragTarget;
    private (int Channel, int Phase, int Index)? _selectedPoint;
    private bool _isDragging;

    public PoleZeroCanvas()
    {
        UseLayoutRounding = true;
        RenderOptions.SetEdgeMode(this, EdgeMode.Aliased);
    }

    static PoleZeroCanvas()
    {
        AffectsRender<PoleZeroCanvas>(FilterProperty, ZoomLevelProperty, IsSnapEnabledProperty);
    }

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property == FilterProperty)
        {
            UnsubscribeFilter();
            if (change.NewValue is FilterViewModel f)
            {
                SubscribeFilter(f);
            }
        }
    }

    private void SubscribeFilter(FilterViewModel f)
    {
        _subscribedFilter = f;
        f.PropertyChanged += OnFilterChanged;
    }

    private void UnsubscribeFilter()
    {
        if (_subscribedFilter is null)
        {
            return;
        }

        _subscribedFilter.PropertyChanged -= OnFilterChanged;
        _subscribedFilter = null;
    }

    private void OnFilterChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        var filter = Filter;
        if (filter is null || !filter.HasFilter)
        {
            return;
        }

        if (filter.PolePhase.IsDefault || filter.PoleMagnitude.IsDefault)
        {
            return;
        }

        var pos = e.GetPosition(this);
        var plotBounds = PlotBounds;
        if (plotBounds.Width <= 0 || plotBounds.Height <= 0)
        {
            return;
        }

        const double hitThreshold = 10;
        var bestDist = hitThreshold;
        (int Channel, int Phase, int Index)? best = null;

        for (var phase = 0; phase < 2; phase++)
        {
            for (var channel = 0; channel < 2 && channel < filter.PolePhase.Length; channel++)
            {
                var poleCount = channel == 0 ? filter.PoleCount0 : filter.PoleCount1;
                if (filter.PolePhase[channel].IsDefault)
                {
                    continue;
                }

                if (phase >= filter.PolePhase[channel].Length)
                {
                    continue;
                }

                if (filter.PolePhase[channel][phase].IsDefault)
                {
                    continue;
                }

                for (var p = 0; p < poleCount && p < filter.PolePhase[channel][phase].Length; p++)
                {
                    var point = PolePoint(filter, channel, phase, p, plotBounds, ZoomLevel);

                    var dist = Math.Sqrt(
                        (pos.X - point.X) * (pos.X - point.X)
                            + (pos.Y - point.Y) * (pos.Y - point.Y)
                    );
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
            _selectedPoint = best;
            _isDragging = true;
            BeginPreviewEdit();
            InvalidateVisual();
            e.Handled = true;
        }
        else
        {
            _selectedPoint = null;
            InvalidateVisual();
        }
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);
        if (!_isDragging || _dragTarget is null)
        {
            return;
        }

        var filter = Filter;
        if (filter is null)
        {
            return;
        }

        var pos = e.GetPosition(this);
        var plotBounds = PlotBounds;
        if (plotBounds.Width <= 0 || plotBounds.Height <= 0)
        {
            return;
        }

        var newPhase = XToRawPhase(pos.X, plotBounds, ZoomLevel);
        var newMagnitude = YToRawMagnitude(pos.Y, plotBounds);
        if (IsSnapEnabled)
        {
            newPhase = SnapRawPhase(newPhase);
            newMagnitude = SnapRawMagnitude(newMagnitude);
        }

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
            EndPreviewEdit();
            e.Handled = true;
        }
    }

    private Rect PlotBounds => new(0, 0, Bounds.Width, Bounds.Height);

    private void BeginPreviewEdit()
    {
        if (TopLevel.GetTopLevel(this)?.DataContext is MainViewModel vm)
        {
            vm.BeginPreviewEdit();
        }
    }

    private void EndPreviewEdit()
    {
        if (TopLevel.GetTopLevel(this)?.DataContext is MainViewModel vm)
        {
            vm.EndPreviewEdit();
        }
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));
        AnalysisGridRenderer.Draw(
            context,
            new Rect(0, 0, w, h),
            includeVertical: true,
            zoomLevel: ZoomLevel
        );

        var filter = Filter;
        if (filter is null || !filter.HasFilter)
        {
            return;
        }

        if (filter.PolePhase.IsDefault || filter.PoleMagnitude.IsDefault)
        {
            return;
        }

        var plotBounds = PlotBounds;
        DrawDirectionRails(context, filter, plotBounds);
        DrawPhaseVectors(context, filter, plotBounds, ZoomLevel);

        const double s = 3;

        // Draw Phase-1 (dimmed, behind) then Phase-0 (full color, on top)
        for (var phase = 1; phase >= 0; phase--)
        {
            for (var channel = 0; channel < 2 && channel < filter.PolePhase.Length; channel++)
            {
                var poleCount = channel == 0 ? filter.PoleCount0 : filter.PoleCount1;
                if (filter.PolePhase[channel].IsDefault)
                {
                    continue;
                }

                if (phase >= filter.PolePhase[channel].Length)
                {
                    continue;
                }

                if (filter.PolePhase[channel][phase].IsDefault)
                {
                    continue;
                }

                for (var p = 0; p < poleCount && p < filter.PolePhase[channel][phase].Length; p++)
                {
                    var point = PolePoint(filter, channel, phase, p, plotBounds, ZoomLevel);
                    if (_selectedPoint == (channel, phase, p))
                    {
                        context.DrawEllipse(SelectionBrush, SelectionPen, point, 5, 5);
                    }

                    if (channel == 0)
                    {
                        // × marker for direction 0
                        context.DrawLine(
                            ThemeColors.SectionTracePen,
                            new Point(point.X - s, point.Y - s),
                            new Point(point.X + s, point.Y + s)
                        );
                        context.DrawLine(
                            ThemeColors.SectionTracePen,
                            new Point(point.X - s, point.Y + s),
                            new Point(point.X + s, point.Y - s)
                        );
                    }
                    else
                    {
                        // ○ marker for direction 1
                        context.DrawEllipse(null, ThemeColors.AccentPenOnePointFive, point, s, s);
                    }
                }
            }
        }
    }

    private static void DrawDirectionRails(
        DrawingContext context,
        FilterViewModel filter,
        Rect plotBounds
    )
    {
        var y0 = GainToY(filter.UnityGain0, plotBounds);
        var y1 = GainToY(filter.UnityGain1, plotBounds);

        context.DrawLine(
            ThemeColors.PoleAxisAPen,
            new Point(plotBounds.Left, y0),
            new Point(plotBounds.Right, y0)
        );
        context.DrawLine(
            ThemeColors.PoleAxisBPen,
            new Point(plotBounds.Left, y1),
            new Point(plotBounds.Right, y1)
        );
    }

    private static void DrawPhaseVectors(
        DrawingContext context,
        FilterViewModel filter,
        Rect plotBounds,
        int zoomLevel
    )
    {
        for (var channel = 0; channel < 2 && channel < filter.PolePhase.Length; channel++)
        {
            var poleCount = channel == 0 ? filter.PoleCount0 : filter.PoleCount1;
            if (filter.PolePhase[channel].IsDefault || filter.PoleMagnitude[channel].IsDefault)
            {
                continue;
            }

            if (filter.PolePhase[channel].Length < 2 || filter.PoleMagnitude[channel].Length < 2)
            {
                continue;
            }

            if (filter.PolePhase[channel][0].IsDefault || filter.PolePhase[channel][1].IsDefault)
            {
                continue;
            }

            var count = Math.Min(
                poleCount,
                Math.Min(filter.PolePhase[channel][0].Length, filter.PolePhase[channel][1].Length)
            );

            for (var index = 0; index < count; index++)
            {
                var start = PolePoint(filter, channel, 0, index, plotBounds, zoomLevel);
                var end = PolePoint(filter, channel, 1, index, plotBounds, zoomLevel);
                context.DrawLine(ThemeColors.SectionTracePen, start, end);
            }
        }
    }

    private static Point PolePoint(
        FilterViewModel filter,
        int channel,
        int phase,
        int index,
        Rect plotBounds,
        int zoomLevel
    )
    {
        var phaseRaw = filter.PolePhase[channel][phase][index];
        var magnitudeRaw = filter.PoleMagnitude[channel][phase][index];
        return ThemeColors.Snap(
            new Point(
                RawPhaseToX(phaseRaw, plotBounds, zoomLevel),
                RawMagnitudeToY(magnitudeRaw, plotBounds)
            )
        );
    }

    private static double RawPhaseToX(int rawPhase, Rect plotBounds, int zoomLevel)
    {
        var normalized = FilterFrequencyScale.RawPhaseToNormalizedX(rawPhase);
        return plotBounds.Left + plotBounds.Width * Math.Max(1, zoomLevel) * normalized;
    }

    private static int XToRawPhase(double x, Rect plotBounds, int zoomLevel)
    {
        var effectiveWidth = plotBounds.Width * Math.Max(1, zoomLevel);
        var normalized = Math.Clamp((x - plotBounds.Left) / effectiveWidth, 0.0, 1.0);
        return FilterFrequencyScale.NormalizedXToRawPhase(normalized);
    }

    private static double RawMagnitudeToY(int rawMagnitude, Rect plotBounds)
    {
        var magnitudeDb = Math.Clamp(rawMagnitude, 0, ushort.MaxValue) * MagnitudeDbScaleFactor;
        var maxMagnitudeDb = MaximumRawValue * MagnitudeDbScaleFactor;
        var normalized = Math.Clamp(magnitudeDb / maxMagnitudeDb, 0.0, 1.0);
        return NormalizedMagnitudeToY(normalized, plotBounds);
    }

    private static int YToRawMagnitude(double y, Rect plotBounds)
    {
        var normalized = Math.Clamp((plotBounds.Bottom - y) / plotBounds.Height, 0.0, 1.0);
        var maxMagnitudeDb = MaximumRawValue * MagnitudeDbScaleFactor;
        return (int)Math.Round(normalized * maxMagnitudeDb / MagnitudeDbScaleFactor);
    }

    private static int SnapRawPhase(int rawPhase)
    {
        var frequency = FilterFrequencyScale.RawPhaseToFrequency(rawPhase);
        var semitone = Math.Round(
            FilterFrequencyScale.SemitonesPerOctave
                * Math.Log2(frequency / FilterFrequencyScale.BaseFrequencyHz)
        );
        var snappedFrequency = FilterFrequencyScale.SemitoneToFrequency((int)semitone);
        return FilterFrequencyScale.NormalizedXToRawPhase(
            FilterFrequencyScale.FrequencyToNormalizedX(snappedFrequency)
        );
    }

    private static int SnapRawMagnitude(int rawMagnitude)
    {
        var step = ushort.MaxValue / (double)MagnitudeSnapDivisions;
        return Math.Clamp(
            (int)Math.Round(Math.Round(rawMagnitude / step) * step),
            0,
            ushort.MaxValue
        );
    }

    private static double NormalizedMagnitudeToY(double normalized, Rect plotBounds)
    {
        var y = plotBounds.Bottom - plotBounds.Height * Math.Clamp(normalized, 0.0, 1.0);
        return ThemeColors.Snap(Math.Clamp(y, plotBounds.Top + 1, plotBounds.Bottom - 1));
    }

    private static double GainToY(int rawGain, Rect plotBounds)
    {
        var gainDb = Math.Clamp(rawGain, 0, ushort.MaxValue) * GainDbScaleFactor;
        var maxGainDb = MaximumRawValue * GainDbScaleFactor;
        var normalized = Math.Clamp(gainDb / maxGainDb, 0.0, 1.0);
        return NormalizedMagnitudeToY(normalized, plotBounds);
    }
}
