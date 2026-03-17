using System.Collections.Immutable;
namespace JagFx.Domain.Models;

/// <summary>A single voice within a synthesizer patch.</summary>
/// <param name="DurationMs">Voice duration in milliseconds.</param>
/// <param name="OffsetMs">Voice start offset in milliseconds.</param>
public record class Voice(
    Envelope FrequencyEnvelope,
    Envelope AmplitudeEnvelope,
    LowFrequencyOscillator? PitchLfo,
    LowFrequencyOscillator? AmplitudeLfo,
    Envelope? GapOffEnvelope,
    Envelope? GapOnEnvelope,
    ImmutableList<Partial> Partials,
    Echo Echo,
    int DurationMs,
    int OffsetMs,
    Filter? Filter = null
);
