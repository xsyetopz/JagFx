using JagFx.Core.Types;
namespace JagFx.Domain.Models;

public enum Waveform
{
    Off = 0,
    Square = 1,
    Sine = 2,
    Saw = 3,
    Noise = 4
}

public record class Partial(Percent Amplitude, int PitchOffsetSemitones, Milliseconds Delay);

public static class WaveformExtensions
{
    public static readonly Dictionary<Waveform, string> Names = new()
    {
        [Waveform.Off] = "off",
        [Waveform.Square] = "square",
        [Waveform.Sine] = "sine",
        [Waveform.Saw] = "saw",
        [Waveform.Noise] = "noise",
    };

    private static readonly Dictionary<string, Waveform> _byName =
        Names.ToDictionary(kv => kv.Value, kv => kv.Key);

    public static Waveform FromId(int id) =>
        (id >= 1 && id <= 4) ? (Waveform)id : Waveform.Off;

    public static Waveform FromName(string name) =>
        _byName.TryGetValue(name.ToLowerInvariant(), out var w) ? w : Waveform.Off;
}
