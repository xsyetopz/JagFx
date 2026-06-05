using Avalonia;
using Avalonia.Controls.Primitives;

namespace JagFx.Desktop.Controls;

public class CollapsibleSection : HeaderedContentControl
{
    public static readonly StyledProperty<bool> IsExpandedProperty = AvaloniaProperty.Register<
        CollapsibleSection,
        bool
    >(nameof(IsExpanded), true);

    public bool IsExpanded
    {
        get => GetValue(IsExpandedProperty);
        set => SetValue(IsExpandedProperty, value);
    }

    protected override void OnApplyTemplate(TemplateAppliedEventArgs e) => base.OnApplyTemplate(e);

    protected override Size MeasureOverride(Size availableSize)
    {
        var headerHeight = 20.0;

        return !IsExpanded
            ? new Size(availableSize.Width, headerHeight)
            : base.MeasureOverride(availableSize);
    }
}
