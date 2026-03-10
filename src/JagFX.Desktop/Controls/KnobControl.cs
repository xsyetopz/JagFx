using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Input;
using Avalonia.Media;

namespace JagFx.Desktop.Controls;

public class KnobControl : Control
{
    public static readonly StyledProperty<double> ValueProperty =
        AvaloniaProperty.Register<KnobControl, double>(nameof(Value), defaultBindingMode: Avalonia.Data.BindingMode.TwoWay);

    public static readonly StyledProperty<double> MinimumProperty =
        AvaloniaProperty.Register<KnobControl, double>(nameof(Minimum), defaultValue: 0.0);

    public static readonly StyledProperty<double> MaximumProperty =
        AvaloniaProperty.Register<KnobControl, double>(nameof(Maximum), defaultValue: 1.0);

    public static readonly StyledProperty<string> LabelProperty =
        AvaloniaProperty.Register<KnobControl, string>(nameof(Label), defaultValue: string.Empty);

    public static readonly StyledProperty<double> StepProperty =
        AvaloniaProperty.Register<KnobControl, double>(nameof(Step), defaultValue: 1.0);

    public static readonly StyledProperty<string> FormatStringProperty =
        AvaloniaProperty.Register<KnobControl, string>(nameof(FormatString), defaultValue: "F0");

    public static readonly StyledProperty<string> UnitProperty =
        AvaloniaProperty.Register<KnobControl, string>(nameof(Unit), defaultValue: string.Empty);

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

    static KnobControl()
    {
        AffectsRender<KnobControl>(ValueProperty, MinimumProperty, MaximumProperty,
            LabelProperty, FormatStringProperty, UnitProperty);
    }

    private Point _lastPointerPos;
    private bool _isDragging;

    private const double StartAngleDeg = 225.0;
    private const double SweepDeg = 270.0;
    private const double LabelHeight = 12.0;
    private const double ValueHeight = 11.0;

    protected override Size MeasureOverride(Size availableSize)
    {
        var w = double.IsInfinity(availableSize.Width) ? 52 : Math.Min(52, availableSize.Width);
        var h = double.IsInfinity(availableSize.Height) ? 60 : Math.Min(60, availableSize.Height);
        return new Size(w, h);
    }

    public override void Render(DrawingContext context)
    {
        var w = Bounds.Width;
        var h = Bounds.Height;
        var cx = w / 2.0;
        var bottomSpace = LabelHeight + ValueHeight;
        var cy = (h - bottomSpace) / 2.0;
        var r = Math.Min(cx - 3, cy - 2);
        if (r < 6) r = 6;

        var dimmed = !IsEffectivelyEnabled;
        var arcBrush = dimmed ? new SolidColorBrush(Color.Parse("#1a3344")) : ThemeColors.AccentBrush;
        var textBrush = dimmed
            ? new SolidColorBrush(Color.Parse("#555555"))
            : new SolidColorBrush(Color.Parse("#f0f0f0"));
        var labelBrush = dimmed
            ? new SolidColorBrush(Color.Parse("#444444"))
            : new SolidColorBrush(Color.Parse("#888888"));

        // Track arc background
        DrawArc(context, cx, cy, r + 2, StartAngleDeg, SweepDeg,
            new Pen(new SolidColorBrush(Color.Parse("#2a2a2a")), 3));

        // Filled arc
        var range = Maximum - Minimum;
        var fraction = range > 0 ? Math.Clamp((Value - Minimum) / range, 0, 1) : 0;
        if (fraction > 0)
        {
            DrawArc(context, cx, cy, r + 2, StartAngleDeg, SweepDeg * fraction,
                new Pen(arcBrush, 3));
        }

        // Knob body
        context.DrawEllipse(
            new SolidColorBrush(Color.Parse("#1e1e1e")),
            new Pen(new SolidColorBrush(Color.Parse("#383838")), 1),
            new Point(cx, cy), r, r);

        // Indicator line
        var indicatorAngleDeg = StartAngleDeg + SweepDeg * fraction;
        var indicatorAngleRad = indicatorAngleDeg * Math.PI / 180.0;
        var lineStartX = cx + Math.Cos(indicatorAngleRad) * (r * 0.35);
        var lineStartY = cy + Math.Sin(indicatorAngleRad) * (r * 0.35);
        var lineEndX = cx + Math.Cos(indicatorAngleRad) * (r * 0.85);
        var lineEndY = cy + Math.Sin(indicatorAngleRad) * (r * 0.85);
        context.DrawLine(new Pen(arcBrush, 2),
            new Point(lineStartX, lineStartY), new Point(lineEndX, lineEndY));

        // Value text (below knob)
        var valueText = Value.ToString(FormatString);
        if (!string.IsNullOrEmpty(Unit))
            valueText += Unit;
        var vtFt = new FormattedText(
            valueText,
            System.Globalization.CultureInfo.CurrentCulture,
            FlowDirection.LeftToRight,
            Typeface.Default,
            9,
            textBrush);
        context.DrawText(vtFt, new Point(cx - vtFt.Width / 2, h - bottomSpace + 1));

        // Label text (bottom)
        var label = Label;
        if (!string.IsNullOrEmpty(label))
        {
            var ft = new FormattedText(
                label,
                System.Globalization.CultureInfo.CurrentCulture,
                FlowDirection.LeftToRight,
                Typeface.Default,
                8,
                labelBrush);
            context.DrawText(ft, new Point(cx - ft.Width / 2, h - LabelHeight + 1));
        }
    }

    private static void DrawArc(DrawingContext context, double cx, double cy, double r,
        double startDeg, double sweepDeg, IPen pen)
    {
        if (sweepDeg <= 0) return;

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
            ctx.ArcTo(new Point(endX, endY), new Size(r, r), 0, isLargeArc, SweepDirection.Clockwise);
        }

        context.DrawGeometry(null, pen, geometry);
    }

    protected override void OnPointerPressed(PointerPressedEventArgs e)
    {
        base.OnPointerPressed(e);
        if (!IsEffectivelyEnabled) return;

        if (e.GetCurrentPoint(this).Properties.IsRightButtonPressed)
        {
            ShowValueFlyout();
            e.Handled = true;
            return;
        }

        if (!e.GetCurrentPoint(this).Properties.IsLeftButtonPressed) return;

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

        var flyout = new Flyout
        {
            Content = nud,
            Placement = PlacementMode.Bottom,
        };

        FlyoutBase.SetAttachedFlyout(this, flyout);
        FlyoutBase.ShowAttachedFlyout(this);
    }

    protected override void OnPointerMoved(PointerEventArgs e)
    {
        base.OnPointerMoved(e);
        if (!_isDragging) return;

        var pos = e.GetPosition(this);
        var deltaY = _lastPointerPos.Y - pos.Y;
        var range = Maximum - Minimum;
        var sensitivity = range / 100.0;
        Value = Math.Clamp(Value + deltaY * sensitivity, Minimum, Maximum);
        _lastPointerPos = pos;
        e.Handled = true;
    }

    protected override void OnPointerReleased(PointerReleasedEventArgs e)
    {
        base.OnPointerReleased(e);
        _isDragging = false;
        e.Pointer.Capture(null);
        e.Handled = true;
    }

    protected override void OnPointerWheelChanged(PointerWheelEventArgs e)
    {
        base.OnPointerWheelChanged(e);
        if (!IsEffectivelyEnabled) return;

        var delta = e.Delta.Y > 0 ? Step : e.Delta.Y < 0 ? -Step : 0;
        if (delta != 0)
        {
            Value = Math.Clamp(Value + delta, Minimum, Maximum);
            e.Handled = true;
        }
    }
}
