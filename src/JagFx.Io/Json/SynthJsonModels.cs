using System.Text.Json.Serialization;

namespace JagFx.Io.Json;

public record PatchJson
{
    [JsonPropertyName("voices")]
    public required List<VoiceJson?> Voices { get; init; }

    [JsonPropertyName("loop")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public LoopSegmentJson? Loop { get; init; }
}

public record VoiceJson
{
    [JsonPropertyName("frequencyEnvelope")]
    public required EnvelopeJson FrequencyEnvelope { get; init; }

    [JsonPropertyName("amplitudeEnvelope")]
    public required EnvelopeJson AmplitudeEnvelope { get; init; }

    [JsonPropertyName("pitchLfo")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public LfoJson? PitchLfo { get; init; }

    [JsonPropertyName("amplitudeLfo")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public LfoJson? AmplitudeLfo { get; init; }

    [JsonPropertyName("gapOffEnvelope")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public EnvelopeJson? GapOffEnvelope { get; init; }

    [JsonPropertyName("gapOnEnvelope")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public EnvelopeJson? GapOnEnvelope { get; init; }

    [JsonPropertyName("partials")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public List<PartialJson>? Partials { get; init; }

    [JsonPropertyName("echo")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public EchoJson? Echo { get; init; }

    [JsonPropertyName("durationMs")]
    public required int DurationMs { get; init; }

    [JsonPropertyName("offsetMs")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingDefault)]
    public int OffsetMs { get; init; }

    [JsonPropertyName("filter")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public FilterJson? Filter { get; init; }
}

public record EnvelopeJson
{
    [JsonPropertyName("waveform")]
    public required string Waveform { get; init; }

    [JsonPropertyName("startValue")]
    public required int StartValue { get; init; }

    [JsonPropertyName("endValue")]
    public required int EndValue { get; init; }

    [JsonPropertyName("segments")]
    public required List<SegmentJson> Segments { get; init; }
}

public record SegmentJson
{
    [JsonPropertyName("duration")]
    public required int Duration { get; init; }

    [JsonPropertyName("targetLevel")]
    public required int TargetLevel { get; init; }
}

public record LfoJson
{
    [JsonPropertyName("rateEnvelope")]
    public required EnvelopeJson RateEnvelope { get; init; }

    [JsonPropertyName("modulationDepth")]
    public required EnvelopeJson ModulationDepth { get; init; }
}

public record PartialJson
{
    [JsonPropertyName("amplitude")]
    public required int Amplitude { get; init; }

    [JsonPropertyName("pitchOffsetSemitones")]
    public required int PitchOffsetSemitones { get; init; }

    [JsonPropertyName("delay")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingDefault)]
    public int Delay { get; init; }
}

public record EchoJson
{
    [JsonPropertyName("delayMilliseconds")]
    public required int DelayMilliseconds { get; init; }

    [JsonPropertyName("feedbackPercent")]
    public required int FeedbackPercent { get; init; }
}

public record FilterJson
{
    [JsonPropertyName("poleCounts")]
    public required List<int> PoleCounts { get; init; }

    [JsonPropertyName("unityGain")]
    public required List<int> UnityGain { get; init; }

    [JsonPropertyName("polePhase")]
    public required FilterCoefficientsJson PolePhase { get; init; }

    [JsonPropertyName("poleMagnitude")]
    public required FilterCoefficientsJson PoleMagnitude { get; init; }

    [JsonPropertyName("modulationEnvelope")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public EnvelopeJson? ModulationEnvelope { get; init; }
}

public record FilterCoefficientsJson
{
    [JsonPropertyName("feedforward")]
    public required FilterPhasesJson Feedforward { get; init; }

    [JsonPropertyName("feedback")]
    public required FilterPhasesJson Feedback { get; init; }
}

public record FilterPhasesJson
{
    [JsonPropertyName("baseline")]
    public required List<int> Baseline { get; init; }

    [JsonPropertyName("modulated")]
    public required List<int> Modulated { get; init; }
}

public record LoopSegmentJson
{
    [JsonPropertyName("beginMs")]
    public required int BeginMs { get; init; }

    [JsonPropertyName("endMs")]
    public required int EndMs { get; init; }
}
