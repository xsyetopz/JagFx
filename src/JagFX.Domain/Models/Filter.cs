using System.Collections.Immutable;
namespace JagFx.Domain.Models;

public record Filter(
    ImmutableArray<int> PoleCounts,
    ImmutableArray<int> UnityGain,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PolePhase,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PoleMagnitude,
    Envelope? ModulationEnvelope
);
