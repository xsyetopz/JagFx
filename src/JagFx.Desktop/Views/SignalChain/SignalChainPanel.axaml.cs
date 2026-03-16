using System.ComponentModel;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.Primitives;
using Avalonia.Input;
using Avalonia.Media;
using Avalonia.Styling;
using JagFx.Desktop.Controls;
using JagFx.Desktop.ViewModels;
using JagFx.Domain.Models;

namespace JagFx.Desktop.Views.SignalChain;

public partial class SignalChainPanel : UserControl
{
    private MainViewModel? _subscribedVm;
    private PatchViewModel? _subscribedPatch;

    private abstract record SlotBase(SignalChainSlot Slot, Border Container);
    private sealed record EnvelopeSlot(SignalChainSlot Slot, Border Container, EnvelopeCanvas Canvas) : SlotBase(Slot, Container);
    private sealed record SpecialSlot(SignalChainSlot Slot, Border Container, Control Canvas) : SlotBase(Slot, Container);

    private readonly List<SlotBase> _slots = [];

    // 3×4 matrix layout: [row][col] = (slot, type)
    private static readonly (SignalChainSlot Slot, SlotType Type)[,] Matrix =
    {
        { (SignalChainSlot.Pitch, SlotType.Envelope), (SignalChainSlot.VibratoRate, SlotType.Envelope), (SignalChainSlot.VibratoDepth, SlotType.Envelope), (SignalChainSlot.PoleZero, SlotType.PoleZero) },
        { (SignalChainSlot.Volume, SlotType.Envelope), (SignalChainSlot.TremoloRate, SlotType.Envelope), (SignalChainSlot.TremoloDepth, SlotType.Envelope), (SignalChainSlot.Filter, SlotType.Envelope) },
        { (SignalChainSlot.GapOff, SlotType.Envelope), (SignalChainSlot.GapOn, SlotType.Envelope), (SignalChainSlot.Output, SlotType.Waveform), (SignalChainSlot.Bode, SlotType.Bode) },
    };

    private enum SlotType { Envelope, PoleZero, Waveform, Bode }

    private WaveformCanvas? _outCanvas;
    private PoleZeroCanvas? _pzCanvas;
    private FrequencyResponseCanvas? _bodeCanvas;

    public SignalChainPanel()
    {
        InitializeComponent();
        BuildMatrix();
        DataContextChanged += OnDataContextChanged;
    }

    private void BuildMatrix()
    {
        for (var row = 0; row < 3; row++)
        {
            for (var col = 0; col < 4; col++)
            {
                var (slot, slotType) = Matrix[row, col];
                var color = ThemeColors.SlotColor(slot);

                var titleBlock = new TextBlock
                {
                    Text = slot.DisplayName(),
                    FontSize = 10,
                    FontWeight = FontWeight.Bold,
                    Foreground = new SolidColorBrush(Color.Parse("#f0f0f0")),
                    Opacity = 0.7,
                    Margin = new Thickness(4, 1, 0, 0),
                    TextTrimming = TextTrimming.CharacterEllipsis,
                };

                var canvas = CreateSlotCanvas(slotType, color);
                if (canvas is null) continue;

                switch (canvas)
                {
                    case PoleZeroCanvas pzc: _pzCanvas = pzc; break;
                    case WaveformCanvas wc: _outCanvas = wc; break;
                    case FrequencyResponseCanvas frc: _bodeCanvas = frc; break;
                }

                var container = WrapInCell(titleBlock, canvas, row, col, slot, slotType);
                SlotBase slotRecord = canvas is EnvelopeCanvas ec
                    ? new EnvelopeSlot(slot, container, ec)
                    : new SpecialSlot(slot, container, canvas);

                _slots.Add(slotRecord);
            }
        }
    }

    private Control? CreateSlotCanvas(SlotType slotType, string color) => slotType switch
    {
        SlotType.Envelope => new EnvelopeCanvas
        {
            IsThumbnail = false,
            LineColor = new SolidColorBrush(Color.Parse(color)),
        },
        SlotType.PoleZero => new PoleZeroCanvas(),
        SlotType.Waveform => new WaveformCanvas(),
        SlotType.Bode => new FrequencyResponseCanvas(),
        _ => null,
    };

