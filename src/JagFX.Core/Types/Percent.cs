namespace JagFx.Core.Types;

/// <summary>
/// Represents a percentage value (0-100).
/// Used for amplitude and other relative values.
/// </summary>
public readonly record struct Percent
{
    public int Value { get; }

    public Percent(int value) => Value = value;
}
