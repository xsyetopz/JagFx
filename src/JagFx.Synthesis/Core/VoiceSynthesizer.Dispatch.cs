using JagFx.Domain.Models;

namespace JagFx.Synthesis.Core;

public static partial class VoiceSynthesizer
{
    private static void RenderGatedSamples(
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
        if (state.PartialCount == 0)
        {
            return;
        }

        switch (voice.FrequencyEnvelope.Waveform)
        {
            case Waveform.Square:
                RenderGatedSquareSamples(
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
                break;
            case Waveform.Sine:
                RenderGatedSineSamples(
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
                break;
            case Waveform.Saw:
                RenderGatedSawSamples(
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
                break;
            case Waveform.Noise:
                RenderGatedNoiseSamples(
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
                break;
            case Waveform.Off:
            default:
                break;
        }
    }

    private static void RenderSamples(
        int[] buffer,
        Voice voice,
        VoiceRenderState state,
        ReadOnlySpan<int> partialDelays,
        ReadOnlySpan<int> partialVolumes,
        ReadOnlySpan<int> partialSemitones,
        ReadOnlySpan<int> partialStarts,
        int sampleCount,
        CancellationToken ct
    )
    {
        if (state.PartialCount == 0)
        {
            return;
        }

        switch (voice.FrequencyEnvelope.Waveform)
        {
            case Waveform.Square:
                RenderSquareSamples(
                    buffer,
                    voice,
                    state,
                    partialDelays,
                    partialVolumes,
                    partialSemitones,
                    partialStarts,
                    sampleCount,
                    ct
                );
                break;
            case Waveform.Sine:
                RenderSineSamples(
                    buffer,
                    voice,
                    state,
                    partialDelays,
                    partialVolumes,
                    partialSemitones,
                    partialStarts,
                    sampleCount,
                    ct
                );
                break;
            case Waveform.Saw:
                RenderSawSamples(
                    buffer,
                    voice,
                    state,
                    partialDelays,
                    partialVolumes,
                    partialSemitones,
                    partialStarts,
                    sampleCount,
                    ct
                );
                break;
            case Waveform.Noise:
                RenderNoiseSamples(
                    buffer,
                    voice,
                    state,
                    partialDelays,
                    partialVolumes,
                    partialSemitones,
                    partialStarts,
                    sampleCount,
                    ct
                );
                break;
            case Waveform.Off:
            default:
                break;
        }
    }
}
