using System.Collections.Specialized;
using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Media;
using JagFX.Desktop.ViewModels;

namespace JagFX.Desktop.Controls;

public class EnvelopeCanvas : Control
{
    public static readonly StyledProperty<EnvelopeViewModel?> EnvelopeProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, EnvelopeViewModel?>(nameof(Envelope));

    public static readonly StyledProperty<IBrush?> LineColorProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, IBrush?>(nameof(LineColor),
            new SolidColorBrush(ThemeColors.Accent));

    public static readonly StyledProperty<bool> IsSelectedProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, bool>(nameof(IsSelected));

    private int _dragIndex = -1;
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

    static EnvelopeCanvas()
    {
        AffectsRender<EnvelopeCanvas>(EnvelopeProperty, LineColorProperty, IsSelectedProperty);
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
            context.DrawRectangle(null, ThemeColors.AccentPen2, new Rect(1, 1, w - 2, h - 2));

        DrawGrid(context, w, h);

        var env = Envelope;
        if (env is null || env.Segments.Count == 0) return;

        var geometry = EnvelopeGeometry.Compute(env, w, h);
        DrawEnvelope(context, geometry);
    }

    private static void DrawGrid(DrawingContext context, double w, double h)
    {
        var midY = h / 2;
        context.DrawLine(ThemeColors.MidPen, new Point(0, midY), new Point(w, midY));
        context.DrawLine(ThemeColors.GridPen, new Point(0, h * 0.25), new Point(w, h * 0.25));
        context.DrawLine(ThemeColors.GridPen, new Point(0, h * 0.75), new Point(w, h * 0.75));

        for (var i = 1; i < 4; i++)
        {
            var x = w * i / 4.0;
            context.DrawLine(ThemeColors.GridPen, new Point(x, 0), new Point(x, h));
        }
    }

    private void DrawEnvelope(DrawingContext context, EnvelopeGeometry geo)
    {
        var lineBrush = LineColor ?? ThemeColors.AccentBrush;
        var linePen = new Pen(lineBrush, 1.5);
        var points = geo.Points;

        for (var i = 0; i < points.Length - 1; i++)
            context.DrawLine(linePen, points[i], points[i + 1]);

        foreach (var pt in points)
            context.DrawEllipse(ThemeColors.AccentBrush, null, pt, 2.5, 2.5);
    }

    #endregion

    #region Pointer interaction

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);

        var env = Envelope;
        if (env is null || env.Segments.Count == 0) return;

        var geo = EnvelopeGeometry.Compute(env, Bounds.Width, Bounds.Height);
        _dragIndex = geo.HitTest(e.GetPosition(this));

        if (_dragIndex >= 0)
            e.Handled = true;
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);

        if (_dragIndex < 0) return;

        var env = Envelope;
        if (env is null || _dragIndex >= env.Segments.Count) return;

        var pos = e.GetPosition(this);
        env.Segments[_dragIndex].TargetLevel = EnvelopeGeometry.YToPeakLevel(pos.Y, Bounds.Height, env);
        InvalidateVisual();
        e.Handled = true;
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);
        _dragIndex = -1;
    }

    #endregion
}
