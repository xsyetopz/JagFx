using System.Collections.Immutable;
using System.Collections.Specialized;
using System.ComponentModel;
using Avalonia.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using JagFx.Desktop.Controls;
using JagFx.Desktop.Services;
using JagFx.Domain.Models;
using JagFx.Io;
using JagFx.Synthesis.Data;

namespace JagFx.Desktop.ViewModels;

public enum GridMode { Main, Filter, Both }

public partial class MainViewModel : ObservableObject, IDisposable
{
    [ObservableProperty]
    private GridMode _gridMode = GridMode.Main;

    public bool IsMainMode => GridMode == GridMode.Main;
    public bool IsFilterMode => GridMode == GridMode.Filter;
    public bool IsBothMode => GridMode == GridMode.Both;

    partial void OnGridModeChanged(GridMode value)
    {
        OnPropertyChanged(nameof(IsMainMode));
        OnPropertyChanged(nameof(IsFilterMode));
        OnPropertyChanged(nameof(IsBothMode));
    }

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
    private string _selectedEnvelopeColor = "#44BB77";

    [ObservableProperty]
    private float[]? _outputSamples;

    [ObservableProperty]
    private double _playbackPosition;

    [ObservableProperty]
    private bool _playSingleVoice;

    [ObservableProperty]
    private string? _soloedEnvelope;

    [ObservableProperty]
    private bool _isLooping;

    [ObservableProperty]
    private bool _trueWaveEnabled = true;

    [ObservableProperty]
    private bool _isCopyMode;

    public Func<Task>? RequestOpenDialog { get; set; }
    public Func<Task>? RequestSaveAsDialog { get; set; }

    public string WindowTitle
    {
        get
        {
            var dirty = IsDirty ? " *" : "";
            if (string.IsNullOrEmpty(FilePath))
                return $"JagFx - {PatchName}{dirty}";
            var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var display = FilePath.StartsWith(home)
                ? "~" + FilePath[home.Length..]
                : FilePath;
            return $"JagFx - {display}{dirty}";
        }
    }

    public PatchViewModel Patch { get; } = new();

    private readonly PatchFileManager _fileManager;
    private readonly AudioPlaybackService _playback = new();

    private AudioBuffer? _cachedBuffer;
    private CancellationTokenSource? _renderCts;
    private System.Threading.Timer? _debounceTimer;
    private DispatcherTimer? _positionTimer;
    private DateTime _playbackStart;
    private double _playbackDuration;
    private VoiceViewModel? _subscribedVoice;
    private bool _stoppingManually;
    private Voice? _copiedVoice;

    public MainViewModel()
    {
        _fileManager = new PatchFileManager(Patch);
        _fileManager.FileChanged += OnFileChanged;
        Patch.PropertyChanged += (_, e) =>
        {
            MarkDirty();
            if (e.PropertyName == nameof(PatchViewModel.SelectedVoiceIndex))
            {
                SubscribeVoiceChanges(Patch.SelectedVoice);
            }
        };
        _playback.PlaybackFinished += OnPlaybackFinished;
        SubscribeVoiceChanges(Patch.SelectedVoice);
    }

    partial void OnPatchNameChanged(string value) => OnPropertyChanged(nameof(WindowTitle));
    partial void OnIsDirtyChanged(bool value) => OnPropertyChanged(nameof(WindowTitle));
    partial void OnFilePathChanged(string? value) => OnPropertyChanged(nameof(WindowTitle));

    partial void OnPlaySingleVoiceChanged(bool value)
    {
        _cachedBuffer = null;
        ScheduleRerender();
    }

    partial void OnSoloedEnvelopeChanged(string? value)
    {
        _cachedBuffer = null;
        ScheduleRerender();
    }

    private void MarkDirty()
    {
        IsDirty = true;
        _cachedBuffer = null;
        ScheduleRerender();
    }

    partial void OnTrueWaveEnabledChanged(bool value)
    {
        if (value) ScheduleRerender();
    }

