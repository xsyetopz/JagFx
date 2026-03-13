using Avalonia;
using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Media;

namespace JagFx.Desktop.Controls;

public class WaveformCanvas : Control
{
    public static readonly StyledProperty<float[]?> SamplesProperty =
        AvaloniaProperty.Register<WaveformCanvas, float[]?>(nameof(Samples));

    public static readonly StyledProperty<double> PlaybackPositionProperty =
        AvaloniaProperty.Register<WaveformCanvas, double>(nameof(PlaybackPosition));

    public static readonly StyledProperty<int> ZoomLevelProperty =
        AvaloniaProperty.Register<WaveformCanvas, int>(nameof(ZoomLevel), 1);

    public static readonly StyledProperty<double> ScrollOffsetProperty =
        AvaloniaProperty.Register<WaveformCanvas, double>(nameof(ScrollOffset));

    private static readonly int[] ZoomLevels = [1, 2, 4];

    private bool _isPanning;
    private double _panStartX;
    private double _panStartOffset;

    public float[]? Samples
    {
        get => GetValue(SamplesProperty);
        set => SetValue(SamplesProperty, value);
    }

    public double PlaybackPosition
    {
        get => GetValue(PlaybackPositionProperty);
        set => SetValue(PlaybackPositionProperty, value);
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

    static WaveformCanvas()
    {
        AffectsRender<WaveformCanvas>(SamplesProperty, PlaybackPositionProperty, ZoomLevelProperty, ScrollOffsetProperty);
    }

    private double MaxScrollOffset => Math.Max(0, Bounds.Width * ZoomLevel - Bounds.Width);

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property == ZoomLevelProperty)
        {
            if (ZoomLevel == 1)
                ScrollOffset = 0;
            else
                ScrollOffset = Math.Clamp(ScrollOffset, 0, MaxScrollOffset);
        }
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;

        context.FillRectangle(ThemeColors.CanvasBackgroundBrush, new Rect(0, 0, w, h));

        var samples = Samples;
        if (samples is null || samples.Length == 0) return;

        using var clip = context.PushClip(new Rect(0, 0, w, h));

        var cy = h / 2;
        var scale = h * 0.45;
        var effectiveW = w * ZoomLevel;
        var step = (double)samples.Length / effectiveW;
        var offset = ScrollOffset;

        // Center line
        context.DrawLine(ThemeColors.MidPen, new Point(0, cy), new Point(w, cy));

        // Min/max per column rendering
        var pen = ThemeColors.WaveformPen;
        for (int px = 0; px < (int)w; px++)
        {
            var sampleStart = (int)((px + offset) * step);
            var sampleEnd = (int)((px + 1 + offset) * step);
            sampleEnd = Math.Min(sampleEnd, samples.Length);
            if (sampleStart >= samples.Length) break;
            if (sampleStart < 0) continue;

            float min = float.MaxValue, max = float.MinValue;
            for (int j = sampleStart; j < sampleEnd; j++)
            {
                if (samples[j] < min) min = samples[j];
                if (samples[j] > max) max = samples[j];
            }

            if (min == float.MaxValue) continue;

            var yMin = cy - min * scale;
            var yMax = cy - max * scale;
            context.DrawLine(pen, new Point(px, yMin), new Point(px, yMax));
        }

        // Playback position marker
        var pos = PlaybackPosition;
        if (pos > 0 && pos <= 1)
        {
            var px = pos * effectiveW - offset;
            if (px >= 0 && px <= w)
            {
                var markerPen = new Pen(Brushes.White, 1);
                context.DrawLine(markerPen, new Point(px, 0), new Point(px, h));
            }
        }
    }

    #region Pointer interaction

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        if (ZoomLevel <= 1) return;

        _isPanning = true;
        _panStartX = e.GetPosition(this).X;
        _panStartOffset = ScrollOffset;
        e.Pointer.Capture(this);
        e.Handled = true;
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);
        if (!_isPanning) return;

        var dx = _panStartX - e.GetPosition(this).X;
        ScrollOffset = Math.Clamp(_panStartOffset + dx, 0, MaxScrollOffset);
        e.Handled = true;
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);
        if (_isPanning)
        {
            e.Pointer.Capture(null);
            _isPanning = false;
        }
    }

    protected override void OnPointerWheelChanged(PointerWheelEventArgs e)
    {
        base.OnPointerWheelChanged(e);

        var currentIndex = Array.IndexOf(ZoomLevels, ZoomLevel);
        if (currentIndex < 0) currentIndex = 0;

        if (e.Delta.Y > 0 && currentIndex < ZoomLevels.Length - 1)
            ZoomLevel = ZoomLevels[currentIndex + 1];
        else if (e.Delta.Y < 0 && currentIndex > 0)
            ZoomLevel = ZoomLevels[currentIndex - 1];

        e.Handled = true;
    }

    #endregion
}
