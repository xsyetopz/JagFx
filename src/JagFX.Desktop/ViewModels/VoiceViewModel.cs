using System.Collections.Immutable;
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using JagFX.Core.Constants;
using JagFX.Domain.Models;

namespace JagFX.Desktop.ViewModels;

public partial class VoiceViewModel : ObservableObject
{
    [ObservableProperty]
    private bool _isEnabled;

    [ObservableProperty]
    private int _index;

    [ObservableProperty]
    private bool _isSelected;

    public int DisplayIndex => Index + 1;

    [ObservableProperty]
    private int _durationMs = 1000;

    [ObservableProperty]
    private int _offsetMs;

    [ObservableProperty]
    private int _echoDelay;

    [ObservableProperty]
    private int _echoFeedback;

    public EnvelopeViewModel Pitch { get; } = new();
    public EnvelopeViewModel Volume { get; } = new();
    public EnvelopeViewModel VibratoRate { get; } = new();
    public EnvelopeViewModel VibratoDepth { get; } = new();
    public EnvelopeViewModel TremoloRate { get; } = new();
    public EnvelopeViewModel TremoloDepth { get; } = new();
    public EnvelopeViewModel GapOff { get; } = new();
    public EnvelopeViewModel GapOn { get; } = new();
    public EnvelopeViewModel FilterEnvelope { get; } = new();
    public FilterViewModel Filter { get; } = new();

    [ObservableProperty]
    private int _partialBankOffset;

    public IEnumerable<PartialViewModel> VisiblePartials =>
        Partials.Skip(PartialBankOffset).Take(5);

    partial void OnPartialBankOffsetChanged(int value) =>
        OnPropertyChanged(nameof(VisiblePartials));

    public ObservableCollection<PartialViewModel> Partials { get; }

    public VoiceViewModel()
    {
        Partials = new ObservableCollection<PartialViewModel>(
            Enumerable.Range(0, AudioConstants.MaxOscillators)
                .Select(_ => new PartialViewModel()));
    }

    public void Load(Voice? voice)
    {
        if (voice is null)
        {
            Clear();
            return;
        }

        IsEnabled = true;
        DurationMs = voice.DurationMs;
        OffsetMs = voice.OffsetMs;
        EchoDelay = voice.Echo.DelayMilliseconds;
        EchoFeedback = voice.Echo.FeedbackPercent;

        Pitch.Load(voice.FrequencyEnvelope);
        Volume.Load(voice.AmplitudeEnvelope);

        ClearModulationEnvelopes();

        if (voice.PitchLfo is { } pitchLfo)
        {
            VibratoRate.Load(pitchLfo.RateEnvelope);
            VibratoDepth.Load(pitchLfo.ModulationDepth);
        }

        if (voice.AmplitudeLfo is { } ampLfo)
        {
            TremoloRate.Load(ampLfo.RateEnvelope);
            TremoloDepth.Load(ampLfo.ModulationDepth);
        }

        if (voice.GapOffEnvelope is { } gapOff)
            GapOff.Load(gapOff);

        if (voice.GapOnEnvelope is { } gapOn)
            GapOn.Load(gapOn);

        Filter.Load(voice.Filter);
        if (voice.Filter?.ModulationEnvelope is { } modEnv)
            FilterEnvelope.Load(modEnv);

        for (var i = 0; i < AudioConstants.MaxOscillators; i++)
        {
            if (i < voice.Partials.Count)
                Partials[i].Load(voice.Partials[i]);
            else
                Partials[i].Clear();
        }
    }

    public void Clear()
    {
        IsEnabled = false;
        DurationMs = 1000;
        OffsetMs = 0;
        EchoDelay = 0;
        EchoFeedback = 0;

        Pitch.Clear();
        Volume.Clear();
        ClearModulationEnvelopes();
        Filter.Clear();

        foreach (var p in Partials)
            p.Clear();
    }

    public Voice? ToModel()
    {
        if (!IsEnabled) return null;

        var activePartials = Partials
            .Where(p => p.IsActive)
            .Select(p => p.ToModel())
            .ToImmutableList();

        var pitchLfo = (!VibratoRate.IsEmpty && !VibratoDepth.IsEmpty)
            ? new LowFrequencyOscillator(VibratoRate.ToModel(), VibratoDepth.ToModel())
            : null;

        var ampLfo = (!TremoloRate.IsEmpty && !TremoloDepth.IsEmpty)
            ? new LowFrequencyOscillator(TremoloRate.ToModel(), TremoloDepth.ToModel())
            : null;

        var gapOff = GapOff.IsEmpty ? null : GapOff.ToModel();
        var gapOn = GapOn.IsEmpty ? null : GapOn.ToModel();

        var filter = Filter.ToModel();
        if (filter is not null && !FilterEnvelope.IsEmpty)
        {
            filter = filter with { ModulationEnvelope = FilterEnvelope.ToModel() };
        }

        return new Voice(
            Pitch.ToModel(),
            Volume.ToModel(),
            pitchLfo,
            ampLfo,
            gapOff,
            gapOn,
            activePartials,
            new Echo(EchoDelay, EchoFeedback),
            DurationMs,
            OffsetMs,
            filter);
    }

    private void ClearModulationEnvelopes()
    {
        VibratoRate.Clear();
        VibratoDepth.Clear();
        TremoloRate.Clear();
        TremoloDepth.Clear();
        GapOff.Clear();
        GapOn.Clear();
        FilterEnvelope.Clear();
    }
}
