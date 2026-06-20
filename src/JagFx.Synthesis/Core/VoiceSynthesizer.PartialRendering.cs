using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Synthesis.Audio;
using JagFx.Synthesis.Processing;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    private static void RenderSquarePartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
            buffer[position] +=
                (phases[partial] & AudioConstants.PhaseMask) < AudioConstants.FixedPoint.Quarter
                    ? sampleAmplitude
                    : -sampleAmplitude;
            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderSinePartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        var sineTable = WaveformTables.SineWaveTable;
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
            buffer[position] +=
                (sineTable[phases[partial] & AudioConstants.PhaseMask] * sampleAmplitude) >> 14;
            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderSawPartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
            buffer[position] +=
                (((phases[partial] & AudioConstants.PhaseMask) * sampleAmplitude) >> 14)
                - sampleAmplitude;
            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderNoisePartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        var noiseTable = WaveformTables.NoiseTable;
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
            buffer[position] +=
                noiseTable[
                    (phases[partial] / AudioConstants.NoisePhaseDiv) & AudioConstants.PhaseMask
                ] * sampleAmplitude;
            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderGatedSquarePartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            if (gateMask[position] != 0)
            {
                var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
                buffer[position] +=
                    (phases[partial] & AudioConstants.PhaseMask) < AudioConstants.FixedPoint.Quarter
                        ? sampleAmplitude
                        : -sampleAmplitude;
            }

            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderGatedSinePartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        var sineTable = WaveformTables.SineWaveTable;
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            if (gateMask[position] != 0)
            {
                var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
                buffer[position] +=
                    (sineTable[phases[partial] & AudioConstants.PhaseMask] * sampleAmplitude) >> 14;
            }

            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderGatedSawPartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            if (gateMask[position] != 0)
            {
                var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
                buffer[position] +=
                    (((phases[partial] & AudioConstants.PhaseMask) * sampleAmplitude) >> 14)
                    - sampleAmplitude;
            }

            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static void RenderGatedNoisePartials(
        int[] buffer,
        int partialCount,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sample,
        int frequency,
        int amplitude,
        Span<int> phases
    )
    {
        var noiseTable = WaveformTables.NoiseTable;
        for (var partial = 0; partial < partialCount; partial++)
        {
            var position = sample + partialDelays[partial];
            if (gateMask[position] != 0)
            {
                var sampleAmplitude = amplitude * partialVolumes[partial] >> 15;
                buffer[position] +=
                    noiseTable[
                        (phases[partial] / AudioConstants.NoisePhaseDiv) & AudioConstants.PhaseMask
                    ] * sampleAmplitude;
            }

            phases[partial] +=
                (frequency * partialSemitones[partial] >> 16) + partialStarts[partial];
        }
    }

    private static (int mod, int nextPhase) EvaluateModulation(
        EnvelopeGenerator rateEval,
        EnvelopeGenerator rangeEval,
        int baseValue,
        int step,
        int phase,
        int sampleCount,
        Waveform waveform
    )
    {
        var rate = rateEval.Evaluate(sampleCount);
        var range = rangeEval.Evaluate(sampleCount);
        var mod = GenerateSample(range, phase, waveform) >> 1;
        var nextPhase = phase + baseValue + (rate * step >> 16);
        return (mod, nextPhase);
    }
}
