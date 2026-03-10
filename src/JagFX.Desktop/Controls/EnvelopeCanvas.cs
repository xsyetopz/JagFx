using System.Collections.Specialized;
using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Media;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls;

public class EnvelopeCanvas : Control
{
    public static readonly StyledProperty<EnvelopeViewModel?> EnvelopeProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, EnvelopeViewModel?>(nameof(Envelope));

    public static readonly StyledProperty<IBrush?> LineColorProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, IBrush?>(nameof(LineColor),
            new SolidColorBrush(ThemeColors.Accent));

    public static readonly StyledProperty<bool> IsSelectedProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, bool>(nameof(IsSelected));

    public static readonly StyledProperty<bool> IsThumbnailProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, bool>(nameof(IsThumbnail));

    public static readonly StyledProperty<int> ZoomLevelProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, int>(nameof(ZoomLevel), 1);

    private int _dragIndex = -1;
    private double _dragMinLevel;
    private double _dragRange;
    private double _dragTotalDuration;
    private EnvelopeViewModel? _subscribedEnvelope;

    public EnvelopeViewModel? Envelope
    {
        get => GetValue(EnvelopeProperty);
        set => SetValue(EnvelopeProperty, value);
    }

    public IBrush? LineColor
    {
        get => GetValue(LineColorProperty);
        set => SetValue(LineColorProperty, value);
    }

    public bool IsSelected
    {
        get => GetValue(IsSelectedProperty);
        set => SetValue(IsSelectedProperty, value);
    }

    public bool IsThumbnail
    {
        get => GetValue(IsThumbnailProperty);
        set => SetValue(IsThumbnailProperty, value);
    }

    public int ZoomLevel
    {
        get => GetValue(ZoomLevelProperty);
        set => SetValue(ZoomLevelProperty, value);
    }

    static EnvelopeCanvas()
    {
        AffectsRender<EnvelopeCanvas>(EnvelopeProperty, LineColorProperty, IsSelectedProperty, IsThumbnailProperty, ZoomLevelProperty);
    }

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property == EnvelopeProperty)
        {
            UnsubscribeEnvelope();
            if (change.NewValue is EnvelopeViewModel env)
                SubscribeEnvelope(env);
        }
    }

    #region Envelope change subscriptions

    private void SubscribeEnvelope(EnvelopeViewModel env)
    {
        _subscribedEnvelope = env;
        env.Segments.CollectionChanged += OnSegmentsCollectionChanged;
        env.PropertyChanged += OnEnvelopeVmChanged;
        foreach (var seg in env.Segments)
            seg.PropertyChanged += OnSegmentChanged;
    }

    private void UnsubscribeEnvelope()
    {
        if (_subscribedEnvelope is null) return;
        _subscribedEnvelope.Segments.CollectionChanged -= OnSegmentsCollectionChanged;
        _subscribedEnvelope.PropertyChanged -= OnEnvelopeVmChanged;
        foreach (var seg in _subscribedEnvelope.Segments)
            seg.PropertyChanged -= OnSegmentChanged;
        _subscribedEnvelope = null;
    }

    private void OnSegmentsCollectionChanged(object? s, NotifyCollectionChangedEventArgs e)
    {
        if (e.NewItems is not null)
            foreach (SegmentViewModel seg in e.NewItems)
                seg.PropertyChanged += OnSegmentChanged;
        if (e.OldItems is not null)
            foreach (SegmentViewModel seg in e.OldItems)
                seg.PropertyChanged -= OnSegmentChanged;
        InvalidateVisual();
    }

    private void OnSegmentChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();
    private void OnEnvelopeVmChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();

    #endregion

    #region Rendering

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        if (IsSelected)
            context.DrawRectangle(null, ThemeColors.AccentPen1, new Rect(0.5, 0.5, w - 1, h - 1));

        using var clip = context.PushClip(new Rect(0, 0, w, h));

        DrawGrid(context, w, h, ZoomLevel);

        var env = Envelope;
        if (env is null || env.Segments.Count == 0) return;

        var geometry = EnvelopeGeometry.Compute(env, w, h, ZoomLevel);
        DrawEnvelope(context, geometry);
    }

    private static void DrawGrid(DrawingContext context, double w, double h, int zoomLevel)
    {
        // Horizontal grid: scale subdivisions with zoom
        // 1x → 4 divisions (25% steps, 3 lines), 2x → 8 (12.5%, 7 lines), 4x → 16 (6.25%, 15 lines)
        var hDivisions = 4 * zoomLevel;
        for (var i = 1; i < hDivisions; i++)
        {
            var y = ThemeColors.Snap(h * i / (double)hDivisions);
            var pen = i == hDivisions / 2 ? ThemeColors.MidPen : ThemeColors.GridFaintPen;
            context.DrawLine(pen, new Point(0, y), new Point(w, y));
        }

        // Vertical grid: scale columns with zoom
        var vDivisions = 8 * zoomLevel;
        for (var i = 1; i < vDivisions; i++)
        {
            var x = ThemeColors.Snap(w * i / (double)vDivisions);
            context.DrawLine(ThemeColors.GridFaintPen, new Point(x, 0), new Point(x, h));
        }
    }

    private void DrawEnvelope(DrawingContext context, EnvelopeGeometry geo)
    {
        var lineBrush = LineColor ?? ThemeColors.AccentBrush;
        var linePen = new Pen(lineBrush, 1.5);
        var points = geo.Points;

        for (var i = 0; i < points.Length - 1; i++)
            context.DrawLine(linePen, points[i], points[i + 1]);

        const double s = 3;
        foreach (var pt in points)
        {
            context.DrawLine(ThemeColors.MarkerPen, new Point(pt.X - s, pt.Y - s), new Point(pt.X + s, pt.Y + s));
            context.DrawLine(ThemeColors.MarkerPen, new Point(pt.X - s, pt.Y + s), new Point(pt.X + s, pt.Y - s));
        }
    }

    #endregion

    #region Pointer interaction

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        if (IsThumbnail) return;

        var env = Envelope;
        if (env is null || env.Segments.Count == 0) return;

        var geo = EnvelopeGeometry.Compute(env, Bounds.Width, Bounds.Height, ZoomLevel);
        _dragIndex = geo.HitTest(e.GetPosition(this));

        if (_dragIndex >= 0)
        {
            _dragMinLevel = Math.Min(env.StartValue, env.Segments.Min(s => s.TargetLevel));
            _dragRange = Math.Max(env.StartValue, env.Segments.Max(s => s.TargetLevel)) - _dragMinLevel;
            if (_dragRange <= 0) _dragRange = 1;
            _dragTotalDuration = env.Segments.Sum(s => s.Duration);
            if (_dragTotalDuration <= 0) _dragTotalDuration = 1;
            e.Pointer.Capture(this);
            e.Handled = true;
        }
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);

        if (_dragIndex < 0) return;

        var env = Envelope;
        if (env is null || _dragIndex >= env.Segments.Count) return;

        var pos = e.GetPosition(this);
        env.Segments[_dragIndex].TargetLevel = EnvelopeGeometry.YToPeakLevel(pos.Y, Bounds.Height, _dragMinLevel, _dragRange);
        EnvelopeGeometry.AdjustDuration(pos.X, Bounds.Width, _dragIndex, env, _dragTotalDuration, ZoomLevel);
        InvalidateVisual();
        e.Handled = true;
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);
        if (_dragIndex >= 0)
        {
            e.Pointer.Capture(null);
            _dragIndex = -1;
            _dragMinLevel = 0;
            _dragRange = 0;
            _dragTotalDuration = 0;
        }
    }

    #endregion
}
