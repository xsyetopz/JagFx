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
    public static Waveform FromId(int id) => id switch
    {
        1 => Waveform.Square,
        2 => Waveform.Sine,
        3 => Waveform.Saw,
        4 => Waveform.Noise,
        _ => Waveform.Off
    };
}
