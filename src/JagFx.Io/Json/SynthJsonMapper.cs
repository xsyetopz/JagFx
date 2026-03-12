using System.Collections.Immutable;
using JagFx.Core.Types;
using JagFx.Domain.Models;

namespace JagFx.Io.Json;

public static class SynthJsonMapper
{
    public static PatchJson ToJson(Patch patch)
    {
        var voices = patch.Voices.Select(v => v != null ? VoiceToJson(v) : null).ToList();

        // Trim trailing nulls
        while (voices.Count > 0 && voices[^1] == null)
            voices.RemoveAt(voices.Count - 1);

        var loop = patch.Loop is { BeginMs: 0, EndMs: 0 }
            ? null
            : new LoopSegmentJson { BeginMs = patch.Loop.BeginMs, EndMs = patch.Loop.EndMs };

        return new PatchJson { Voices = voices, Loop = loop };
    }

    public static Patch FromJson(PatchJson json)
    {
        var voices = new List<Voice?>();
        foreach (var v in json.Voices)
            voices.Add(v != null ? VoiceFromJson(v) : null);

        var loop = json.Loop != null
            ? new LoopSegment(json.Loop.BeginMs, json.Loop.EndMs)
            : new LoopSegment(0, 0);

        return new Patch([.. voices], loop, warnings: null);
    }

    private static VoiceJson VoiceToJson(Voice voice) => new()
    {
        FrequencyEnvelope = EnvelopeToJson(voice.FrequencyEnvelope),
        AmplitudeEnvelope = EnvelopeToJson(voice.AmplitudeEnvelope),
        PitchLfo = voice.PitchLfo != null ? LfoToJson(voice.PitchLfo) : null,
        AmplitudeLfo = voice.AmplitudeLfo != null ? LfoToJson(voice.AmplitudeLfo) : null,
        GapOffEnvelope = voice.GapOffEnvelope != null ? EnvelopeToJson(voice.GapOffEnvelope) : null,
        GapOnEnvelope = voice.GapOnEnvelope != null ? EnvelopeToJson(voice.GapOnEnvelope) : null,
        Partials = voice.Partials.Count > 0
            ? voice.Partials.Select(PartialToJson).ToList()
            : null,
        Echo = voice.Echo is { DelayMilliseconds: 0, FeedbackPercent: 0 }
            ? null
            : new EchoJson
            {
                DelayMilliseconds = voice.Echo.DelayMilliseconds,
                FeedbackPercent = voice.Echo.FeedbackPercent
            },
        DurationMs = voice.DurationMs,
        OffsetMs = voice.OffsetMs,
        Filter = voice.Filter != null ? FilterToJson(voice.Filter) : null
    };

    private static Voice VoiceFromJson(VoiceJson json) => new(
        FrequencyEnvelope: EnvelopeFromJson(json.FrequencyEnvelope),
        AmplitudeEnvelope: EnvelopeFromJson(json.AmplitudeEnvelope),
        PitchLfo: json.PitchLfo != null ? LfoFromJson(json.PitchLfo) : null,
        AmplitudeLfo: json.AmplitudeLfo != null ? LfoFromJson(json.AmplitudeLfo) : null,
        GapOffEnvelope: json.GapOffEnvelope != null ? EnvelopeFromJson(json.GapOffEnvelope) : null,
        GapOnEnvelope: json.GapOnEnvelope != null ? EnvelopeFromJson(json.GapOnEnvelope) : null,
        Partials: json.Partials != null
            ? [.. json.Partials.Select(PartialFromJson)]
            : [],
        Echo: json.Echo != null
            ? new Echo(json.Echo.DelayMilliseconds, json.Echo.FeedbackPercent)
            : new Echo(0, 0),
        DurationMs: json.DurationMs,
        OffsetMs: json.OffsetMs,
        Filter: json.Filter != null ? FilterFromJson(json.Filter) : null
    );

    private static EnvelopeJson EnvelopeToJson(Envelope envelope) => new()
    {
        Waveform = WaveformToString(envelope.Waveform),
        StartValue = envelope.StartValue,
        EndValue = envelope.EndValue,
        Segments = envelope.Segments.Select(s => new SegmentJson
        {
            Duration = s.Duration,
            TargetLevel = s.TargetLevel
        }).ToList()
    };

