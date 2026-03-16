using JagFx.Core.Constants;

namespace JagFx.Core.Types;

/// <summary>
/// Represents a count of audio samples.
/// Provides conversion to and from milliseconds.
/// </summary>
public readonly record struct Samples
{
    public int Value { get; }

    public Samples(int value) => Value = value;

    public Milliseconds ToMilliseconds() => new((int)(Value / AudioConstants.SampleRatePerMillisecond));
}
