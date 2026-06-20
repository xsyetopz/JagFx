using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Data;
using Avalonia.Input;
using Avalonia.Media;
using Avalonia.Styling;
using JagFx.Desktop.Controls;
using JagFx.Desktop.Controls.Canvases;
using JagFx.Desktop.ViewModels;
using JagFx.Domain.Models;

namespace JagFx.Desktop.Views.SignalChain;

public partial class SignalChainPanel
{
    private static Control CreateSlotCanvas(SignalChainSlot slot, SlotType slotType, Color color) =>
        slotType switch
        {
            SlotType.Envelope => new EnvelopeCanvas
            {
                IsThumbnail = false,
                LineColor = new SolidColorBrush(color),
                UseAnalysisGrid = slot == SignalChainSlot.Filter,
            },
            SlotType.PoleZero => new PoleZeroCanvas(),
            SlotType.Waveform => new WaveformCanvas(),
            SlotType.Bode => new FrequencyResponseCanvas(),
            _ => throw new ArgumentOutOfRangeException(nameof(slotType), slotType, null),
        };

    private Border WrapInCell(
        Control titleBlock,
        Control canvas,
        int row,
        int col,
        SignalChainSlot slot,
        SlotType slotType
    )
    {
        var innerGrid = new Grid();
        innerGrid.RowDefinitions.Add(new RowDefinition(GridLength.Auto));
        innerGrid.RowDefinitions.Add(new RowDefinition(new GridLength(1, GridUnitType.Star)));

        // Header row: title (Star) + toolbar (Auto)
        var headerGrid = new Grid { ClipToBounds = true };
        headerGrid.ColumnDefinitions.Add(
            new ColumnDefinition(new GridLength(1, GridUnitType.Star))
        );
        headerGrid.ColumnDefinitions.Add(new ColumnDefinition(GridLength.Auto));

        titleBlock.VerticalAlignment = Avalonia.Layout.VerticalAlignment.Center;
        Grid.SetColumn(titleBlock, 0);
        headerGrid.Children.Add(titleBlock);

        var toolbar = CreateToolbar(canvas, slot, slotType);
        toolbar.Margin = new Thickness(0);
        toolbar.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Right;
        Grid.SetColumn(toolbar, 1);
        headerGrid.Children.Add(toolbar);

        Grid.SetRow(headerGrid, 0);
        innerGrid.Children.Add(headerGrid);

        // Canvas row
        Grid.SetRow(canvas, 1);
        innerGrid.Children.Add(canvas);

        var container = new Border
        {
            BorderBrush = ThemeColors.CellBorderBrush,
            BorderThickness = new Thickness(1),
            Padding = new Thickness(3, 2),
            Child = innerGrid,
            Cursor = new Cursor(StandardCursorType.Hand),
        };

        container.PointerPressed += (_, _) => _subscribedVm?.SelectEnvelope(slot);

        Grid.SetRow(container, row);
        Grid.SetColumn(container, col);
        MatrixGrid.Children.Add(container);

        return container;
    }

    private static Border CreateSlotHeader(string slotLabel, string slotTooltip)
    {
        var label = new TextBlock
        {
            Text = slotLabel,
            TextAlignment = Avalonia.Media.TextAlignment.Left,
            TextTrimming = TextTrimming.CharacterEllipsis,
            TextWrapping = Avalonia.Media.TextWrapping.NoWrap,
            Foreground = (IBrush)Application.Current!.FindResource("TextPrimaryBrush")!,
            HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Stretch,
            VerticalAlignment = Avalonia.Layout.VerticalAlignment.Center,
            FontSize = 11,
            FontWeight = FontWeight.SemiBold,
        };
        var panel = new Border
        {
            Padding = new Thickness(5, 2),
            Margin = new Thickness(4, 1, 4, 0),
            HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Stretch,
            VerticalAlignment = Avalonia.Layout.VerticalAlignment.Center,
            Child = label,
        };

        ToolTip.SetTip(panel, slotTooltip);
        ToolTip.SetTip(label, slotTooltip);
        return panel;
    }

    #region Toolbar creation

    private static StackPanel CreateToolbar(Control canvas, SignalChainSlot slot, SlotType slotType)
    {
        var toolbar = new StackPanel
        {
            Orientation = Avalonia.Layout.Orientation.Horizontal,
            Spacing = 1,
            Margin = new Thickness(2, 0, 0, 1),
        };

        // Zoom buttons only for canvases that support zoom
        if (slotType != SlotType.Bode)
        {
            if (slot == SignalChainSlot.Output)
            {
                toolbar.Children.Add(CreateTrueWaveToggle());
            }

            var zoomGroup = CreateZoomGroup(canvas);
            toolbar.Children.Add(zoomGroup);
        }

        // Editable cells get snap toggle
        if (slotType is SlotType.Envelope or SlotType.PoleZero)
        {
            var snapBtn = CreateSnapButton(canvas);
            toolbar.Children.Add(snapBtn);
        }

        return toolbar;
    }

