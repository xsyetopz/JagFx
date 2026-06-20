using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Synthesis.Audio;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    private static int GenerateSample(int amplitude, int phase, Waveform waveform)
    {
        return waveform switch
        {
            Waveform.Square => (phase & AudioConstants.PhaseMask)
            < AudioConstants.FixedPoint.Quarter
                ? amplitude
                : -amplitude,
            Waveform.Sine => (
                WaveformTables.SineWaveTable[phase & AudioConstants.PhaseMask] * amplitude
            ) >> 14,
            Waveform.Saw => (((phase & AudioConstants.PhaseMask) * amplitude) >> 14) - amplitude,
            Waveform.Noise => WaveformTables.NoiseTable[
                (phase / AudioConstants.NoisePhaseDiv) & AudioConstants.PhaseMask
            ] * amplitude,
            Waveform.Off => 0,
            _ => 0,
        };
    }

    private static void ApplyEcho(
        int[] buffer,
        Voice voice,
        double samplesPerStep,
        int sampleCount,
        CancellationToken ct
    )
    {
        if (voice.Echo.DelayMilliseconds > 0 && voice.Echo.FeedbackPercent > 0)
        {
            var start = (int)(voice.Echo.DelayMilliseconds * samplesPerStep);
            for (var sample = start; sample < sampleCount; sample++)
            {
                if ((sample & 0xFF) == 0)
                {
                    ct.ThrowIfCancellationRequested();
                }

                buffer[sample] += buffer[sample - start] * voice.Echo.FeedbackPercent / 100;
            }
        }
    }

    internal readonly struct PooledVoiceBuffer(int[] samples, int length)
    {
        public int[] Samples { get; } = samples;
        public int Length { get; } = length;
    }
}
