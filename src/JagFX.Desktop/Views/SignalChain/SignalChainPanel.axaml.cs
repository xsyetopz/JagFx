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

    private abstract record SlotBase(string Title, Border Container);
    private sealed record EnvelopeSlot(string Title, Border Container, EnvelopeCanvas Canvas) : SlotBase(Title, Container);
    private sealed record SpecialSlot(string Title, Border Container, Control Canvas) : SlotBase(Title, Container);

    private readonly List<SlotBase> _slots = [];

    // 3×4 matrix layout: [row][col] = (title, type)
    private static readonly (string Title, SlotType Type)[,] Matrix =
    {
        { ("PITCH", SlotType.Envelope), ("V.RATE", SlotType.Envelope), ("V.DEPTH", SlotType.Envelope), ("P/Z", SlotType.PoleZero) },
        { ("VOLUME", SlotType.Envelope), ("T.RATE", SlotType.Envelope), ("T.DEPTH", SlotType.Envelope), ("FILTER", SlotType.Envelope) },
        { ("GAP OFF", SlotType.Envelope), ("GAP ON", SlotType.Envelope), ("OUT", SlotType.Waveform), ("BODE", SlotType.Bode) },
    };

    private enum SlotType { Envelope, PoleZero, Waveform, Bode }

    private WaveformCanvas? _outCanvas;
    private PoleZeroCanvas? _pzCanvas;
    private FrequencyResponseCanvas? _bodeCanvas;
    private ToggleButton? _activeSoloButton;

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
                var (title, slotType) = Matrix[row, col];
                var color = GetSlotColor(title, slotType);

                var titleBlock = new TextBlock
                {
                    Text = title,
                    FontSize = 10,
                    FontWeight = FontWeight.Bold,
                    Foreground = new SolidColorBrush(Color.Parse(color)),
                    Opacity = 0.7,
                    Margin = new Thickness(4, 1, 0, 0),
                };

                Control canvas;
                SlotBase slot;

                switch (slotType)
                {
                    case SlotType.Envelope:
                        {
                            var envCanvas = new EnvelopeCanvas
                            {
                                IsThumbnail = false,
                                LineColor = new SolidColorBrush(Color.Parse(color)),
                            };
                            canvas = envCanvas;
                            var container = WrapInCell(titleBlock, canvas, row, col, title, slotType);
                            slot = new EnvelopeSlot(title, container, envCanvas);
                            break;
                        }
                    case SlotType.PoleZero:
                        {
                            var pzc = new PoleZeroCanvas();
                            _pzCanvas = pzc;
                            canvas = pzc;
                            var container = WrapInCell(titleBlock, canvas, row, col, title, slotType);
                            slot = new SpecialSlot(title, container, pzc);
                            break;
                        }
                    case SlotType.Waveform:
                        {
                            var wc = new WaveformCanvas();
                            _outCanvas = wc;
                            canvas = wc;
                            var container = WrapInCell(titleBlock, canvas, row, col, title, slotType);
                            slot = new SpecialSlot(title, container, wc);
                            break;
                        }
                    case SlotType.Bode:
                        {
                            var frc = new FrequencyResponseCanvas();
                            _bodeCanvas = frc;
                            canvas = frc;
                            var container = WrapInCell(titleBlock, canvas, row, col, title, slotType);
                            slot = new SpecialSlot(title, container, frc);
                            break;
                        }
                    default:
                        continue;
                }

                _slots.Add(slot);
            }
        }
    }

    private Border WrapInCell(TextBlock titleBlock, Control canvas, int row, int col, string title, SlotType slotType)
    {
        var innerGrid = new Grid();
        innerGrid.RowDefinitions.Add(new RowDefinition(GridLength.Auto));
        innerGrid.RowDefinitions.Add(new RowDefinition(new GridLength(1, GridUnitType.Star)));

        // Header row: title (left) + toolbar (right) on same line
        var headerGrid = new Grid();
        headerGrid.ColumnDefinitions.Add(new ColumnDefinition(new GridLength(1, GridUnitType.Star)));
        headerGrid.ColumnDefinitions.Add(new ColumnDefinition(GridLength.Auto));

        Grid.SetColumn(titleBlock, 0);
        headerGrid.Children.Add(titleBlock);

        var toolbar = CreateToolbar(canvas, title, slotType);
        toolbar.Margin = new Thickness(0);
        Grid.SetColumn(toolbar, 1);
        headerGrid.Children.Add(toolbar);

        Grid.SetRow(headerGrid, 0);
        innerGrid.Children.Add(headerGrid);

        Grid.SetRow(canvas, 1);
        innerGrid.Children.Add(canvas);

        var container = new Border
        {
            BorderBrush = SolidColorBrush.Parse("#272727"),
            BorderThickness = new Thickness(1),
            Padding = new Thickness(2, 1),
            Child = innerGrid,
            Cursor = new Cursor(StandardCursorType.Hand),
        };

        container.PointerPressed += (_, _) =>
        {
            if (_subscribedVm is not null)
                _subscribedVm.SelectEnvelope(title);
        };

        Grid.SetRow(container, row);
        Grid.SetColumn(container, col);
        MatrixGrid.Children.Add(container);

        return container;
    }

    #region Toolbar creation

    private StackPanel CreateToolbar(Control canvas, string title, SlotType slotType)
    {
        var toolbar = new StackPanel
        {
            Orientation = Avalonia.Layout.Orientation.Horizontal,
            Spacing = 1,
            Margin = new Thickness(2, 0, 0, 1),
        };

        // All cells get zoom buttons
        var zoomGroup = CreateZoomGroup(canvas);
        toolbar.Children.Add(zoomGroup);

        // Envelope cells get [S] solo button
        if (slotType == SlotType.Envelope)
        {
            var soloBtn = CreateSoloButton(title);
            toolbar.Children.Add(soloBtn);
        }

        // All except P/Z and BODE get M..▼ dropdown
        if (slotType != SlotType.PoleZero && slotType != SlotType.Bode)
        {
            var modeBtn = CreateModeDropdown(title, slotType);
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

            group.Children.Add(btn);
        }

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
            case PoleZeroCanvas pzc:
                pzc.ZoomLevel = zoomLevel;
                break;
            case FrequencyResponseCanvas frc:
                frc.ZoomLevel = zoomLevel;
                break;
        }
    }

    private ToggleButton CreateSoloButton(string title)
    {
        var btn = new ToggleButton
        {
            Content = "S",
            Theme = (ControlTheme?)Application.Current!.FindResource("JagCellToggle"),
            Margin = new Thickness(3, 0, 0, 0),
        };
        btn.Classes.Add("solo");

        btn.Click += (s, _) =>
        {
            if (s is not ToggleButton toggled) return;

            if (toggled.IsChecked == true)
            {
                // Deactivate previous solo
                if (_activeSoloButton is not null && _activeSoloButton != toggled)
                    _activeSoloButton.IsChecked = false;

                _activeSoloButton = toggled;
                if (_subscribedVm is not null)
                    _subscribedVm.SoloedEnvelope = title;
            }
            else
            {
                if (_activeSoloButton == toggled)
                    _activeSoloButton = null;
                if (_subscribedVm is not null)
                    _subscribedVm.SoloedEnvelope = null;
            }
        };

        return btn;
    }

    private Button CreateModeDropdown(string title, SlotType slotType)
    {
        var btn = new Button
        {
            Content = "MODE \u25BE",
            Theme = (ControlTheme?)Application.Current!.FindResource("JagCellButton"),
            Margin = new Thickness(3, 0, 0, 0),
        };

        btn.Click += (s, _) =>
        {
            if (s is not Button button) return;

            var menu = new ContextMenu();

            if (slotType == SlotType.Envelope)
            {
                // Map to Waveform enum
                var waveforms = new (string Label, Waveform Value)[]
                {
                    ("Off", Waveform.Off),
                    ("Square", Waveform.Square),
                    ("Sine", Waveform.Sine),
                    ("Saw", Waveform.Saw),
                    ("Noise", Waveform.Noise),
                };

                foreach (var (label, wfValue) in waveforms)
                {
                    var item = new MenuItem { Header = label };
                    var wf = wfValue;
                    item.Click += (_, _) =>
                    {
                        var env = FindEnvelopeForTitle(title);
                        if (env is not null)
                            env.Waveform = wf;
                    };
                    menu.Items.Add(item);
                }

                // Update check marks based on current waveform
                menu.Opening += (_, _) =>
                {
                    var env = FindEnvelopeForTitle(title);
                    if (env is null) return;
                    for (var i = 0; i < menu.Items.Count; i++)
                    {
                        if (menu.Items[i] is MenuItem mi)
                        {
                            var wf = waveforms[i].Value;
                            mi.Icon = env.Waveform == wf
                                ? new TextBlock { Text = "\u2713", FontSize = 9 }
                                : null;
                        }
                    }
                };
            }
            else if (slotType == SlotType.Waveform)
            {
                // Stub display mode options
                var modes = new[] { "Wave", "Spectrum" };
                foreach (var mode in modes)
                {
                    var item = new MenuItem { Header = mode, IsEnabled = mode == "Wave" };
                    menu.Items.Add(item);
                }
            }

            menu.Open(button);
        };

        return btn;
    }

    private EnvelopeViewModel? FindEnvelopeForTitle(string title)
    {
        if (_subscribedVm is null) return null;
        var voice = _subscribedVm.Patch.SelectedVoice;
        var entry = MainViewModel.SignalChain.FirstOrDefault(e => e.Title == title);
        return entry.Getter?.Invoke(voice);
    }

    #endregion

    private static string GetSlotColor(string title, SlotType type) => type switch
    {
        SlotType.PoleZero => "#8888d4",
        SlotType.Waveform => "#44BB77",
        SlotType.Bode => "#8888d4",
        _ => MainViewModel.SignalChain.FirstOrDefault(e => e.Title == title).Color ?? "#44BB77",
    };

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
            UpdateSelection(vm.SelectedEnvelopeTitle);
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

        if (e.PropertyName == nameof(MainViewModel.SelectedEnvelopeTitle))
            UpdateSelection(_subscribedVm.SelectedEnvelopeTitle);
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
                var entry = MainViewModel.SignalChain.FirstOrDefault(e => e.Title == envSlot.Title);
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

    private void UpdateSelection(string title)
    {
        foreach (var slot in _slots)
        {
            var selected = slot.Title == title;
            slot.Container.BorderBrush = SolidColorBrush.Parse(selected ? "#44BB77" : "#272727");
            slot.Container.BorderThickness = new Thickness(1.5, 0.5, 0.5, 0.5);
        }
    }

    // Column 3 cells (filter-related)
    private static readonly HashSet<string> FilterCells = ["P/Z", "FILTER", "BODE"];

    private void UpdateGridMode(GridMode mode)
    {
        var colDefs = MatrixGrid.ColumnDefinitions;
        var rowDefs = MatrixGrid.RowDefinitions;

        switch (mode)
        {
            case GridMode.Main:
                // 3×3: cols 0-2 equal, col 3 = 0
                for (var i = 0; i < 3; i++)
                    colDefs[i].Width = new GridLength(1, GridUnitType.Star);
                colDefs[3].Width = new GridLength(0);
                for (var i = 0; i < 3; i++)
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                break;

            case GridMode.Filter:
                // Only col 3 visible (stacked vertically in 3 rows)
                for (var i = 0; i < 3; i++)
                    colDefs[i].Width = new GridLength(0);
                colDefs[3].Width = new GridLength(1, GridUnitType.Star);
                for (var i = 0; i < 3; i++)
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                break;

            case GridMode.Both:
                // Full 3×4
                colDefs[0].Width = new GridLength(27, GridUnitType.Star);
                colDefs[1].Width = new GridLength(27, GridUnitType.Star);
                colDefs[2].Width = new GridLength(27, GridUnitType.Star);
                colDefs[3].Width = new GridLength(19, GridUnitType.Star);
                for (var i = 0; i < 3; i++)
                    rowDefs[i].Height = new GridLength(1, GridUnitType.Star);
                break;
        }
    }
}
