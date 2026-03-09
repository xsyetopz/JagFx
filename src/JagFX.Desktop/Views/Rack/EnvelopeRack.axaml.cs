using Avalonia.Controls;
using Avalonia.Media;
using JagFX.Desktop.ViewModels;
using System.ComponentModel;

namespace JagFX.Desktop.Views.Rack;

public partial class EnvelopeRack : UserControl
{
    public EnvelopeRack()
    {
        InitializeComponent();
        DataContextChanged += OnDataContextChanged;
    }

    private void OnDataContextChanged(object? sender, EventArgs e)
    {
        if (DataContext is MainViewModel vm)
        {
            vm.Patch.PropertyChanged += OnPatchPropertyChanged;
            vm.PropertyChanged += OnMainViewModelPropertyChanged;
            BindCells(vm);
        }
    }

    private void OnPatchPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(PatchViewModel.SelectedVoice) && DataContext is MainViewModel vm)
        {
            BindCells(vm);
        }
    }

    private void OnMainViewModelPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (sender is not MainViewModel vm) return;

        if (e.PropertyName == nameof(MainViewModel.SelectedEnvelopeTitle))
            UpdateCellSelection(vm.SelectedEnvelopeTitle);

        if (e.PropertyName == nameof(MainViewModel.OutputSamples))
            CellOutput.WaveformSamples = vm.OutputSamples;
    }

    private void UpdateCellSelection(string title)
    {
        foreach (var cell in GetAllCells())
            cell.SetSelected(cell.CellTitle == title);
    }

    private EnvelopeCell[] GetAllCells() =>
    [
        CellPitch, CellVibratoRate, CellVibratoDepth, CellPoleZero,
        CellVolume, CellTremoloRate, CellTremoloDepth, CellFilter,
        CellGapOff, CellGapOn, CellOutput, CellBode
    ];

    private void BindCells(MainViewModel vm)
    {
        var voice = vm.Patch.SelectedVoice;

        // Row 0: Pitch
        SetupEnvelopeCell(CellPitch, "PITCH", voice.Pitch, "#4db8d4", vm);
        SetupEnvelopeCell(CellVibratoRate, "V.RATE", voice.VibratoRate, "#d4a84d", vm);
        SetupEnvelopeCell(CellVibratoDepth, "V.DEPTH", voice.VibratoDepth, "#d4a84d", vm);
        SetupSpecialCell(CellPoleZero, "P/Z", RackCellType.PoleZero, voice, vm);

        // Row 1: Amplitude
        SetupEnvelopeCell(CellVolume, "VOLUME", voice.Volume, "#4db8d4", vm);
        SetupEnvelopeCell(CellTremoloRate, "T.RATE", voice.TremoloRate, "#d47a4d", vm);
        SetupEnvelopeCell(CellTremoloDepth, "T.DEPTH", voice.TremoloDepth, "#d47a4d", vm);
        SetupEnvelopeCell(CellFilter, "FILTER", voice.FilterEnvelope, "#8888d4", vm);

        // Row 2: Output
        SetupEnvelopeCell(CellGapOff, "GAP OFF", voice.GapOff, "#4db8a8", vm);
        SetupEnvelopeCell(CellGapOn, "GAP ON", voice.GapOn, "#4db8a8", vm);
        SetupSpecialCell(CellOutput, "OUTPUT", RackCellType.Output, voice, vm);
        SetupSpecialCell(CellBode, "BODE", RackCellType.BodePlot, voice, vm);
    }

    private static void SetupEnvelopeCell(EnvelopeCell cell, string title,
        EnvelopeViewModel envelope, string color, MainViewModel vm)
    {
        cell.CellTitle = title;
        cell.Envelope = envelope;
        cell.LineColor = new SolidColorBrush(Color.Parse(color));
        cell.CellType = RackCellType.Envelope;
        cell.Filter = null;
        cell.MainViewModel = vm;
    }

    private static void SetupSpecialCell(EnvelopeCell cell, string title,
        RackCellType cellType, VoiceViewModel voice, MainViewModel vm)
    {
        cell.CellTitle = title;
        cell.CellType = cellType;
        cell.Envelope = null;
        cell.Filter = voice.Filter;
        cell.MainViewModel = vm;
    }
}
