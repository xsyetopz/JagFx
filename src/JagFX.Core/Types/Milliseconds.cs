using JagFx.Core.Constants;

namespace JagFx.Core.Types;

/// <summary>
/// Represents a time duration in milliseconds.
/// Provides conversion to and from samples.
/// </summary>
public readonly record struct Milliseconds
{
    public int Value { get; }

    public Milliseconds(int value) => Value = value;

    public Milliseconds ToMilliseconds() => new(Value * AudioConstants.MillisecondsPerSample);

    public Samples ToSamples() => new((int)(Value * AudioConstants.SampleRatePerMillisecond));
}
