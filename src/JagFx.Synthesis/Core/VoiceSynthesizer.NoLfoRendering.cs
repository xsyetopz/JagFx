using JagFx.Core.Constants;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    private static void RenderSquareSamplesNoLfo(
        int[] buffer,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
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
            RenderSquarePartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }

    private static void RenderSineSamplesNoLfo(
        int[] buffer,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
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
            RenderSinePartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }

    private static void RenderSawSamplesNoLfo(
        int[] buffer,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
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
            RenderSawPartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }

    private static void RenderNoiseSamplesNoLfo(
        int[] buffer,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sampleCount,
        CancellationToken ct
    )
    {
        Span<int> phases = stackalloc int[AudioConstants.MaxOscillators];
        var activePartialCount = state.PartialCount;
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
            RenderNoisePartials(
                buffer,
                activePartialCount,
                partialDelays,
                partialVolumes,
                partialSemitones,
                partialStarts,
                sample,
                frequency,
                amplitude,
                phases
            );
        }
    }

    private static void RenderGatedSquareSamplesNoLfo(
        int[] buffer,
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

    private static void RenderGatedSineSamplesNoLfo(
        int[] buffer,
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
            RenderGatedSinePartials(
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

    private static void RenderGatedSawSamplesNoLfo(
        int[] buffer,
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
            RenderGatedSawPartials(
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

    private static void RenderGatedNoiseSamplesNoLfo(
        int[] buffer,
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
            RenderGatedNoisePartials(
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
