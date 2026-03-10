namespace JagFx.Domain.Models;

public record class LowFrequencyOscillator(
    Envelope RateEnvelope,
    Envelope ModulationDepth
);
