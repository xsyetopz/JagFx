using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Input;
using Avalonia.Media;

namespace JagFx.Desktop.Controls.Canvases;

public class KnobControl : Control
{
    public static readonly StyledProperty<double> ValueProperty = AvaloniaProperty.Register<
        KnobControl,
        double
    >(nameof(Value), defaultBindingMode: Avalonia.Data.BindingMode.TwoWay);

    public static readonly StyledProperty<double> MinimumProperty = AvaloniaProperty.Register<
        KnobControl,
        double
    >(nameof(Minimum), defaultValue: 0.0);

    public static readonly StyledProperty<double> MaximumProperty = AvaloniaProperty.Register<
        KnobControl,
        double
    >(nameof(Maximum), defaultValue: 1.0);

    public static readonly StyledProperty<string> LabelProperty = AvaloniaProperty.Register<
        KnobControl,
        string
    >(nameof(Label), defaultValue: string.Empty);

    public static readonly StyledProperty<double> StepProperty = AvaloniaProperty.Register<
        KnobControl,
        double
    >(nameof(Step), defaultValue: 1.0);

    public static readonly StyledProperty<string> FormatStringProperty = AvaloniaProperty.Register<
        KnobControl,
        string
    >(nameof(FormatString), defaultValue: "F0");

    public static readonly StyledProperty<string> UnitProperty = AvaloniaProperty.Register<
        KnobControl,
        string
    >(nameof(Unit), defaultValue: string.Empty);

    public static readonly StyledProperty<double> DefaultValueProperty = AvaloniaProperty.Register<
        KnobControl,
        double
    >(nameof(DefaultValue), defaultValue: double.NaN);

    public double Value
    {
        get => GetValue(ValueProperty);
        set => SetValue(ValueProperty, value);
    }

    public double Minimum
    {
        get => GetValue(MinimumProperty);
        set => SetValue(MinimumProperty, value);
    }

    public double Maximum
    {
        get => GetValue(MaximumProperty);
        set => SetValue(MaximumProperty, value);
    }

    public string Label
    {
        get => GetValue(LabelProperty);
        set => SetValue(LabelProperty, value);
    }

    public double Step
    {
        get => GetValue(StepProperty);
        set => SetValue(StepProperty, value);
    }

    public string FormatString
    {
        get => GetValue(FormatStringProperty);
        set => SetValue(FormatStringProperty, value);
    }

    public string Unit
    {
        get => GetValue(UnitProperty);
        set => SetValue(UnitProperty, value);
    }

    public double DefaultValue
    {
        get => GetValue(DefaultValueProperty);
        set => SetValue(DefaultValueProperty, value);
    }

    static KnobControl()
    {
        AffectsRender<KnobControl>(
            ValueProperty,
            MinimumProperty,
            MaximumProperty,
            LabelProperty,
            FormatStringProperty,
            UnitProperty
        );
    }

    protected override void OnPropertyChanged(AvaloniaPropertyChangedEventArgs change)
    {
        base.OnPropertyChanged(change);
        if (change.Property == IsEnabledProperty)
            InvalidateVisual();
    }

    private Point _lastPointerPos;
    private bool _isDragging;

    private const double StartAngleDeg = 135.0;
    private const double SweepDeg = 270.0;

    public static event Action<string>? HintChanged;

    public static void RaiseHint(string hint) => HintChanged?.Invoke(hint);

    protected override Size MeasureOverride(Size availableSize)
    {
        var w = double.IsInfinity(availableSize.Width) ? 44 : Math.Min(44, availableSize.Width);
        var h = double.IsInfinity(availableSize.Height) ? 32 : Math.Min(32, availableSize.Height);
        return new Size(w, h);
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;
        var cx = w / 2.0;
        var cy = h / 2.0;
        var r = Math.Min(cx - 3, cy - 2);
        if (r < 6)
            r = 6;

        var dimmed = !IsEffectivelyEnabled;
        var arcBrush = dimmed
            ? new SolidColorBrush(Color.Parse("#555555"))
            : ThemeColors.AccentBrush;

        // Track arc background
        DrawArc(
            context,
            cx,
            cy,
            r + 2,
            StartAngleDeg,
            SweepDeg,
            new Pen(new SolidColorBrush(Color.Parse("#333333")), 3)
        );

        // Filled arc
        var range = Maximum - Minimum;
        var fraction = range > 0 ? Math.Clamp((Value - Minimum) / range, 0, 1) : 0;
        if (fraction > 0)
        {
            DrawArc(
                context,
                cx,
                cy,
                r + 2,
                StartAngleDeg,
                SweepDeg * fraction,
                new Pen(arcBrush, 3)
            );
        }

        // Knob body
        context.DrawEllipse(
            new SolidColorBrush(Color.Parse("#1a1a1a")),
            new Pen(new SolidColorBrush(Color.Parse("#444444")), 1),
            new Point(cx, cy),
            r,
            r
        );

        // Indicator line
        var indicatorAngleDeg = StartAngleDeg + SweepDeg * fraction;
        var indicatorAngleRad = indicatorAngleDeg * Math.PI / 180.0;
        var lineStartX = cx + Math.Cos(indicatorAngleRad) * (r * 0.35);
        var lineStartY = cy + Math.Sin(indicatorAngleRad) * (r * 0.35);
        var lineEndX = cx + Math.Cos(indicatorAngleRad) * (r * 0.85);
        var lineEndY = cy + Math.Sin(indicatorAngleRad) * (r * 0.85);
        context.DrawLine(
            new Pen(arcBrush, 2),
            new Point(lineStartX, lineStartY),
            new Point(lineEndX, lineEndY)
        );

        // Draw label in the dead zone below the knob arc
        if (!string.IsNullOrEmpty(Label))
        {
            var labelText = new FormattedText(
                Label,
                System.Globalization.CultureInfo.InvariantCulture,
                FlowDirection.LeftToRight,
                new Typeface("Consolas, Monaco, Courier New, monospace"),
                7,
                new SolidColorBrush(Color.Parse("#aaaaaa"))
            );
            var labelX = cx - labelText.Width / 2;
            var labelY = h - labelText.Height;
            context.DrawText(labelText, new Point(labelX, labelY));
        }
    }