    private static StackPanel CreateZoomGroup(Control canvas)
    {
        var group = new StackPanel
        {
            Orientation = Avalonia.Layout.Orientation.Horizontal,
            Spacing = 0,
        };

        ToggleButton? activeToggle = null;
        int[] levels = [1, 2, 4];

        foreach (var level in levels)
        {
            var btn = new ToggleButton
            {
                Theme = (ControlTheme?)Application.Current!.FindResource("JagCellToggle"),
                IsChecked = level == 1,
                Tag = level,
                Content = $"{level}X",
            };

            if (level == 1)
            {
                activeToggle = btn;
            }

            btn.Click += (s, _) =>
            {
                if (s is not ToggleButton toggled)
                {
                    return;
                }

                // Uncheck siblings
                foreach (var child in group.Children)
                {
                    if (child is ToggleButton tb && tb != toggled)
                    {
                        tb.IsChecked = false;
                    }
                }

                // Ensure at least one is checked
                toggled.IsChecked = true;

                var zoomLevel = (int)(toggled.Tag ?? 1);
                SetCanvasZoom(canvas, zoomLevel);
            };

            ToolTip.SetTip(btn, Loc.Format("TooltipZoom", level));
            group.Children.Add(btn);
        }

        // Sync toggle buttons when zoom changes from wheel input
        var groupRef = group;
        canvas.PropertyChanged += (_, e) =>
        {
            var isZoomProp = canvas switch
            {
                EnvelopeCanvas => e.Property == EnvelopeCanvas.ZoomLevelProperty,
                WaveformCanvas => e.Property == WaveformCanvas.ZoomLevelProperty,
                PoleZeroCanvas => e.Property == PoleZeroCanvas.ZoomLevelProperty,
                _ => false,
            };
            if (!isZoomProp)
            {
                return;
            }

            var newZoom = (int)(e.NewValue ?? 1);
            foreach (var child in groupRef.Children)
            {
                if (child is ToggleButton tb)
                {
                    tb.IsChecked = (int)(tb.Tag ?? 1) == newZoom;
                }
            }
        };

        return group;
    }

    private static ToggleButton CreateTrueWaveToggle()
    {
        var btn = new ToggleButton
        {
            Theme = (ControlTheme?)Application.Current!.FindResource("JagCellToggle"),
        };
        _ = btn.Bind(
            ToggleButton.IsCheckedProperty,
            new Binding(nameof(MainViewModel.TrueWaveEnabled)) { Mode = BindingMode.TwoWay }
        );
        ToolTip.SetTip(btn, Loc.Get("TooltipTrueWave"));
        btn.Content = new OptrisIcon
        {
            Value = "mdi-waveform",
            FontSize = 15,
            HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Center,
            VerticalAlignment = Avalonia.Layout.VerticalAlignment.Center,
        };
        return btn;
    }

    private static void SetCanvasZoom(Control canvas, int zoomLevel)
    {
        switch (canvas)
        {
            case EnvelopeCanvas ec:
                ec.ZoomLevel = zoomLevel;
                break;
            case WaveformCanvas wc:
                wc.ZoomLevel = zoomLevel;
                break;
            case PoleZeroCanvas pzc:
                pzc.ZoomLevel = zoomLevel;
                break;
            default:
                break;
        }
    }

    private static ToggleButton CreateSnapButton(Control canvas)
    {
        var btn = new ToggleButton
        {
            Theme = (ControlTheme?)Application.Current!.FindResource("JagCellToggle"),
            Margin = new Thickness(3, 0, 0, 0),
        };
        btn.Classes.Add("snap");
        ToolTip.SetTip(btn, Loc.Get("TooltipSnapGrid"));
        btn.Content = new OptrisIcon
        {
            Value = "mdi-grid",
            FontSize = 15,
            HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Center,
            VerticalAlignment = Avalonia.Layout.VerticalAlignment.Center,
        };

        btn.Click += (s, _) =>
        {
            if (s is not ToggleButton toggled)
            {
                return;
            }

            switch (canvas)
            {
                case EnvelopeCanvas ec:
                    ec.IsSnapEnabled = toggled.IsChecked == true;
                    break;
                case PoleZeroCanvas pzc:
                    pzc.IsSnapEnabled = toggled.IsChecked == true;
                    break;
                default:
                    break;
            }
        };

        return btn;
    }

    #endregion
}
