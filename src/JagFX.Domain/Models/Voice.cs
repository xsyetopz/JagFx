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
    int DurationMs,
    int OffsetMs,
    Filter? Filter = null
);