    private static void DrawArc(
        DrawingContext context,
        double cx,
        double cy,
        double r,
        double startDeg,
        double sweepDeg,
        IPen pen
    )
    {
        if (sweepDeg <= 0)
            return;

        var startRad = startDeg * Math.PI / 180.0;
        var endRad = (startDeg + sweepDeg) * Math.PI / 180.0;

        var startX = cx + Math.Cos(startRad) * r;
        var startY = cy + Math.Sin(startRad) * r;
        var endX = cx + Math.Cos(endRad) * r;
        var endY = cy + Math.Sin(endRad) * r;

        var isLargeArc = sweepDeg > 180;
        var geometry = new StreamGeometry();
        using (var ctx = geometry.Open())
        {
            ctx.BeginFigure(new Point(startX, startY), false);
            ctx.ArcTo(
                new Point(endX, endY),
                new Size(r, r),
                0,
                isLargeArc,
                SweepDirection.Clockwise
            );
        }

        context.DrawGeometry(null, pen, geometry);
    }

    private string FormatHint() => $"{Label}: {Value.ToString(FormatString)}{Unit}";

    protected override void OnPointerEntered(PointerEventArgs e)
    {
        base.OnPointerEntered(e);
        if (IsEffectivelyEnabled)
            HintChanged?.Invoke(FormatHint());
    }

    protected override void OnPointerExited(PointerEventArgs e)
    {
        base.OnPointerExited(e);
        if (!_isDragging)
            HintChanged?.Invoke("");
    }

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        if (!IsEffectivelyEnabled)
            return;

        if (e.GetCurrentPoint(this).Properties.IsRightButtonPressed)
        {
            ShowValueFlyout();
            e.Handled = true;
            return;
        }

        if (!e.GetCurrentPoint(this).Properties.IsLeftButtonPressed)
            return;

        if (e.ClickCount == 2)
        {
            Value = double.IsNaN(DefaultValue) ? Minimum : DefaultValue;
            HintChanged?.Invoke(FormatHint());
            e.Handled = true;
            return;
        }

        _lastPointerPos = e.GetPosition(this);
        _isDragging = true;
        e.Pointer.Capture(this);
        e.Handled = true;
    }

    private void ShowValueFlyout()
    {
        var nud = new NumericUpDown
        {
            Value = (decimal)Value,
            Minimum = (decimal)Minimum,
            Maximum = (decimal)Maximum,
            Increment = (decimal)Step,
            FormatString = FormatString,
            Width = 100,
            MinHeight = 24,
        };

        nud.ValueChanged += (_, args) =>
        {
            var newVal = args.NewValue ?? 0m;
            Value = Math.Clamp((double)newVal, Minimum, Maximum);
        };

        var flyout = new Flyout { Content = nud, Placement = PlacementMode.Bottom };

        FlyoutBase.SetAttachedFlyout(this, flyout);
        FlyoutBase.ShowAttachedFlyout(this);
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);
        if (!_isDragging)
            return;

        var pos = e.GetPosition(this);
        var deltaY = _lastPointerPos.Y - pos.Y;
        var range = Maximum - Minimum;
        var sensitivity = range / 100.0;
        Value = Math.Clamp(Value + deltaY * sensitivity, Minimum, Maximum);
        _lastPointerPos = pos;
        HintChanged?.Invoke(FormatHint());
        e.Handled = true;
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);
        _isDragging = false;
        e.Pointer.Capture(null);
        HintChanged?.Invoke("");
        e.Handled = true;
    }

    protected override void OnPointerWheelChanged(PointerWheelEventArgs e)
    {
        base.OnPointerWheelChanged(e);
        if (!IsEffectivelyEnabled)
            return;

        var delta =
            e.Delta.Y > 0 ? Step
            : e.Delta.Y < 0 ? -Step
            : 0;
        if (delta != 0)
        {
            Value = Math.Clamp(Value + delta, Minimum, Maximum);
            e.Handled = true;
        }
    }
}