    private static Envelope EnvelopeFromJson(EnvelopeJson json) => new(
        Waveform: WaveformFromString(json.Waveform),
        StartValue: json.StartValue,
        EndValue: json.EndValue,
        Segments: [.. json.Segments.Select(s => new Segment(s.Duration, s.TargetLevel))]
    );

    private static LfoJson LfoToJson(LowFrequencyOscillator lfo) => new()
    {
        RateEnvelope = EnvelopeToJson(lfo.RateEnvelope),
        ModulationDepth = EnvelopeToJson(lfo.ModulationDepth)
    };

    private static LowFrequencyOscillator LfoFromJson(LfoJson json) => new(
        RateEnvelope: EnvelopeFromJson(json.RateEnvelope),
        ModulationDepth: EnvelopeFromJson(json.ModulationDepth)
    );

    private static PartialJson PartialToJson(Partial partial) => new()
    {
        Amplitude = partial.Amplitude.Value,
        PitchOffsetSemitones = partial.PitchOffsetSemitones,
        Delay = partial.Delay.Value
    };

    private static Partial PartialFromJson(PartialJson json) => new(
        Amplitude: new Percent(json.Amplitude),
        PitchOffsetSemitones: json.PitchOffsetSemitones,
        Delay: new Milliseconds(json.Delay)
    );

    private static FilterJson FilterToJson(Filter filter)
    {
        var polePhase = CoefficientsToJson(filter.PolePhase);
        var poleMagnitude = CoefficientsToJson(filter.PoleMagnitude);

        return new FilterJson
        {
            PoleCounts = [filter.PoleCounts[0], filter.PoleCounts[1]],
            UnityGain = [filter.UnityGain[0], filter.UnityGain[1]],
            PolePhase = polePhase,
            PoleMagnitude = poleMagnitude,
            ModulationEnvelope = filter.ModulationEnvelope != null
                ? EnvelopeToJson(filter.ModulationEnvelope)
                : null
        };
    }

    private static Filter FilterFromJson(FilterJson json)
    {
        var poleCounts = ImmutableArray.Create(json.PoleCounts[0], json.PoleCounts[1]);
        var unityGain = ImmutableArray.Create(json.UnityGain[0], json.UnityGain[1]);

        var polePhase = CoefficientsFromJson(json.PolePhase);
        var poleMagnitude = CoefficientsFromJson(json.PoleMagnitude);

        var modulationEnvelope = json.ModulationEnvelope != null
            ? EnvelopeFromJson(json.ModulationEnvelope)
            : null;

        return new Filter(poleCounts, unityGain, polePhase, poleMagnitude, modulationEnvelope);
    }

    private static FilterCoefficientsJson CoefficientsToJson(
        ImmutableArray<ImmutableArray<ImmutableArray<int>>> coefficients) => new()
    {
        Feedforward = new FilterPhasesJson
        {
            Baseline = [.. coefficients[0][0]],
            Modulated = [.. coefficients[0][1]]
        },
        Feedback = new FilterPhasesJson
        {
            Baseline = [.. coefficients[1][0]],
            Modulated = [.. coefficients[1][1]]
        }
    };

    private static ImmutableArray<ImmutableArray<ImmutableArray<int>>> CoefficientsFromJson(
        FilterCoefficientsJson json)
    {
        var channel0 = ImmutableArray.Create(
            [.. json.Feedforward.Baseline],
            ImmutableArray.CreateRange(json.Feedforward.Modulated)
        );
        var channel1 = ImmutableArray.Create(
            [.. json.Feedback.Baseline],
            ImmutableArray.CreateRange(json.Feedback.Modulated)
        );
        return ImmutableArray.Create(channel0, channel1);
    }

    private static string WaveformToString(Waveform waveform) => waveform switch
    {
        Waveform.Off => "off",
        Waveform.Square => "square",
        Waveform.Sine => "sine",
        Waveform.Saw => "saw",
        Waveform.Noise => "noise",
        _ => "off"
    };

    private static Waveform WaveformFromString(string waveform) => waveform.ToLowerInvariant() switch
    {
        "off" => Waveform.Off,
        "square" => Waveform.Square,
        "sine" => Waveform.Sine,
        "saw" => Waveform.Saw,
        "noise" => Waveform.Noise,
        _ => Waveform.Off
    };
}
