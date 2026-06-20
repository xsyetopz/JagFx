using Avalonia;
using Avalonia.Input;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls.Canvases;

public partial class EnvelopeCanvas
{
    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        if (IsThumbnail)
        {
            return;
        }

        var env = Envelope;
        if (env is null || env.Segments.Count == 0)
        {
            return;
        }

        var pos = e.GetPosition(this);
        var props = e.GetCurrentPoint(this).Properties;

        if (props.IsRightButtonPressed)
        {
            HandleContextMenu(e, pos, env);
            return;
        }

        var geo = EnvelopeGeometry.Compute(
            env,
            Bounds.Width,
            Bounds.Height,
            ZoomLevel,
            ScrollOffset,
            DisplayMode
        );
        var hitIndex = geo.HitTest(pos);

        if (e.ClickCount == 2 && hitIndex < 0)
        {
            HandlePointInsert(e, pos, geo, env);
            return;
        }

        if (hitIndex >= 0)
        {
            HandleDragStart(e, env, hitIndex);
        }
        else if (ZoomLevel > 1)
        {
            HandlePanStart(e, pos);
        }
        else
        {
            _selectedIndex = -1;
            InvalidateVisual();
        }
    }

    private void HandleContextMenu(PointerPressedEventArgs e, Point pos, EnvelopeViewModel env)
    {
        var geo = EnvelopeGeometry.Compute(
            env,
            Bounds.Width,
            Bounds.Height,
            ZoomLevel,
            ScrollOffset,
            DisplayMode
        );
        ShowContextMenu(pos, geo, env);
        e.Handled = true;
    }

    private void HandlePointInsert(
        PointerPressedEventArgs e,
        Point pos,
        EnvelopeGeometry geo,
        EnvelopeViewModel env
    )
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
        _ = Focus();
        BeginPreviewEdit();

        if (DisplayMode is EnvelopeDisplayMode.FullScale or EnvelopeDisplayMode.Normalized)
        {
            _dragMinLevel = 0;
            _dragRange = 65535;
        }
        else
        {
            _dragMinLevel = Math.Min(env.StartValue, env.Segments.Min(s => s.TargetLevel));
            _dragRange =
                Math.Max(env.StartValue, env.Segments.Max(s => s.TargetLevel)) - _dragMinLevel;
            if (_dragRange <= 0)
            {
                _dragRange = 1;
            }
        }
        _dragTotalDuration = env.Segments.Sum(s => s.Duration);
        if (_dragTotalDuration <= 0)
        {
            _dragTotalDuration = 1;
        }

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
            if (env is null || _dragIndex >= env.Segments.Count)
            {
                return;
            }

            var pos = e.GetPosition(this);
            AutoScrollWhileDragging(pos.X);

            var targetLevel = EnvelopeGeometry.YToPeakLevel(pos.Y, Bounds.Height, env, DisplayMode);
            if (IsSnapEnabled)
            {
                targetLevel = EnvelopeGeometry.SnapLevel(
                    targetLevel,
                    _dragMinLevel,
                    _dragRange,
                    ZoomLevel
                );
            }

            env.Segments[_dragIndex].TargetLevel = targetLevel;
            EnvelopeGeometry.AdjustDuration(
                pos.X,
                Bounds.Width,
                _dragIndex,
                env,
                _dragTotalDuration,
                ZoomLevel,
                ScrollOffset
            );
            if (IsSnapEnabled)
            {
                var preDur = env.Segments[_dragIndex].Duration;
                var snappedDur = EnvelopeGeometry.SnapDuration(
                    preDur,
                    _dragTotalDuration,
                    ZoomLevel
                );
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
                var geo = EnvelopeGeometry.Compute(
                    env,
                    Bounds.Width,
                    Bounds.Height,
                    ZoomLevel,
                    ScrollOffset,
                    DisplayMode
                );

                Cursor =
                    geo.HitTest(pos) >= 0 ? new Cursor(StandardCursorType.Hand)
                    : geo.LineHitTest(pos) >= 0 ? new Cursor(StandardCursorType.Cross)
                    : Cursor.Default;
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
            _dragIndex = -1;
            _dragMinLevel = 0;
            _dragRange = 0;
            _dragTotalDuration = 0;
            EndPreviewEdit();
        }
    }

    private void AutoScrollWhileDragging(double pointerX)
    {
        if (ZoomLevel <= 1)
        {
            return;
        }

        const double edgeWidth = 28;
        const double maxStep = 18;
        var nextOffset = ScrollOffset;
        if (pointerX < edgeWidth)
        {
            nextOffset -= (edgeWidth - pointerX) / edgeWidth * maxStep;
        }
        else if (pointerX > Bounds.Width - edgeWidth)
        {
            nextOffset += (pointerX - (Bounds.Width - edgeWidth)) / edgeWidth * maxStep;
        }

        ScrollOffset = Math.Clamp(nextOffset, 0, MaxScrollOffset);
    }

    protected override void OnPointerWheelChanged(PointerWheelEventArgs e)
    {
        base.OnPointerWheelChanged(e);
        if (IsThumbnail)
        {
            return;
        }

        ZoomLevel = CanvasPanZoomController.StepZoom(ZoomLevel, e.Delta.Y);

        e.Handled = true;
    }

    protected override void OnKeyDown(KeyEventArgs e)
    {
        base.OnKeyDown(e);

        if (e.Key is Key.Delete or Key.Back)
        {
            var env = Envelope;
            if (env is null || _selectedIndex < 0 || _selectedIndex >= env.Segments.Count)
            {
                return;
            }

            if (env.Segments.Count <= 1)
            {
                return;
            }

            env.RemoveSegmentAt(_selectedIndex);
            _selectedIndex = Math.Min(_selectedIndex, env.Segments.Count - 1);
            InvalidateVisual();
            RequestPreviewUpdate(immediate: true);
            e.Handled = true;
        }
    }
}