    private void ScheduleRerender()
    {
        IsDirty = true;
        _cachedBuffer = null;

        if (!TrueWaveEnabled) return;

        _debounceTimer?.Dispose();
        _debounceTimer = new System.Threading.Timer(
            _ => Dispatcher.UIThread.Post(async () =>
            {
                try { await RenderAndCacheAsync(); }
                catch (Exception ex) { System.Diagnostics.Debug.WriteLine($"Render failed: {ex}"); }
            }),
            null, 100, Timeout.Infinite);
    }

    private async Task RenderAndCacheAsync()
    {
        _renderCts?.Cancel();
        _renderCts = new CancellationTokenSource();
        var cts = _renderCts;
        try
        {
            var model = Patch.ToModel();
            var voiceFilter = PlaySingleVoice ? Patch.SelectedVoiceIndex : -1;

            if (SoloedEnvelope is not null)
                model = ApplySolo(model, voiceFilter >= 0 ? voiceFilter : Patch.SelectedVoiceIndex);

            var buffer = await SynthesisService.RenderAsync(model, voiceFilter: voiceFilter, ct: cts.Token);
            if (cts.Token.IsCancellationRequested) return;
            _cachedBuffer = buffer;
            if (buffer.Length > 0)
            {
                var maxAbs = buffer.Samples.Max(Math.Abs);
                var scale = maxAbs > 0 ? 1.0f / maxAbs : 0f;
                OutputSamples = Array.ConvertAll(buffer.Samples, s => s * scale);
            }
        }
        catch (OperationCanceledException) { }
    }

    private Patch ApplySolo(Patch model, int voiceIndex)
    {
        if (voiceIndex < 0 || voiceIndex >= model.Voices.Count) return model;
        var voice = model.Voices[voiceIndex];
        if (voice is null) return model;

        var soloTitle = SoloedEnvelope;

        // Build neutralized voice based on what's soloed
        var neutralFreq = voice.FrequencyEnvelope; // always keep base frequency
        var neutralAmp = new Envelope(Waveform.Off, 65535, 65535,
            ImmutableList.Create(new Segment(voice.DurationMs, 65535))); // constant max amplitude

        Voice soloedVoice = soloTitle switch
        {
            "PITCH" => voice with
            {
                AmplitudeEnvelope = neutralAmp,
                PitchLfo = null,
                AmplitudeLfo = null,
                GapOffEnvelope = null,
                GapOnEnvelope = null,
                Filter = null
            },
            "VOLUME" => voice with
            {
                PitchLfo = null,
                AmplitudeLfo = null,
                GapOffEnvelope = null,
                GapOnEnvelope = null,
                Filter = null
            },
            "V.RATE" or "V.DEPTH" => voice with
            {
                AmplitudeEnvelope = neutralAmp,
                AmplitudeLfo = null,
                GapOffEnvelope = null,
                GapOnEnvelope = null,
                Filter = null
            },
            "T.RATE" or "T.DEPTH" => voice with
            {
                PitchLfo = null,
                GapOffEnvelope = null,
                GapOnEnvelope = null,
                Filter = null
            },
            "GAP OFF" or "GAP ON" => voice with
            {
                AmplitudeEnvelope = neutralAmp,
                PitchLfo = null,
                AmplitudeLfo = null,
                Filter = null
            },
            "FILTER" => voice with
            {
                AmplitudeEnvelope = neutralAmp,
                PitchLfo = null,
                AmplitudeLfo = null,
                GapOffEnvelope = null,
                GapOnEnvelope = null
            },
            _ => voice
        };

        var voices = model.Voices.SetItem(voiceIndex, soloedVoice);
        return model with { Voices = voices };
    }

    private void OnFileChanged()
    {
        PatchName = _fileManager.PatchName;
        FilePath = _fileManager.FilePath;
        IsDirty = false;
        _ = RenderAndCacheAsync();
    }

    #region Deep voice change subscriptions