    private Border WrapInCell(TextBlock titleBlock, Control canvas, int row, int col, SignalChainSlot slot, SlotType slotType)
    {
        var innerGrid = new Grid();
        innerGrid.RowDefinitions.Add(new RowDefinition(GridLength.Auto));
        innerGrid.RowDefinitions.Add(new RowDefinition(new GridLength(1, GridUnitType.Star)));

        // Header row: title (Star) + toolbar (Auto)
        var headerGrid = new Grid { ClipToBounds = true };
        headerGrid.ColumnDefinitions.Add(new ColumnDefinition(new GridLength(1, GridUnitType.Star)));
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
            BorderBrush = SolidColorBrush.Parse("#4a4a4a"),
            BorderThickness = new Thickness(1),
            Padding = new Thickness(2, 1),
            Child = innerGrid,
            Cursor = new Cursor(StandardCursorType.Hand),
        };

        container.PointerPressed += (_, _) =>
        {
            if (_subscribedVm is not null)
                _subscribedVm.SelectEnvelope(slot);
        };

        Grid.SetRow(container, row);
        Grid.SetColumn(container, col);
        MatrixGrid.Children.Add(container);

        return container;
    }

    #region Toolbar creation

    private StackPanel CreateToolbar(Control canvas, SignalChainSlot slot, SlotType slotType)
    {
        var toolbar = new StackPanel
        {
            Orientation = Avalonia.Layout.Orientation.Horizontal,
            Spacing = 1,
            Margin = new Thickness(2, 0, 0, 1),
        };

        // Zoom buttons only for canvases that support zoom
        if (slotType != SlotType.PoleZero && slotType != SlotType.Bode)
        {
            var zoomGroup = CreateZoomGroup(canvas);
            toolbar.Children.Add(zoomGroup);
        }

        // Envelope cells get [S] snap button
        if (slotType == SlotType.Envelope)
        {
            var snapBtn = CreateSnapButton(canvas);
            toolbar.Children.Add(snapBtn);
        }

        // All except P/Z and BODE get MODE ▾ dropdown
        if (slotType != SlotType.PoleZero && slotType != SlotType.Bode)
        {
            var modeBtn = CreateModeDropdown(canvas, slot, slotType);
            toolbar.Children.Add(modeBtn);
        }

        return toolbar;
    }

    private StackPanel CreateZoomGroup(Control canvas)
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
                Content = $"{level}x",
                Theme = (ControlTheme?)Application.Current!.FindResource("JagCellToggle"),
                IsChecked = level == 1,
                Tag = level,
            };

            if (level == 1) activeToggle = btn;

            btn.Click += (s, _) =>
            {
                if (s is not ToggleButton toggled) return;

                // Uncheck siblings
                foreach (var child in group.Children)
                {
                    if (child is ToggleButton tb && tb != toggled)
                        tb.IsChecked = false;
                }

                // Ensure at least one is checked
                toggled.IsChecked = true;

                var zoomLevel = (int)(toggled.Tag ?? 1);
                SetCanvasZoom(canvas, zoomLevel);
            };

            ToolTip.SetTip(btn, $"Zoom {level}x");
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
                _ => false,
            };
            if (!isZoomProp) return;

            var newZoom = (int)(e.NewValue ?? 1);
            foreach (var child in groupRef.Children)
            {
                if (child is ToggleButton tb)
                    tb.IsChecked = (int)(tb.Tag ?? 1) == newZoom;
            }
        };

        return group;
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
        }
    }

    private static ToggleButton CreateSnapButton(Control canvas)
    {
        var btn = new ToggleButton
        {
            Content = "S",
            Theme = (ControlTheme?)Application.Current!.FindResource("JagCellToggle"),
            Margin = new Thickness(3, 0, 0, 0),
        };
        btn.Classes.Add("snap");
        ToolTip.SetTip(btn, "Snap to grid (S)");

        btn.Click += (s, _) =>
        {
            if (s is not ToggleButton toggled) return;
            if (canvas is EnvelopeCanvas ec)
                ec.IsSnapEnabled = toggled.IsChecked == true;
        };

        return btn;
    }

    private Button CreateModeDropdown(Control canvas, SignalChainSlot slot, SlotType slotType)
    {
        var btn = new Button
        {
            Content = "MODE \u25BE",
            Theme = (ControlTheme?)Application.Current!.FindResource("JagCellButton"),
            Margin = new Thickness(3, 0, 0, 0),
        };
        ToolTip.SetTip(btn, "Display mode");

        btn.Click += (s, _) =>
        {
            if (s is not Button button) return;

            var menu = new ContextMenu();

            if (slotType == SlotType.Envelope && canvas is EnvelopeCanvas ec)
            {
                var modes = new (string Label, EnvelopeDisplayMode Mode)[]
                {
                    ("Auto", EnvelopeDisplayMode.AutoScale),
                    ("Full", EnvelopeDisplayMode.FullScale),
                    ("Norm", EnvelopeDisplayMode.Normalized),
                };

                var ecRef = ec;
                foreach (var (label, modeValue) in modes)
                {
                    var item = new MenuItem { Header = label };
                    var mode = modeValue;
                    item.Click += (_, _) => ecRef.DisplayMode = mode;
                    menu.Items.Add(item);
                }

                menu.Opening += (_, _) =>
                {
                    for (var i = 0; i < menu.Items.Count; i++)
                    {
                        if (menu.Items[i] is MenuItem mi)
                        {
                            mi.Icon = ecRef.DisplayMode == modes[i].Mode
                                ? new TextBlock { Text = "\u2713", FontSize = 9 }
                                : null;
                        }
                    }
                };
            }
            else if (slotType == SlotType.Waveform)
            {
                var waveformModes = new[] { "Wave", "Spectrum" };
                foreach (var mode in waveformModes)
                {
                    var item = new MenuItem { Header = mode, IsEnabled = mode == "Wave" };
                    menu.Items.Add(item);
                }
            }

            menu.Open(button);
        };

        return btn;
    }

    private EnvelopeViewModel? FindEnvelopeForSlot(SignalChainSlot slot)
    {
        if (_subscribedVm is null) return null;
        var voice = _subscribedVm.Patch.SelectedVoice;
        var entry = MainViewModel.SignalChain.FirstOrDefault(e => e.Slot == slot);
        return entry.Getter?.Invoke(voice);
    }

    #endregion


    private void OnDataContextChanged(object? sender, EventArgs e)
    {
        if (_subscribedVm is not null)
        {
            _subscribedVm.PropertyChanged -= OnVmPropertyChanged;
            UnsubscribePatch();
            UnsubscribeEnvelopeChanges();
        }

        if (DataContext is MainViewModel vm)
        {
            _subscribedVm = vm;
            vm.PropertyChanged += OnVmPropertyChanged;
            SubscribePatch(vm);
            BindAll(vm);
            UpdateSelection(vm.SelectedSlot);
            UpdateGridMode(vm.GridMode);
        }
        else
        {
            _subscribedVm = null;
        }
    }

    private void SubscribePatch(MainViewModel vm)
    {
        _subscribedPatch = vm.Patch;
        _subscribedPatch.PropertyChanged += OnPatchPropertyChanged;
    }

    private void UnsubscribePatch()
    {
        if (_subscribedPatch is not null)
        {
            _subscribedPatch.PropertyChanged -= OnPatchPropertyChanged;
            _subscribedPatch = null;
        }
    }

    private void OnPatchPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(PatchViewModel.SelectedVoice) && _subscribedVm is not null)
            BindAll(_subscribedVm);
    }

    private void OnVmPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (_subscribedVm is null) return;

        if (e.PropertyName == nameof(MainViewModel.SelectedSlot))
            UpdateSelection(_subscribedVm.SelectedSlot);
        if (e.PropertyName == nameof(MainViewModel.OutputSamples) && _outCanvas is not null)
            _outCanvas.Samples = _subscribedVm.OutputSamples;
        if (e.PropertyName == nameof(MainViewModel.PlaybackPosition) && _outCanvas is not null)
            _outCanvas.PlaybackPosition = _subscribedVm.PlaybackPosition;
        if (e.PropertyName == nameof(MainViewModel.GridMode))
            UpdateGridMode(_subscribedVm.GridMode);
    }

    private void BindAll(MainViewModel vm)
    {
        UnsubscribeEnvelopeChanges();
        var voice = vm.Patch.SelectedVoice;

        foreach (var slot in _slots)
        {
            if (slot is EnvelopeSlot envSlot)
            {
                var entry = MainViewModel.SignalChain.FirstOrDefault(e => e.Slot == envSlot.Slot);
                if (entry.Getter is null) continue;
                var envelope = entry.Getter(voice);
                envSlot.Canvas.Envelope = envelope;
                envelope.PropertyChanged += OnEnvelopePropertyChanged;
                UpdateDimming(envSlot, envelope);
            }
        }

        // Bind filter canvases
        var filter = voice.Filter;
        if (_pzCanvas is not null)
        {
            _pzCanvas.Filter = filter;
            _pzCanvas.InvalidateVisual();
        }

        if (_bodeCanvas is not null)
        {
            _bodeCanvas.Filter = filter;
            _bodeCanvas.InvalidateVisual();
        }

        // Bind output waveform
        if (_outCanvas is not null)
        {
            _outCanvas.Samples = vm.OutputSamples;
            _outCanvas.PlaybackPosition = vm.PlaybackPosition;
        }
    }

    private void UnsubscribeEnvelopeChanges()
    {
        foreach (var slot in _slots)
        {
            if (slot is EnvelopeSlot envSlot && envSlot.Canvas.Envelope is { } env)
                env.PropertyChanged -= OnEnvelopePropertyChanged;
        }
    }

    private void OnEnvelopePropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName != nameof(EnvelopeViewModel.IsEmpty)) return;
        if (sender is not EnvelopeViewModel env) return;

        foreach (var slot in _slots)
        {
            if (slot is EnvelopeSlot envSlot && ReferenceEquals(envSlot.Canvas.Envelope, env))
                UpdateDimming(envSlot, env);
        }
    }

    private static void UpdateDimming(EnvelopeSlot slot, EnvelopeViewModel envelope)
    {
        slot.Container.Opacity = envelope.IsEmpty ? 0.35 : 1.0;
    }

    private void UpdateSelection(SignalChainSlot selectedSlot)
    {
        foreach (var slot in _slots)
        {
            var selected = slot.Slot == selectedSlot;
            slot.Container.BorderBrush = SolidColorBrush.Parse(selected ? "#009E73" : "#4a4a4a");
            slot.Container.BorderThickness = new Thickness(1.5, 0.5, 0.5, 0.5);
        }
    }

    // Column 3 cells (filter-related)
    private static readonly HashSet<SignalChainSlot> FilterCells = [SignalChainSlot.PoleZero, SignalChainSlot.Filter, SignalChainSlot.Bode];

    private void UpdateGridMode(GridMode mode)
    {
        var colDefs = MatrixGrid.ColumnDefinitions;
        var rowDefs = MatrixGrid.RowDefinitions;

        switch (mode)
        {
            case GridMode.Main:
                // 3×3: cols 0-2 equal, col 3 = 0
                foreach (var slot in _slots)
                {
                    if (FilterCells.Contains(slot.Slot))
                        slot.Container.Width = double.NaN;
                }
                for (var i = 0; i < 3; i++)
                    colDefs[i].Width = new GridLength(1, GridUnitType.Star);
                colDefs[3].Width = new GridLength(0);
                colDefs[3].MaxWidth = double.PositiveInfinity;
                for (var i = 0; i < 3; i++)
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                MatrixGrid.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Stretch;
                break;

            case GridMode.Filter:
                // Only col 3 visible (stacked vertically in 3 rows)
                for (var i = 0; i < 3; i++)
                    colDefs[i].Width = new GridLength(0);
                colDefs[3].Width = GridLength.Auto;
                colDefs[3].MaxWidth = double.PositiveInfinity;
                for (var i = 0; i < 3; i++)
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                MatrixGrid.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Center;
                foreach (var slot in _slots)
                {
                    if (FilterCells.Contains(slot.Slot))
                        slot.Container.Width = 300;
                }
                break;

            case GridMode.Both:
                // Full 3×4 — equal columns
                foreach (var slot in _slots)
                {
                    if (FilterCells.Contains(slot.Slot))
                        slot.Container.Width = double.NaN;
                }
                colDefs[0].Width = new GridLength(1, GridUnitType.Star);
                colDefs[1].Width = new GridLength(1, GridUnitType.Star);
                colDefs[2].Width = new GridLength(1, GridUnitType.Star);
                colDefs[3].Width = new GridLength(1, GridUnitType.Star);
                colDefs[3].MaxWidth = double.PositiveInfinity;
                for (var i = 0; i < 3; i++)
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                MatrixGrid.HorizontalAlignment = Avalonia.Layout.HorizontalAlignment.Stretch;
                break;
        }
    }
}
