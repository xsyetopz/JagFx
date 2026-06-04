using System.Collections.Specialized;
using System.ComponentModel;
using System.Globalization;
using Avalonia.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using JagFx.Core.Constants;
using JagFx.Desktop.Services;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Io;
using JagFx.Synthesis.Core;
using JagFx.Synthesis.Data;

namespace JagFx.Desktop.ViewModels;

public enum GridMode
{
    Main,
    Filter,
    Both,
}

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
    private SignalChainSlot _selectedSlot;

    public string SelectedEnvelopeTitle =>
        Loc.Format("SelectedEnvelopeTitle", Loc.Get($"Slot{SelectedSlot}"));

    [ObservableProperty]
    private float[]? _outputSamples;

    [ObservableProperty]
    private double _playbackPosition;

    [ObservableProperty]
    private bool _playSingleVoice;

    [ObservableProperty]
    private bool _isLooping;

    [ObservableProperty]
    private int _loopCount;

    [ObservableProperty]
    private bool _trueWaveEnabled = true;

    [ObservableProperty]
    private bool _isCopyMode;

    [ObservableProperty]
    private string _statusHint = "";

    public Func<Task>? RequestOpenDialog { get; set; }
    public Func<Task>? RequestSaveAsDialog { get; set; }

    public string WindowTitle
    {
        get
        {
            var dirty = IsDirty ? " *" : "";
            if (string.IsNullOrEmpty(FilePath))
                return $"JagFx: {PatchName}{dirty}";
            var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var display = FilePath.StartsWith(home, StringComparison.Ordinal)
                ? "~" + FilePath[home.Length..]
                : FilePath;
            return $"JagFx: {display}{dirty}";
        }
    }

    public PatchViewModel Patch { get; } = new();

    private readonly PatchFileManager _fileManager;
    private readonly AudioPlaybackService _playback = new();

    private AudioBuffer? _cachedBuffer;
    private bool _bufferStale;
    private CancellationTokenSource? _renderCts;
    private System.Threading.Timer? _debounceTimer;
    private DispatcherTimer? _positionTimer;
    private DateTime _playbackStart;
    private double _playbackDuration;
    private VoiceViewModel? _subscribedVoice;
    private bool _stoppingManually;
    private double _singlePassDuration;
    private double _loopCycleDuration;
    private double _loopStartNormalized;
    private double _loopEndNormalized;
    private Voice? _copiedVoice;
    private int _interactiveEditDepth;
    private int _previewRenderPending;
    private int _previewRenderQueued;
    private readonly Dictionary<int, AudioBuffer> _previewVoiceCache = [];
    private readonly HashSet<int> _dirtyPreviewVoices = [];
    private readonly object _previewCacheLock = new();

    private const int CommittedPreviewDelayMs = 400;

    public MainViewModel()
    {
        _fileManager = new PatchFileManager(Patch);
        _fileManager.FileChanged += OnFileChanged;
        Patch.PropertyChanged += (_, e) =>
        {
            ScheduleRerender(immediate: true);
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

    partial void OnSelectedSlotChanged(SignalChainSlot value) =>
        OnPropertyChanged(nameof(SelectedEnvelopeTitle));

    private int EffectiveLoopCount => IsLooping ? (LoopCount == 0 ? 50 : LoopCount) : 1;

    partial void OnPlaySingleVoiceChanged(bool value) => ScheduleRerender(immediate: true);

    partial void OnIsLoopingChanged(bool value) => ScheduleRerender(immediate: true);

    partial void OnLoopCountChanged(int value) => ScheduleRerender(immediate: true);

    partial void OnTrueWaveEnabledChanged(bool value) => ScheduleRerender(immediate: true);

    public void RequestPreviewUpdate(bool immediate = false) => ScheduleRerender(immediate);

    public void BeginPreviewEdit() => _interactiveEditDepth++;

    public void EndPreviewEdit()
    {
        if (_interactiveEditDepth > 0)
            _interactiveEditDepth--;

        if (_interactiveEditDepth == 0)
            ScheduleRerender(immediate: true);
    }

    private void ScheduleRerender(bool immediate = false)
    {
        IsDirty = true;
        _bufferStale = true;

        if (!TrueWaveEnabled)
            _renderCts?.Cancel();

        _debounceTimer?.Dispose();
        if (!TrueWaveEnabled && _interactiveEditDepth > 0)
            return;

        if (immediate || TrueWaveEnabled)
        {
            QueuePreviewRender();
            return;
        }

        _debounceTimer = new System.Threading.Timer(
            _ => QueuePreviewRender(),
            null,
            CommittedPreviewDelayMs,
            Timeout.Infinite
        );
    }

    private void QueuePreviewRender()
    {
        _ = System.Threading.Interlocked.Exchange(ref _previewRenderPending, 1);

        if (System.Threading.Interlocked.Exchange(ref _previewRenderQueued, 1) == 1)
            return;

        Dispatcher.UIThread.Post(ProcessPreviewRenderQueue, DispatcherPriority.Input);
    }

    private void ProcessPreviewRenderQueue() => _ = ProcessPreviewRenderQueueAsync();

    private async Task ProcessPreviewRenderQueueAsync()
    {
        try
        {
            while (System.Threading.Interlocked.Exchange(ref _previewRenderPending, 0) == 1)
            {
                try
                {
                    await RenderAndCacheAsync().ConfigureAwait(true);
                }
                catch (OperationCanceledException ex)
                {
                    System.Diagnostics.Debug.WriteLine($"Render canceled: {ex.Message}");
                }
                catch (Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine($"Render failed: {ex}");
                }
            }
        }
        finally
        {
            System.Threading.Volatile.Write(ref _previewRenderQueued, 0);

            if (
                System.Threading.Volatile.Read(ref _previewRenderPending) == 1
                && System.Threading.Interlocked.Exchange(ref _previewRenderQueued, 1) == 0
            )
            {
                Dispatcher.UIThread.Post(ProcessPreviewRenderQueue, DispatcherPriority.Input);
            }
        }
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

            // Always render single pass for waveform display.
            var buffer = await RenderPreviewAsync(model, voiceFilter, cts.Token)
                .ConfigureAwait(true);
            if (cts.Token.IsCancellationRequested)
                return;

            _singlePassDuration = buffer.Length / (double)buffer.SampleRate;
            var loopBeginMs = Patch.LoopBegin;
            var loopEndMs = Patch.LoopEnd;
            if (loopEndMs > loopBeginMs && _singlePassDuration > 0)
            {
                _loopCycleDuration = (loopEndMs - loopBeginMs) / 1000.0;
                var totalMs = _singlePassDuration * 1000.0;
                _loopStartNormalized = loopBeginMs / totalMs;
                _loopEndNormalized = loopEndMs / totalMs;
            }
            else
            {
                _loopCycleDuration = 0;
            }

            NormalizeAndSetOutput(buffer);

            // If looping playback is active, re-render the playback buffer too
            if (IsPlaying)
            {
                model = DefaultLoopIfUnset(model);

                var playbackBuffer = await SynthesisService
                    .RenderAsync(
                        model,
                        loopCount: EffectiveLoopCount,
                        voiceFilter: voiceFilter,
                        ct: cts.Token
                    )
                    .ConfigureAwait(true);
                if (cts.Token.IsCancellationRequested)
                    return;
                _cachedBuffer = playbackBuffer;
                _bufferStale = false;
                await _playback.UpdateWavAsync(playbackBuffer).ConfigureAwait(true);
            }
        }
        catch (OperationCanceledException ex)
        {
            System.Diagnostics.Debug.WriteLine($"Render canceled: {ex.Message}");
        }
    }

    private Patch DefaultLoopIfUnset(Patch model)
    {
        if (IsLooping && model.Loop.BeginMs >= model.Loop.EndMs && !model.ActiveVoices.IsEmpty)
        {
            var maxMs = model.ActiveVoices.Max(v => v.Voice.DurationMs + v.Voice.OffsetMs);
            if (maxMs > 0)
                return model with { Loop = new LoopSegment(0, maxMs) };
        }
        return model;
    }

    private async Task<AudioBuffer> RenderPreviewAsync(
        Patch model,
        int voiceFilter,
        CancellationToken ct
    ) => await Task.Run(() => RenderPreview(model, voiceFilter, ct), ct).ConfigureAwait(false);

    private AudioBuffer RenderPreview(Patch model, int voiceFilter, CancellationToken ct)
    {
        ct.ThrowIfCancellationRequested();

        var activeVoices =
            voiceFilter < 0
                ? model.ActiveVoices
                : [.. model.ActiveVoices.Where(v => v.Index == voiceFilter)];

        if (activeVoices.IsEmpty)
            return AudioBuffer.Empty(0);

        var maxDuration = activeVoices.Max(v => v.Voice.DurationMs + v.Voice.OffsetMs);
        if (maxDuration <= 0)
            return AudioBuffer.Empty(0);

        var sampleCount = (int)(maxDuration * AudioConstants.SampleRatePerMillisecond);
        var mix = new int[sampleCount];

        foreach (var (index, voice) in activeVoices)
        {
            ct.ThrowIfCancellationRequested();

            AudioBuffer? voiceBuffer;
            var needsRender = false;
            lock (_previewCacheLock)
            {
                needsRender =
                    !_previewVoiceCache.TryGetValue(index, out voiceBuffer)
                    || _dirtyPreviewVoices.Contains(index);
            }

            if (needsRender)
            {
                voiceBuffer = VoiceSynthesizer.Synthesize(voice, ct);
                ct.ThrowIfCancellationRequested();

                lock (_previewCacheLock)
                {
                    _previewVoiceCache[index] = voiceBuffer;
                    _ = _dirtyPreviewVoices.Remove(index);
                }
            }

            if (voiceBuffer is null)
                continue;

            var startOffset = (int)(voice.OffsetMs * AudioConstants.SampleRatePerMillisecond);
            for (var i = 0; i < voiceBuffer.Length; i++)
            {
                if ((i & 0x1FF) == 0)
                    ct.ThrowIfCancellationRequested();

                var pos = i + startOffset;
                if (pos >= 0 && pos < sampleCount)
                    mix[pos] += voiceBuffer.Samples[i];
            }
        }

        AudioMath.ClipInt16(mix, sampleCount);
        return new AudioBuffer(mix, AudioConstants.SampleRate);
    }

    private void NormalizeAndSetOutput(AudioBuffer buffer)
    {
        var samples = buffer.Samples;
        if (samples.Length <= 0)
            return;

        var maxAbs = 0;
        foreach (var s in samples)
        {
            var abs = Math.Abs(s);
            if (abs > maxAbs)
                maxAbs = abs;
        }

        var output = new float[samples.Length];
        if (maxAbs > 0)
        {
            var scale = 1.0f / maxAbs;
            for (var i = 0; i < samples.Length; i++)
                output[i] = samples[i] * scale;
        }

        OutputSamples = output;
    }

    private void OnFileChanged()
    {
        lock (_previewCacheLock)
        {
            _previewVoiceCache.Clear();
            _dirtyPreviewVoices.Clear();
        }
        PatchName = _fileManager.PatchName;
        FilePath = _fileManager.FilePath;
        IsDirty = false;
        QueuePreviewRender();
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

        voice.Filter.PropertyChanged += OnNestedPropertyChanged;
        foreach (var partial in voice.Partials)
            partial.PropertyChanged += OnNestedPropertyChanged;
    }

    private void UnsubscribeVoiceChanges()
    {
        if (_subscribedVoice is null)
            return;
        _subscribedVoice.PropertyChanged -= OnVoicePropertyChanged;

        foreach (var env in GetVoiceEnvelopes(_subscribedVoice))
        {
            env.PropertyChanged -= OnNestedPropertyChanged;
            env.Segments.CollectionChanged -= OnSegmentsCollectionChanged;
            foreach (var seg in env.Segments)
                seg.PropertyChanged -= OnNestedPropertyChanged;
        }

        _subscribedVoice.Filter.PropertyChanged -= OnNestedPropertyChanged;
        foreach (var partial in _subscribedVoice.Partials)
            partial.PropertyChanged -= OnNestedPropertyChanged;

        _subscribedVoice = null;
    }

    private static EnvelopeViewModel[] GetVoiceEnvelopes(VoiceViewModel v) =>
        [
            v.Pitch,
            v.Volume,
            v.VibratoRate,
            v.VibratoDepth,
            v.TremoloRate,
            v.TremoloDepth,
            v.FilterEnvelope,
            v.GapOff,
            v.GapOn,
        ];

    private void OnVoicePropertyChanged(object? s, PropertyChangedEventArgs e)
    {
        MarkSelectedVoicePreviewDirty();
        ScheduleRerender(immediate: true);
    }

    private void OnNestedPropertyChanged(object? s, PropertyChangedEventArgs e)
    {
        MarkSelectedVoicePreviewDirty();
        ScheduleRerender(immediate: true);
    }

    private void OnSegmentsCollectionChanged(object? s, NotifyCollectionChangedEventArgs e)
    {
        if (e.NewItems is not null)
            foreach (SegmentViewModel seg in e.NewItems)
                seg.PropertyChanged += OnNestedPropertyChanged;
        if (e.OldItems is not null)
            foreach (SegmentViewModel seg in e.OldItems)
                seg.PropertyChanged -= OnNestedPropertyChanged;
        MarkSelectedVoicePreviewDirty();
        ScheduleRerender(immediate: true);
    }

    private void MarkSelectedVoicePreviewDirty()
    {
        lock (_previewCacheLock)
        {
            _ = _dirtyPreviewVoices.Add(Patch.SelectedVoiceIndex);
        }
    }

    #endregion

    #region File operations

    [RelayCommand]
    private void Open() => _ = RequestOpenDialog?.Invoke();

    public bool TryLoadFromPath(string path)
    {
        try
        {
            _fileManager.LoadFromPath(path);
            StatusHint = Loc.Format("StatusLoadedFile", Path.GetFileName(path));
            return true;
        }
        catch (Exception ex)
            when (ex
                    is IOException
                        or UnauthorizedAccessException
                        or InvalidDataException
                        or ArgumentException
            )
        {
            StatusHint = Loc.Format("StatusCouldNotOpenFile", Path.GetFileName(path), ex.Message);
            return false;
        }
    }

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
    private void NavigatePatch(string direction) =>
        _fileManager.NavigatePatch(int.Parse(direction, CultureInfo.InvariantCulture));

    public async Task ExportToPathAsync(string path)
    {
        var model = Patch.ToModel();
        var buffer = await SynthesisService.RenderAsync(model).ConfigureAwait(true);
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
                _bufferStale = true;
            }

            Patch.SelectedVoiceIndex = index;
            if (SelectedEnvelope is not null)
                SelectEnvelope(SelectedSlot);
        }
    }

    public static readonly (
        SignalChainSlot Slot,
        Func<VoiceViewModel, EnvelopeViewModel> Getter
    )[] SignalChain =
    [
        (SignalChainSlot.Pitch, v => v.Pitch),
        (SignalChainSlot.VibratoRate, v => v.VibratoRate),
        (SignalChainSlot.VibratoDepth, v => v.VibratoDepth),
        (SignalChainSlot.Volume, v => v.Volume),
        (SignalChainSlot.TremoloRate, v => v.TremoloRate),
        (SignalChainSlot.TremoloDepth, v => v.TremoloDepth),
        (SignalChainSlot.GapOff, v => v.GapOff),
        (SignalChainSlot.GapOn, v => v.GapOn),
        (SignalChainSlot.Filter, v => v.FilterEnvelope),
    ];

    private static (string Unit, double Min, double Max) GetStartEndMeta(SignalChainSlot slot) =>
        slot switch
        {
            SignalChainSlot.Pitch or SignalChainSlot.VibratoRate or SignalChainSlot.TremoloRate => (
                "Hz",
                -5000,
                5000
            ),
            SignalChainSlot.Volume
            or SignalChainSlot.VibratoDepth
            or SignalChainSlot.TremoloDepth => ("%", -100, 100),
            SignalChainSlot.GapOff or SignalChainSlot.GapOn => ("Gap", -65535, 65535),
            SignalChainSlot.Filter => ("", -65535, 65535),
            SignalChainSlot.PoleZero => ("", -65535, 65535),
            SignalChainSlot.Output => ("", -65535, 65535),
            SignalChainSlot.Bode => ("", -65535, 65535),
            _ => ("", -65535, 65535),
        };

    public string StartEndUnit => GetStartEndMeta(SelectedSlot).Unit;
    public double StartEndMin => GetStartEndMeta(SelectedSlot).Min;
    public double StartEndMax => GetStartEndMeta(SelectedSlot).Max;

    public void SelectEnvelope(SignalChainSlot slot)
    {
        var voice = Patch.SelectedVoice;
        SelectedSlot = slot;

        var (_, getter) = SignalChain.FirstOrDefault(e => e.Slot == slot);
        if (getter is not null)
        {
            SelectedEnvelope = getter(voice);
        }
        else
        {
            SelectedEnvelope = null;
        }

        OnPropertyChanged(nameof(StartEndUnit));
        OnPropertyChanged(nameof(StartEndMin));
        OnPropertyChanged(nameof(StartEndMax));
    }

    public void SelectEnvelopeByOffset(int offset)
    {
        var currentIndex = Array.FindIndex(SignalChain, e => e.Slot == SelectedSlot);
        if (currentIndex < 0)
            currentIndex = 0;
        var newIndex = Math.Clamp(currentIndex + offset, 0, SignalChain.Length - 1);
        SelectEnvelope(SignalChain[newIndex].Slot);
    }

    public void CopyVoice()
    {
        _copiedVoice = Patch.SelectedVoice.ToModel();
        IsCopyMode = true;
    }

    public void ResetVoice()
    {
        Patch.SelectedVoice.Clear();
        _bufferStale = true;
        ScheduleRerender(immediate: true);
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
            buffer = await SynthesizeAndCacheAsync().ConfigureAwait(true);
        }
        else if (_bufferStale)
        {
            buffer = await SynthesizeAndCacheAsync().ConfigureAwait(true);
        }
        else if (_playback.HasWavFile)
        {
            IsPlaying = true;
            _playback.PlayFromCache();
            StartPositionTimer(buffer.Length / (double)buffer.SampleRate);
            return;
        }

        if (buffer is { Length: > 0 })
        {
            IsPlaying = true;
            await _playback.PlayAsync(buffer).ConfigureAwait(true);
            StartPositionTimer(buffer.Length / (double)buffer.SampleRate);
        }
    }

    private async Task<AudioBuffer> SynthesizeAndCacheAsync()
    {
        var model = Patch.ToModel();
        var voiceFilter = PlaySingleVoice ? Patch.SelectedVoiceIndex : -1;

        model = DefaultLoopIfUnset(model);

        var buffer = await SynthesisService
            .RenderAsync(model, loopCount: EffectiveLoopCount, voiceFilter: voiceFilter)
            .ConfigureAwait(true);
        _cachedBuffer = buffer;
        _bufferStale = false;

        // Always compute loop timing from model (loop region may have been defaulted above)
        if (!model.ActiveVoices.IsEmpty)
        {
            var maxDurationMs = model.ActiveVoices.Max(v => v.Voice.DurationMs + v.Voice.OffsetMs);
            _singlePassDuration = maxDurationMs / 1000.0;
            var loopBeginMs = model.Loop.BeginMs;
            var loopEndMs = model.Loop.EndMs;
            if (loopEndMs > loopBeginMs && _singlePassDuration > 0)
            {
                _loopCycleDuration = (loopEndMs - loopBeginMs) / 1000.0;
                var totalMs = _singlePassDuration * 1000.0;
                _loopStartNormalized = loopBeginMs / totalMs;
                _loopEndNormalized = loopEndMs / totalMs;
            }
            else
            {
                _loopCycleDuration = 0;
            }
        }

        return buffer;
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

        if (IsLooping && LoopCount == 0 && _loopCycleDuration > 0)
        {
            if (elapsed < _singlePassDuration)
            {
                // First pass: linear sweep across full waveform
                PlaybackPosition = elapsed / _singlePassDuration;
            }
            else
            {
                // Subsequent passes: cycle through loop region only
                var loopElapsed = elapsed - _singlePassDuration;
                var cyclePos = (loopElapsed % _loopCycleDuration) / _loopCycleDuration;
                PlaybackPosition =
                    _loopStartNormalized + cyclePos * (_loopEndNormalized - _loopStartNormalized);
            }
        }
        else if (IsLooping && LoopCount == 0 && _singlePassDuration > 0)
        {
            // No loop region defined: cycle through full pass
            PlaybackPosition = (elapsed % _singlePassDuration) / _singlePassDuration;
        }
        else
        {
            PlaybackPosition = Math.Clamp(elapsed / _playbackDuration, 0, 1);
            if (PlaybackPosition >= 1.0)
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
            if (
                IsLooping
                && LoopCount == 0
                && !_stoppingManually
                && _cachedBuffer is { Length: > 0 }
            )
            {
                // Silently restart afplay — position continues cycling via modulo
                _playback.ReplayFromExistingFile();
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
