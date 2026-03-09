using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using JagFX.Desktop.Services;
using JagFX.Io;
using JagFX.Synthesis.Data;

namespace JagFX.Desktop.ViewModels;

public partial class MainViewModel : ObservableObject
{
    [ObservableProperty]
    private string _patchName = "untitled";

    [ObservableProperty]
    private string? _filePath;

    [ObservableProperty]
    private bool _isDirty;

    [ObservableProperty]
    private bool _isPlaying;

    [ObservableProperty]
    private EnvelopeViewModel? _selectedEnvelope;

    [ObservableProperty]
    private string _selectedEnvelopeTitle = "";

    [ObservableProperty]
    private float[]? _outputSamples;

    public Func<Task>? RequestOpenDialog { get; set; }
    public Func<Task>? RequestSaveAsDialog { get; set; }
    public Func<Task>? RequestExportDialog { get; set; }

    public string WindowTitle => $"JagFX - {PatchName}{(IsDirty ? " *" : "")}";

    public PatchViewModel Patch { get; } = new();
    public RackCellDefinition[] RackCells { get; }

    private readonly PatchFileManager _fileManager;
    private readonly AudioPlaybackService _playback = new();

    public MainViewModel()
    {
        RackCells = BuildRackDefinitions();
        _fileManager = new PatchFileManager(Patch);
        _fileManager.FileChanged += OnFileChanged;
        Patch.PropertyChanged += (_, _) => MarkDirty();
    }

    partial void OnPatchNameChanged(string value) => OnPropertyChanged(nameof(WindowTitle));
    partial void OnIsDirtyChanged(bool value) => OnPropertyChanged(nameof(WindowTitle));

    private void MarkDirty() => IsDirty = true;

    private void OnFileChanged()
    {
        PatchName = _fileManager.PatchName;
        FilePath = _fileManager.FilePath;
        IsDirty = false;
    }

    #region File operations

    [RelayCommand]
    private void Open() => _ = RequestOpenDialog?.Invoke();

    public void LoadFromPath(string path) => _fileManager.LoadFromPath(path);

    [RelayCommand]
    private void Save() { _fileManager.Save(); IsDirty = false; }

    [RelayCommand]
    private void SaveAs() => _ = RequestSaveAsDialog?.Invoke();

    public void SaveToPath(string path) => _fileManager.SaveToPath(path);

    [RelayCommand]
    private void NavigatePatch(int direction) => _fileManager.NavigatePatch(direction);

    [RelayCommand]
    private void Export() => _ = RequestExportDialog?.Invoke();

    public async Task ExportToPathAsync(string path)
    {
        var model = Patch.ToModel();
        var buffer = await SynthesisService.RenderAsync(model);
        WaveFileWriter.WriteToPath(buffer.ToUBytes(), path);
    }

    #endregion

    #region Voice & envelope selection

    [RelayCommand]
    private void SelectVoice(int index)
    {
        if (index >= 0 && index < Patch.Voices.Count)
        {
            Patch.SelectedVoiceIndex = index;
            if (SelectedEnvelope is not null)
                SelectEnvelope(SelectedEnvelopeTitle);
        }
    }

    public void SelectEnvelope(string title)
    {
        var voice = Patch.SelectedVoice;
        SelectedEnvelopeTitle = title;
        SelectedEnvelope = title switch
        {
            "PITCH" => voice.Pitch,
            "V.RATE" => voice.VibratoRate,
            "V.DEPTH" => voice.VibratoDepth,
            "VOLUME" => voice.Volume,
            "T.RATE" => voice.TremoloRate,
            "T.DEPTH" => voice.TremoloDepth,
            "FILTER" => voice.FilterEnvelope,
            "GAP OFF" => voice.GapOff,
            "GAP ON" => voice.GapOn,
            _ => null
        };
    }

    #endregion

    #region Playback

    [RelayCommand]
    private async Task TogglePlayAsync()
    {
        if (IsPlaying)
        {
            _playback.Stop();
            IsPlaying = false;
            return;
        }

        IsPlaying = true;
        var model = Patch.ToModel();
        var buffer = await SynthesisService.RenderAsync(model);

        if (buffer.Length > 0)
        {
            var maxAbs = buffer.Samples.Max(Math.Abs);
            var scale = maxAbs > 0 ? 1.0f / maxAbs : 0f;
            OutputSamples = Array.ConvertAll(buffer.Samples, s => s * scale);
            _playback.Play(buffer);
        }
        else
        {
            IsPlaying = false;
        }
    }

    [RelayCommand]
    private void Stop()
    {
        _playback.Stop();
        IsPlaying = false;
    }

    #endregion

    private static RackCellDefinition[] BuildRackDefinitions() =>
    [
        new("PITCH", "Frequency Env", RackCellType.Envelope, "LineBase", v => v.Pitch),
        new("V.RATE", "Vibrato Rate", RackCellType.Envelope, "LineVibrato", v => v.VibratoRate),
        new("V.DEPTH", "Vibrato Depth", RackCellType.Envelope, "LineVibrato", v => v.VibratoDepth),
        new("P/Z", "Pole/Zero", RackCellType.PoleZero, "LineFilter", null),

        new("VOLUME", "Volume Env", RackCellType.Envelope, "LineBase", v => v.Volume),
        new("T.RATE", "Tremolo Rate", RackCellType.Envelope, "LineTremolo", v => v.TremoloRate),
        new("T.DEPTH", "Tremolo Depth", RackCellType.Envelope, "LineTremolo", v => v.TremoloDepth),
        new("FILTER", "Filter Cutoff", RackCellType.Envelope, "LineFilter", v => v.FilterEnvelope),

        new("GAP OFF", "Gap Off", RackCellType.Envelope, "LineGate", v => v.GapOff),
        new("GAP ON", "Gap On", RackCellType.Envelope, "LineGate", v => v.GapOn),
        new("OUTPUT", "Waveform", RackCellType.Output, "LineBase", null),
        new("BODE", "Freq Response", RackCellType.BodePlot, "LineFilter", null),
    ];
}

public enum RackCellType
{
    Envelope,
    PoleZero,
    Output,
    BodePlot
}

public record RackCellDefinition(
    string Title,
    string Description,
    RackCellType CellType,
    string ColorKey,
    Func<VoiceViewModel, EnvelopeViewModel>? EnvelopeGetter);
