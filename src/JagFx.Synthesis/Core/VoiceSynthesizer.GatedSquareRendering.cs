using JagFx.Core.Constants;
using JagFx.Domain.Models;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    private static void RenderGatedSquareSamples(
        int[] buffer,
        Voice voice,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sampleCount,
        CancellationToken ct
    )
    {
        var pitchLfoEnabled =
            state.FrequencyModulationRateEval != null && state.FrequencyModulationRangeEval != null;
        var amplitudeLfoEnabled =
            state.AmplitudeModulationRateEval != null && state.AmplitudeModulationRangeEval != null;

        if (pitchLfoEnabled && amplitudeLfoEnabled)
        {
            RenderGatedSquareSamplesPitchAmplitudeLfo(
                buffer,
                voice,
                state,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                gateMask,
                sampleCount,
                ct
            );
            return;
        }

        if (pitchLfoEnabled)
        {
            RenderGatedSquareSamplesPitchLfo(
                buffer,
                voice,
                state,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                gateMask,
                sampleCount,
                ct
            );
            return;
        }

        if (amplitudeLfoEnabled)
        {
            RenderGatedSquareSamplesAmplitudeLfo(
                buffer,
                voice,
                state,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                gateMask,
                sampleCount,
                ct
            );
            return;
        }

        RenderGatedSquareSamplesNoLfo(
            buffer,
            state,
            partialDelays,
            partialVolumes,
            partialSemitones,
            partialStarts,
            gateMask,
            sampleCount,
            ct
        );
    }

    private static void RenderGatedSquareSamplesPitchLfo(
        int[] buffer,
        Voice voice,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
        var frequencyPhase = 0;
        var pitchLfoWaveform = voice.PitchLfo!.RateEnvelope.Waveform;

        for (var sample = 0; sample < sampleCount; sample++)
        {
            if ((sample & 0xFF) == 0)
            {
                ct.ThrowIfCancellationRequested();
            }

            while (
                activePartialCount > 0
                && sample + partialDelays[activePartialCount - 1] >= sampleCount
            )
            {
                activePartialCount--;
            }

            var frequency = state.FrequencyBaseEval.Evaluate(sampleCount);
            var amplitude = state.AmplitudeBaseEval.Evaluate(sampleCount);

            var (pitchMod, nextFrequencyPhase) = EvaluateModulation(
                state.FrequencyModulationRateEval!,
                state.FrequencyModulationRangeEval!,
                state.FrequencyBase,
                state.FrequencyStep,
                frequencyPhase,
                sampleCount,
                pitchLfoWaveform
            );
            frequency += pitchMod;
            frequencyPhase = nextFrequencyPhase;

            RenderGatedSquarePartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                gateMask,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }

    private static void RenderGatedSquareSamplesAmplitudeLfo(
        int[] buffer,
        Voice voice,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
        var amplitudePhase = 0;
        var amplitudeLfoWaveform = voice.AmplitudeLfo!.RateEnvelope.Waveform;

        for (var sample = 0; sample < sampleCount; sample++)
        {
            if ((sample & 0xFF) == 0)
            {
                ct.ThrowIfCancellationRequested();
            }

            while (
                activePartialCount > 0
                && sample + partialDelays[activePartialCount - 1] >= sampleCount
            )
            {
                activePartialCount--;
            }

            var frequency = state.FrequencyBaseEval.Evaluate(sampleCount);
            var amplitude = state.AmplitudeBaseEval.Evaluate(sampleCount);

            var (amplitudeMod, nextAmplitudePhase) = EvaluateModulation(
                state.AmplitudeModulationRateEval!,
                state.AmplitudeModulationRangeEval!,
                state.AmplitudeBase,
                state.AmplitudeStep,
                amplitudePhase,
                sampleCount,
                amplitudeLfoWaveform
            );
            amplitude = amplitude * (amplitudeMod + AudioConstants.FixedPoint.Offset) >> 15;
            amplitudePhase = nextAmplitudePhase;

            RenderGatedSquarePartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                gateMask,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }

    private static void RenderGatedSquareSamplesPitchAmplitudeLfo(
        int[] buffer,
        Voice voice,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        ReadOnlySpan<byte> gateMask,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
        var frequencyPhase = 0;
        var pitchLfoWaveform = voice.PitchLfo!.RateEnvelope.Waveform;
        var amplitudePhase = 0;
        var amplitudeLfoWaveform = voice.AmplitudeLfo!.RateEnvelope.Waveform;

        for (var sample = 0; sample < sampleCount; sample++)
        {
            if ((sample & 0xFF) == 0)
            {
                ct.ThrowIfCancellationRequested();
            }

            while (
                activePartialCount > 0
                && sample + partialDelays[activePartialCount - 1] >= sampleCount
            )
            {
                activePartialCount--;
            }

            var frequency = state.FrequencyBaseEval.Evaluate(sampleCount);
            var amplitude = state.AmplitudeBaseEval.Evaluate(sampleCount);

            var (pitchMod, nextFrequencyPhase) = EvaluateModulation(
                state.FrequencyModulationRateEval!,
                state.FrequencyModulationRangeEval!,
                state.FrequencyBase,
                state.FrequencyStep,
                frequencyPhase,
                sampleCount,
                pitchLfoWaveform
            );
            frequency += pitchMod;
            frequencyPhase = nextFrequencyPhase;

            var (amplitudeMod, nextAmplitudePhase) = EvaluateModulation(
                state.AmplitudeModulationRateEval!,
                state.AmplitudeModulationRangeEval!,
                state.AmplitudeBase,
                state.AmplitudeStep,
                amplitudePhase,
                sampleCount,
                amplitudeLfoWaveform
            );
            amplitude = amplitude * (amplitudeMod + AudioConstants.FixedPoint.Offset) >> 15;
            amplitudePhase = nextAmplitudePhase;

            RenderGatedSquarePartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                gateMask,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }
}
