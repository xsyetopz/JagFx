using Avalonia.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using JagFx.Desktop.Services;
using JagFx.Domain.Models;
using JagFx.Synthesis.Audio;

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
        Loc.Format("SelectedEnvelopeTitle", Loc.Get($"Slot{SelectedSlot}"))
            .ToUpper(Loc.CurrentCulture);

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
            {
                return $"JagFx: {PatchName}{dirty}";
            }

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
        {
            _interactiveEditDepth--;
        }

        if (_interactiveEditDepth == 0)
        {
            ScheduleRerender(immediate: true);
        }
    }
}
