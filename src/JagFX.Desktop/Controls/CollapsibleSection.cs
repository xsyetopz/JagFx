using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Layout;
using Avalonia.Media;

namespace JagFx.Desktop.Controls;

public class CollapsibleSection : HeaderedContentControl
{
    public static readonly StyledProperty<bool> IsExpandedProperty =
        AvaloniaProperty.Register<CollapsibleSection, bool>(nameof(IsExpanded), true);

    public bool IsExpanded
    {
        get => GetValue(IsExpandedProperty);
        set => SetValue(IsExpandedProperty, value);
    }

    protected override void OnApplyTemplate(TemplateAppliedEventArgs e)
    {
        base.OnApplyTemplate(e);
    }

    protected override Size MeasureOverride(Size availableSize)
    {
        var headerHeight = 20.0;

        if (!IsExpanded)
            return new Size(availableSize.Width, headerHeight);

        return base.MeasureOverride(availableSize);
    }
}