    private void SubscribeVoiceChanges(VoiceViewModel voice)
    {
        UnsubscribeVoiceChanges();
        _subscribedVoice = voice;
        voice.PropertyChanged += OnVoicePropertyChanged;

        foreach (var env in GetVoiceEnvelopes(voice))
        {
            env.PropertyChanged += OnNestedPropertyChanged;
            env.Segments.CollectionChanged += OnSegmentsCollectionChanged;
            foreach (var seg in env.Segments)
                seg.PropertyChanged += OnNestedPropertyChanged;
        }
    }

    private void UnsubscribeVoiceChanges()
    {
        if (_subscribedVoice is null) return;
        _subscribedVoice.PropertyChanged -= OnVoicePropertyChanged;

        foreach (var env in GetVoiceEnvelopes(_subscribedVoice))
        {
            env.PropertyChanged -= OnNestedPropertyChanged;
            env.Segments.CollectionChanged -= OnSegmentsCollectionChanged;
            foreach (var seg in env.Segments)
                seg.PropertyChanged -= OnNestedPropertyChanged;
        }

        _subscribedVoice = null;
    }

    private static EnvelopeViewModel[] GetVoiceEnvelopes(VoiceViewModel v) =>
    [
        v.Pitch, v.Volume, v.VibratoRate, v.VibratoDepth,
        v.TremoloRate, v.TremoloDepth, v.FilterEnvelope,
        v.GapOff, v.GapOn
    ];

    private void OnVoicePropertyChanged(object? s, PropertyChangedEventArgs e) => MarkDirty();

    private void OnNestedPropertyChanged(object? s, PropertyChangedEventArgs e) => MarkDirty();

    private void OnSegmentsCollectionChanged(object? s, NotifyCollectionChangedEventArgs e)
    {
        if (e.NewItems is not null)
            foreach (SegmentViewModel seg in e.NewItems)
                seg.PropertyChanged += OnNestedPropertyChanged;
        if (e.OldItems is not null)
            foreach (SegmentViewModel seg in e.OldItems)
                seg.PropertyChanged -= OnNestedPropertyChanged;
        MarkDirty();
    }

    #endregion

    #region File operations

    [RelayCommand]
    private void Open() => _ = RequestOpenDialog?.Invoke();

    public void LoadFromPath(string path) => _fileManager.LoadFromPath(path);

    [RelayCommand]
    private void Save()
    {
        if (FilePath is not null)
        {
            _fileManager.Save();
            IsDirty = false;
        }
        else
        {
            _ = RequestSaveAsDialog?.Invoke();
        }
    }

    public void SaveToPath(string path) => _fileManager.SaveToPath(path);

    [RelayCommand]
    private void NavigatePatch(string direction) => _fileManager.NavigatePatch(int.Parse(direction));

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
            if (IsCopyMode && _copiedVoice is not null && index != Patch.SelectedVoiceIndex)
            {
                Patch.Voices[index].Load(_copiedVoice);
                IsCopyMode = false;
                _cachedBuffer = null;
            }

