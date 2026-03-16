using System.Collections.Immutable;
namespace JagFx.Domain.Models;

public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    LowFrequencyOscillator? PitchLfo,
    LowFrequencyOscillator? AmplitudeLfo,
    Envelope? GapOffEnvelope,
    Envelope? GapOnEnvelope,
    ImmutableList<Partial> Partials,
    Echo Echo,
    /// <summary>Voice duration in milliseconds.</summary>
    int DurationMs,
    /// <summary>Voice start offset in milliseconds.</summary>
    int OffsetMs,
    Filter? Filter = null
);
