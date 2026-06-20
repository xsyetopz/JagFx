using System.Collections.Immutable;

namespace JagFx.Domain.Models;

public record Filter(
    ImmutableArray<int> PoleCounts,
    ImmutableArray<int> UnityGain,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PolePhase,
    ImmutableArray<ImmutableArray<ImmutableArray<int>>> PoleMagnitude,
    Envelope? ModulationEnvelope
)
{
    public ImmutableArray<int> FlatPolePhase { get; } = FlattenPoles(PolePhase);
    public ImmutableArray<int> FlatPoleMagnitude { get; } = FlattenPoles(PoleMagnitude);

    public int GetPolePhase(int direction, int phase, int pole) =>
        FlatPolePhase[GetPoleIndex(direction, phase, pole)];

    public int GetPoleMagnitude(int direction, int phase, int pole) =>
        FlatPoleMagnitude[GetPoleIndex(direction, phase, pole)];

    private static int GetPoleIndex(int direction, int phase, int pole) =>
        ((direction * 2) + phase) * 4 + pole;

    private static ImmutableArray<int> FlattenPoles(
        ImmutableArray<ImmutableArray<ImmutableArray<int>>> source
    )
    {
        var builder = ImmutableArray.CreateBuilder<int>(16);
        for (var direction = 0; direction < 2; direction++)
        {
            for (var phase = 0; phase < 2; phase++)
            {
                var poles = source[direction][phase];
                for (var pole = 0; pole < 4; pole++)
                {
                    builder.Add(pole < poles.Length ? poles[pole] : 0);
                }
            }
        }

        return builder.MoveToImmutable();
    }
}