            Patch.SelectedVoiceIndex = index;
            if (SelectedEnvelope is not null)
                SelectEnvelope(SelectedEnvelopeTitle);
        }
    }

    public static readonly (string Title, string Color, Func<VoiceViewModel, EnvelopeViewModel> Getter)[] SignalChain =
    [
        ("PITCH",   "#44BB77", v => v.Pitch),
        ("V.RATE",  "#44BB77", v => v.VibratoRate),
        ("V.DEPTH", "#44BB77", v => v.VibratoDepth),
        ("VOLUME",  "#44BB77", v => v.Volume),
        ("T.RATE",  "#44BB77", v => v.TremoloRate),
        ("T.DEPTH", "#44BB77", v => v.TremoloDepth),
        ("GAP OFF", "#44BB77", v => v.GapOff),
        ("GAP ON",  "#44BB77", v => v.GapOn),
        ("FILTER",  "#8888d4", v => v.FilterEnvelope),
    ];

    public void SelectEnvelope(string title)
    {
        var voice = Patch.SelectedVoice;
        SelectedEnvelopeTitle = title;

        var entry = SignalChain.FirstOrDefault(e => e.Title == title);
        if (entry.Getter is not null)
        {
            SelectedEnvelope = entry.Getter(voice);
            SelectedEnvelopeColor = entry.Color;
        }
        else
        {
            SelectedEnvelope = null;
        }
    }

    public void SelectEnvelopeByOffset(int offset)
    {
        var currentIndex = Array.FindIndex(SignalChain, e => e.Title == SelectedEnvelopeTitle);
        if (currentIndex < 0) currentIndex = 0;
        var newIndex = Math.Clamp(currentIndex + offset, 0, SignalChain.Length - 1);
        SelectEnvelope(SignalChain[newIndex].Title);
    }

    public void CopyVoice()
    {
        _copiedVoice = Patch.SelectedVoice.ToModel();
        IsCopyMode = true;
    }

    public void ResetVoice()
    {
        Patch.SelectedVoice.Clear();
        _cachedBuffer = null;
        ScheduleRerender();
    }

    #endregion

    #region Playback

    [RelayCommand]
    private async Task TogglePlayAsync()
    {
        _playback.Stop();
        StopPositionTimer();

        var buffer = _cachedBuffer;
        if (buffer is null || buffer.Length == 0)
        {
            var model = Patch.ToModel();
            var voiceFilter = PlaySingleVoice ? Patch.SelectedVoiceIndex : -1;
            buffer = await SynthesisService.RenderAsync(model, voiceFilter: voiceFilter);
            _cachedBuffer = buffer;
            if (buffer.Length > 0)
            {
                var maxAbs = buffer.Samples.Max(Math.Abs);
                var scale = maxAbs > 0 ? 1.0f / maxAbs : 0f;
                OutputSamples = Array.ConvertAll(buffer.Samples, s => s * scale);
            }
        }

        if (buffer.Length > 0)
        {
            IsPlaying = true;
            await _playback.PlayAsync(buffer);
            StartPositionTimer(buffer.Length / (double)buffer.SampleRate);
        }
    }

    [RelayCommand]
    private void Stop()
    {
        _stoppingManually = true;
        _playback.Stop();
        StopPositionTimer();
        IsPlaying = false;
        _stoppingManually = false;
    }

    private void StartPositionTimer(double durationSeconds)
    {
        StopPositionTimer();
        _playbackDuration = durationSeconds;
        _playbackStart = DateTime.UtcNow;
        _positionTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(33) };
        _positionTimer.Tick += OnPositionTick;
        _positionTimer.Start();
    }

    private void OnPositionTick(object? sender, EventArgs e)
    {
        var elapsed = (DateTime.UtcNow - _playbackStart).TotalSeconds;
        PlaybackPosition = Math.Clamp(elapsed / _playbackDuration, 0, 1);
        if (PlaybackPosition >= 1.0)
        {
            if (IsLooping && _cachedBuffer is { Length: > 0 })
            {
                PlaybackPosition = 0;
                _playbackStart = DateTime.UtcNow;
            }
            else
            {
                StopPositionTimer();
                IsPlaying = false;
            }
        }
    }

    private void StopPositionTimer()
    {
        if (_positionTimer is not null)
        {
            _positionTimer.Tick -= OnPositionTick;
            _positionTimer.Stop();
            _positionTimer = null;
        }
        PlaybackPosition = 0;
    }

    private void OnPlaybackFinished()
    {
        Dispatcher.UIThread.Post(() =>
        {
            if (IsLooping && !_stoppingManually && _cachedBuffer is { Length: > 0 })
            {
                _ = _playback.PlayAsync(_cachedBuffer);
                _playbackStart = DateTime.UtcNow;
                return;
            }

            StopPositionTimer();
            IsPlaying = false;
        });
    }

    #endregion

    public void Dispose()
    {
        _debounceTimer?.Dispose();
        StopPositionTimer();
        _renderCts?.Cancel();
        _renderCts?.Dispose();
        _playback.Dispose();
        UnsubscribeVoiceChanges();
        GC.SuppressFinalize(this);
    }

}
