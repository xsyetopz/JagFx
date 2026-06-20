using System.Collections.Specialized;
using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Media;
using JagFx.Desktop.ViewModels;

namespace JagFx.Desktop.Controls.Canvases;

public partial class EnvelopeCanvas : Control
{
    public static readonly StyledProperty<EnvelopeViewModel?> EnvelopeProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, EnvelopeViewModel?>(nameof(Envelope));

    public static readonly StyledProperty<IBrush?> LineColorProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        IBrush?
    >(nameof(LineColor), new SolidColorBrush(ThemeColors.Accent));

    public static readonly StyledProperty<bool> IsSelectedProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        bool
    >(nameof(IsSelected));

    public static readonly StyledProperty<bool> IsThumbnailProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        bool
    >(nameof(IsThumbnail));

    public static readonly StyledProperty<int> ZoomLevelProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        int
    >(nameof(ZoomLevel), 1);

    public static readonly StyledProperty<double> ScrollOffsetProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        double
    >(nameof(ScrollOffset));

    public static readonly StyledProperty<bool> IsSnapEnabledProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        bool
    >(nameof(IsSnapEnabled));

    public static readonly StyledProperty<bool> UseAnalysisGridProperty = AvaloniaProperty.Register<
        EnvelopeCanvas,
        bool
    >(nameof(UseAnalysisGrid));

    public static readonly StyledProperty<EnvelopeDisplayMode> DisplayModeProperty =
        AvaloniaProperty.Register<EnvelopeCanvas, EnvelopeDisplayMode>(
            nameof(DisplayMode),
            EnvelopeDisplayMode.FullScale
        );

    private static readonly IPen SelectionPen = new Pen(ThemeColors.AccentBrush, 1).ToImmutable();
    private static readonly IBrush SelectionBrush = new SolidColorBrush(
        Color.FromArgb(80, 0, 158, 115)
    ).ToImmutable();

    private readonly CanvasPanZoomController _interaction = new();

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

    public bool UseAnalysisGrid
    {
        get => GetValue(UseAnalysisGridProperty);
        set => SetValue(UseAnalysisGridProperty, value);
    }

    public EnvelopeDisplayMode DisplayMode
    {
        get => GetValue(DisplayModeProperty);
        set => SetValue(DisplayModeProperty, value);
    }

    public EnvelopeCanvas()
    {
        Focusable = true;
        UseLayoutRounding = true;
        RenderOptions.SetEdgeMode(this, EdgeMode.Aliased);
    }

    static EnvelopeCanvas()
    {
        AffectsRender<EnvelopeCanvas>(
            EnvelopeProperty,
            LineColorProperty,
            IsSelectedProperty,
            IsThumbnailProperty,
            ZoomLevelProperty,
            ScrollOffsetProperty,
            UseAnalysisGridProperty,
            DisplayModeProperty
        );
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
            {
                SubscribeEnvelope(env);
            }
        }
        else if (change.Property == ZoomLevelProperty)
        {
            ScrollOffset = ZoomLevel == 1 ? 0 : Math.Clamp(ScrollOffset, 0, MaxScrollOffset);
        }
    }

    private void SubscribeEnvelope(EnvelopeViewModel env)
    {
        _subscribedEnvelope = env;
        env.Segments.CollectionChanged += OnSegmentsCollectionChanged;
        env.PropertyChanged += OnEnvelopeVmChanged;
        foreach (var seg in env.Segments)
        {
            seg.PropertyChanged += OnSegmentChanged;
        }
    }

    private void UnsubscribeEnvelope()
    {
        if (_subscribedEnvelope is null)
        {
            return;
        }

        _subscribedEnvelope.Segments.CollectionChanged -= OnSegmentsCollectionChanged;
        _subscribedEnvelope.PropertyChanged -= OnEnvelopeVmChanged;
        foreach (var seg in _subscribedEnvelope.Segments)
        {
            seg.PropertyChanged -= OnSegmentChanged;
        }

        _subscribedEnvelope = null;
    }

    private void OnSegmentsCollectionChanged(object? s, NotifyCollectionChangedEventArgs e)
    {
        if (e.NewItems is not null)
        {
            foreach (SegmentViewModel seg in e.NewItems)
            {
                seg.PropertyChanged += OnSegmentChanged;
            }
        }

        if (e.OldItems is not null)
        {
            foreach (SegmentViewModel seg in e.OldItems)
            {
                seg.PropertyChanged -= OnSegmentChanged;
            }
        }

        InvalidateVisual();
    }

    private void OnSegmentChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();

    private void OnEnvelopeVmChanged(object? s, PropertyChangedEventArgs e) => InvalidateVisual();
}
