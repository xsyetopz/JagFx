namespace JagFX.Domain.Models;

public record class LowFrequencyOscillator(
    Envelope RateEnvelope,
    Envelope ModulationDepth
);
