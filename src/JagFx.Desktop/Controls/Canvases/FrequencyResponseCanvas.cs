using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls.Canvases;

public class FrequencyResponseCanvas : Control
{
    public static readonly StyledProperty<FilterViewModel?> FilterProperty =
        AvaloniaProperty.Register<FrequencyResponseCanvas, FilterViewModel?>(nameof(Filter));

    public static readonly StyledProperty<int> ZoomLevelProperty = AvaloniaProperty.Register<
        FrequencyResponseCanvas,
        int
    >(nameof(ZoomLevel), 1);

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

    private const double DbMin = -80;
    private const double DbMax = 80;
    private const double Pad = 4;

    static FrequencyResponseCanvas()
    {
        AffectsRender<FrequencyResponseCanvas>(FilterProperty, ZoomLevelProperty);
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
        if (_subscribedFilter is null)
            return;
        _subscribedFilter.PropertyChanged -= OnFilterChanged;
        _subscribedFilter = null;
    }

    private void OnFilterChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;
        if (w < 1 || h < 1)
            return;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        var dbRange = DbMax - DbMin;

        // Vertical lines at frequency subdivisions (log scale, 20Hz–11025Hz)
        double[] freqLines = [50, 100, 200, 500, 1000, 2000, 5000, 10000];
        var logMin = Math.Log10(20);
        var logMax = Math.Log10(11025);
        var logRange = logMax - logMin;
        foreach (var freq in freqLines)
        {
            var x = ThemeColors.Snap((Math.Log10(freq) - logMin) / logRange * (w - 2 * Pad) + Pad);
            context.DrawLine(ThemeColors.GridPen, new Point(x, 0), new Point(x, h));
        }

        // Horizontal lines at 10dB intervals
        double[] dbLines = [-70, -60, -50, -40, -30, -20, -10, 0, 10, 20, 30, 40, 50, 60, 70];
        foreach (var dB in dbLines)
        {
            var y = ThemeColors.Snap(Pad + (1 - (dB - DbMin) / dbRange) * (h - 2 * Pad));
            var pen = dB == 0 ? ThemeColors.GridFaintPen : ThemeColors.GridPen;
            context.DrawLine(pen, new Point(0, y), new Point(w, y));
        }

        var filter = Filter;
        if (filter is null || !filter.HasFilter)
            return;

        var numPoints = Math.Max((int)w, 50);

        var hasPhase1 =
            !filter.PolePhase.IsDefault
            && !filter.PolePhase[0].IsDefault
            && filter.PolePhase[0].Length > 1
            && !filter.PolePhase[0][1].IsDefault;

        // Layer 1 (back): intermediate envelope traces at f=0.25, 0.5, 0.75
        if (hasPhase1)
        {
            double[] intermediateFactors = [0.25, 0.5, 0.75];
            foreach (var factor in intermediateFactors)
                DrawResponseTrace(
                    context,
                    filter,
                    factor,
                    numPoints,
                    w,
                    h,
                    dbRange,
                    ThemeColors.SectionTracePen
                );
        }

        // Layer 2: green combined H(z) at envelope factor 1.0 (phase 1 endpoint)
        if (hasPhase1)
            DrawResponseTrace(
                context,
                filter,
                1.0,
                numPoints,
                w,
                h,
                dbRange,
                ThemeColors.AccentPen1_5
            );

        // Layer 3 (front): yellow combined H(z) at envelope factor 0.0 (phase 0 endpoint)
        DrawResponseTrace(
            context,
            filter,
            0.0,
            numPoints,
            w,
            h,
            dbRange,
            ThemeColors.EnvelopeLinePen
        );
    }

    private static void DrawResponseTrace(
        DrawingContext context,
        FilterViewModel filter,
        double envelopeFactor,
        int numPoints,
        double w,
        double h,
        double dbRange,
        IPen pen
    )
    {
        var dBValues = FilterResponseCalculator.ComputeMagnitudeResponse(
            filter,
            envelopeFactor,
            numPoints
        );
        DrawTrace(context, dBValues, numPoints, w, h, dbRange, pen);
    }

    private static void DrawTrace(
        DrawingContext context,
        double[] dBValues,
        int numPoints,
        double w,
        double h,
        double dbRange,
        IPen pen
    )
    {
        Point? prev = null;
        for (var i = 0; i < dBValues.Length; i++)
        {
            var x = Pad + (w - 2 * Pad) * i / (dBValues.Length - 1);
            var normalized = (dBValues[i] - DbMin) / dbRange;
            var y = h - Pad - normalized * (h - 2 * Pad);
            y = Math.Clamp(y, Pad, Math.Max(Pad, h - Pad));
            var pt = new Point(x, y);
            if (prev.HasValue)
                context.DrawLine(pen, prev.Value, pt);
            prev = pt;
        }
    }
}
