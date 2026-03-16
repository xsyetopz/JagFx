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

    public static readonly StyledProperty<double> ScrollOffsetProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, double>(nameof(ScrollOffset));

    public static readonly StyledProperty<bool> IsSnapEnabledProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, bool>(nameof(IsSnapEnabled));

    public static readonly StyledProperty<EnvelopeDisplayMode> DisplayModeProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, EnvelopeDisplayMode>(nameof(DisplayMode), EnvelopeDisplayMode.FullScale);

    private static readonly IPen SelectionPen = new Pen(ThemeColors.AccentBrush, 1.5).ToImmutable();
    private static readonly IBrush SelectionBrush = new SolidColorBrush(Color.FromArgb(80, 0, 158, 115)).ToImmutable();

    private readonly CanvasInteractionHelper _interaction = new();

    private int _dragIndex = -1;
    private double _dragMinLevel;
    private double _dragRange;
    private double _dragTotalDuration;
    private EnvelopeViewModel? _subscribedEnvelope;

    private int _selectedIndex = -1;

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

    public double ScrollOffset
    {
        get => GetValue(ScrollOffsetProperty);
        set => SetValue(ScrollOffsetProperty, value);
    }

    public bool IsSnapEnabled
    {
        get => GetValue(IsSnapEnabledProperty);
        set => SetValue(IsSnapEnabledProperty, value);
    }

    public EnvelopeDisplayMode DisplayMode
    {
        get => GetValue(DisplayModeProperty);
        set => SetValue(DisplayModeProperty, value);
    }

    public EnvelopeCanvas()
    {
        Focusable = true;
    }

    static EnvelopeCanvas()
    {
        AffectsRender<EnvelopeCanvas>(EnvelopeProperty, LineColorProperty, IsSelectedProperty, IsThumbnailProperty, ZoomLevelProperty, ScrollOffsetProperty, DisplayModeProperty);
    }

    private double MaxScrollOffset
    {
        get
        {
            var visibleW = Bounds.Width;
            var plotW = (visibleW - EnvelopeGeometry.Padding * 2) * ZoomLevel;
            return Math.Max(0, plotW - (visibleW - EnvelopeGeometry.Padding * 2));
        }
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
        else if (change.Property == ZoomLevelProperty)
        {
            if (ZoomLevel == 1)
                ScrollOffset = 0;
            else
                ScrollOffset = Math.Clamp(ScrollOffset, 0, MaxScrollOffset);
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

        DrawGrid(context, w, h, ZoomLevel, ScrollOffset);

        var env = Envelope;
        if (env is null || env.Segments.Count == 0) return;

        var geometry = EnvelopeGeometry.Compute(env, w, h, ZoomLevel, ScrollOffset, DisplayMode);
        DrawEnvelope(context, geometry);
    }

    private static void DrawGrid(DrawingContext context, double w, double h, int zoomLevel, double scrollOffset)
    {
        // Horizontal grid: scale subdivisions with zoom
        // 1x -> 4 divisions (25% steps, 3 lines), 2x -> 8 (12.5%, 7 lines), 4x -> 16 (6.25%, 15 lines)
        var hDivisions = 4 * zoomLevel;
        for (var i = 1; i < hDivisions; i++)
        {
            var y = ThemeColors.Snap(h * i / (double)hDivisions);
            var pen = i == hDivisions / 2 ? ThemeColors.MidPen : ThemeColors.GridFaintPen;
            context.DrawLine(pen, new Point(0, y), new Point(w, y));
        }

        // Vertical grid: subdivide visible width, offset by scroll for alignment
        var visibleW = w - EnvelopeGeometry.Padding * 2;
        var vDivisions = 8 * zoomLevel;
        var step = visibleW / vDivisions;
        var offsetMod = scrollOffset % step;
        for (var i = 0; i <= vDivisions; i++)
        {
            var x = ThemeColors.Snap(EnvelopeGeometry.Padding + i * step - offsetMod);
            if (x < EnvelopeGeometry.Padding || x > w - EnvelopeGeometry.Padding) continue;
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
        const double selR = 5;
        for (var i = 0; i < points.Length; i++)
        {
            var pt = points[i];

            // Selection highlight for breakpoints (index i corresponds to segment i-1)
            if (_selectedIndex >= 0 && i == _selectedIndex + 1)
            {
                context.DrawEllipse(SelectionBrush, SelectionPen, pt, selR, selR);
            }

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

        var pos = e.GetPosition(this);
        var props = e.GetCurrentPoint(this).Properties;

        if (props.IsRightButtonPressed)
        {
            HandleContextMenu(e, pos, env);
            return;
        }

        var geo = EnvelopeGeometry.Compute(env, Bounds.Width, Bounds.Height, ZoomLevel, ScrollOffset, DisplayMode);
        var hitIndex = geo.HitTest(pos);

        if (e.ClickCount == 2 && hitIndex < 0)
        {
            HandlePointInsert(e, pos, geo, env);
            return;
        }

        if (hitIndex >= 0)
            HandleDragStart(e, env, hitIndex);
        else if (ZoomLevel > 1)
            HandlePanStart(e, pos);
        else
        {
            _selectedIndex = -1;
            InvalidateVisual();
        }
    }

    private void HandleContextMenu(PointerPressedEventArgs e, Point pos, EnvelopeViewModel env)
    {
        var geo = EnvelopeGeometry.Compute(env, Bounds.Width, Bounds.Height, ZoomLevel, ScrollOffset, DisplayMode);
        ShowContextMenu(pos, geo, env);
        e.Handled = true;
    }

    private void HandlePointInsert(PointerPressedEventArgs e, Point pos, EnvelopeGeometry geo, EnvelopeViewModel env)
    {
        var lineIndex = geo.LineHitTest(pos);
        if (lineIndex >= 0)
        {
            InsertPointOnLine(lineIndex, pos, geo, env);
            e.Handled = true;
        }
    }

    private void HandleDragStart(PointerPressedEventArgs e, EnvelopeViewModel env, int hitIndex)
    {
        _dragIndex = hitIndex;
        _selectedIndex = hitIndex;
        Focus();

        if (DisplayMode is EnvelopeDisplayMode.FullScale or EnvelopeDisplayMode.Normalized)
        {
            _dragMinLevel = 0;
            _dragRange = 65535;
        }
        else
        {
            _dragMinLevel = Math.Min(env.StartValue, env.Segments.Min(s => s.TargetLevel));
            _dragRange = Math.Max(env.StartValue, env.Segments.Max(s => s.TargetLevel)) - _dragMinLevel;
            if (_dragRange <= 0) _dragRange = 1;
        }
        _dragTotalDuration = env.Segments.Sum(s => s.Duration);
        if (_dragTotalDuration <= 0) _dragTotalDuration = 1;
        e.Pointer.Capture(this);
        e.Handled = true;
    }

    private void HandlePanStart(PointerPressedEventArgs e, Point pos)
    {
        _selectedIndex = -1;
        InvalidateVisual();
        _interaction.BeginPan(pos.X, ScrollOffset);
        e.Pointer.Capture(this);
        e.Handled = true;
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);

        if (_interaction.IsPanning)
        {
            ScrollOffset = _interaction.ComputePanOffset(e.GetPosition(this).X, MaxScrollOffset);
            e.Handled = true;
            return;
        }

        if (_dragIndex >= 0)
        {
            var env = Envelope;
            if (env is null || _dragIndex >= env.Segments.Count) return;

            var pos = e.GetPosition(this);
            var level = EnvelopeGeometry.YToPeakLevel(pos.Y, Bounds.Height, _dragMinLevel, _dragRange);
            if (IsSnapEnabled)
                level = EnvelopeGeometry.SnapLevel(level, _dragMinLevel, _dragRange, ZoomLevel);
            env.Segments[_dragIndex].TargetLevel = level;
            EnvelopeGeometry.AdjustDuration(pos.X, Bounds.Width, _dragIndex, env, _dragTotalDuration, ZoomLevel, ScrollOffset);
            if (IsSnapEnabled)
            {
                var preDur = env.Segments[_dragIndex].Duration;
                var snappedDur = EnvelopeGeometry.SnapDuration(preDur, _dragTotalDuration, ZoomLevel);
                if (_dragIndex + 1 < env.Segments.Count)
                {
                    var snapDelta = snappedDur - preDur;
                    var nextDur = env.Segments[_dragIndex + 1].Duration - snapDelta;
                    if (nextDur >= 1)
                    {
                        env.Segments[_dragIndex].Duration = snappedDur;
                        env.Segments[_dragIndex + 1].Duration = nextDur;
                    }
                }
                else
                {
                    env.Segments[_dragIndex].Duration = snappedDur;
                }
            }
            var seg = env.Segments[_dragIndex];
            KnobControl.RaiseHint($"Segment {_dragIndex + 1}: Level={seg.TargetLevel}, Duration={seg.Duration}");
            InvalidateVisual();
            e.Handled = true;
            return;
        }

        // Cursor feedback when not dragging or panning
        if (!IsThumbnail)
        {
            var env = Envelope;
            if (env is not null && env.Segments.Count > 0)
            {
                var pos = e.GetPosition(this);
                var geo = EnvelopeGeometry.Compute(env, Bounds.Width, Bounds.Height, ZoomLevel, ScrollOffset, DisplayMode);

                if (geo.HitTest(pos) >= 0)
                    Cursor = new Cursor(StandardCursorType.Hand);
                else if (geo.LineHitTest(pos) >= 0)
                    Cursor = new Cursor(StandardCursorType.Cross);
                else
                    Cursor = Cursor.Default;
            }
            else
            {
                Cursor = Cursor.Default;
            }
        }
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);

        if (_interaction.IsPanning)
        {
            e.Pointer.Capture(null);
            _interaction.EndPan();
            return;
        }

        if (_dragIndex >= 0)
        {
            e.Pointer.Capture(null);
            KnobControl.RaiseHint("");
            _dragIndex = -1;
            _dragMinLevel = 0;
            _dragRange = 0;
            _dragTotalDuration = 0;
        }
    }

    protected override void OnPointerWheelChanged(PointerWheelEventArgs e)
    {
        base.OnPointerWheelChanged(e);
        if (IsThumbnail) return;

        ZoomLevel = _interaction.StepZoom(ZoomLevel, e.Delta.Y);

        e.Handled = true;
    }

    #endregion

    #region Keyboard interaction

    protected override void OnKeyDown(KeyEventArgs e)
    {
        base.OnKeyDown(e);

        if (e.Key is Key.Delete or Key.Back)
        {
            var env = Envelope;
            if (env is null || _selectedIndex < 0 || _selectedIndex >= env.Segments.Count) return;
            if (env.Segments.Count <= 1) return;

            env.RemoveSegmentAt(_selectedIndex);
            _selectedIndex = Math.Min(_selectedIndex, env.Segments.Count - 1);
            InvalidateVisual();
            e.Handled = true;
        }
    }

    #endregion

    #region Context menu

    private void ShowContextMenu(Point pos, EnvelopeGeometry geo, EnvelopeViewModel env)
    {
        var menu = new MenuFlyout();

        var pointIndex = geo.HitTest(pos);
        if (pointIndex >= 0)
        {
            _selectedIndex = pointIndex;
            InvalidateVisual();

            if (env.Segments.Count > 1)
            {
                var deleteItem = new MenuItem { Header = "Delete Point" };
                var idx = pointIndex;
                deleteItem.Click += (_, _) =>
                {
                    env.RemoveSegmentAt(idx);
                    _selectedIndex = -1;
                    InvalidateVisual();
                };
                menu.Items.Add(deleteItem);
            }
        }
        else
        {
            var lineIndex = geo.LineHitTest(pos);
            if (lineIndex >= 0)
            {
                var addItem = new MenuItem { Header = "Add Point Here" };
                var li = lineIndex;
                addItem.Click += (_, _) =>
                {
                    InsertPointOnLine(li, pos, geo, env);
                };
                menu.Items.Add(addItem);
            }
            else
            {
                var addEndItem = new MenuItem { Header = "Add Point at End" };
                addEndItem.Click += (_, _) =>
                {
                    env.AddSegment(100, 0);
                    _selectedIndex = env.Segments.Count - 1;
                    InvalidateVisual();
                };
                menu.Items.Add(addEndItem);
            }
        }

        if (menu.Items.Count > 0)
            menu.ShowAt(this, true);
    }

    #endregion

    #region Point insertion

    private void InsertPointOnLine(int lineIndex, Point pos, EnvelopeGeometry geo, EnvelopeViewModel env)
    {
        var totalDuration = env.Segments.Sum(s => s.Duration);
        if (totalDuration <= 0) return;

        var clickTime = geo.XToTime(pos.X, totalDuration, ScrollOffset);

        // Calculate cumulative time up to the start of the segment at lineIndex
        double segStartTime = 0;
        for (var i = 0; i < lineIndex; i++)
            segStartTime += env.Segments[i].Duration;

        var seg = env.Segments[lineIndex];
        var segEndTime = segStartTime + seg.Duration;

        // Clamp click time within the segment
        clickTime = Math.Clamp(clickTime, segStartTime + 1, segEndTime - 1);

        var firstDuration = (int)Math.Max(1, clickTime - segStartTime);
        var secondDuration = Math.Max(1, seg.Duration - firstDuration);

        // Interpolate level between the previous point and this segment's target
        var prevLevel = lineIndex == 0 ? env.StartValue : env.Segments[lineIndex - 1].TargetLevel;
        var t = (double)firstDuration / seg.Duration;
        var interpolatedLevel = (int)(prevLevel + t * (seg.TargetLevel - prevLevel));

        // Shorten existing segment to the first portion
        env.Segments[lineIndex].Duration = firstDuration;

        // Insert new segment with remainder duration and original target level
        env.InsertSegment(lineIndex + 1, secondDuration, seg.TargetLevel);

        // Set the shortened segment's target to the interpolated level
        env.Segments[lineIndex].TargetLevel = interpolatedLevel;

        _selectedIndex = lineIndex;
        InvalidateVisual();
    }

    #endregion
}
