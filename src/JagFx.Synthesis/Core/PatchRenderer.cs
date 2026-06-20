using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Synthesis.Audio;

namespace JagFx.Synthesis.Core;

public static class PatchRenderer
{
    public static AudioBuffer Synthesize(
        Patch patch,
        int loopCount,
        int voiceFilter = -1,
        CancellationToken ct = default
    )
    {
        using var buffer = SynthesizePooled(patch, loopCount, voiceFilter, ct);
        return buffer.ToAudioBuffer();
    }

    public static PooledAudioBuffer SynthesizePooled(
        Patch patch,
        int loopCount,
        int voiceFilter = -1,
        CancellationToken ct = default
    )
    {
        ct.ThrowIfCancellationRequested();
        var maxDuration = CalculateMaxDuration(patch, voiceFilter);
        if (maxDuration == 0)
        {
            return new PooledAudioBuffer([], 0);
        }

        var sampleCount = (int)(maxDuration * AudioConstants.SampleRatePerMillisecond);
        var loopStart = (int)(patch.Loop.BeginMs * AudioConstants.SampleRatePerMillisecond);
        var loopStop = (int)(patch.Loop.EndMs * AudioConstants.SampleRatePerMillisecond);

        var effectiveLoopCount = ValidateLoopRegion(loopStart, loopStop, sampleCount, loopCount);
        var totalSampleCount =
            sampleCount + (loopStop - loopStart) * Math.Max(0, effectiveLoopCount - 1);

        var buffer = MixVoices(patch, voiceFilter, sampleCount, totalSampleCount, ct);
        if (effectiveLoopCount > 1)
        {
            ct.ThrowIfCancellationRequested();
            ApplyLoopExpansion(
                buffer,
                sampleCount,
                totalSampleCount,
                loopStart,
                loopStop,
                effectiveLoopCount
            );
        }

        ct.ThrowIfCancellationRequested();
        AudioMath.ClipInt16(buffer, totalSampleCount);

        return new PooledAudioBuffer(buffer, totalSampleCount);
    }

    private static int CalculateMaxDuration(Patch patch, int voiceFilter)
    {
        var maxDuration = 0;
        var voices = patch.Voices;
        for (var i = 0; i < voices.Count; i++)
        {
            if (voiceFilter >= 0 && i != voiceFilter)
            {
                continue;
            }

            var voice = voices[i];
            if (voice == null)
            {
                continue;
            }

            var endTime = voice.DurationMs + voice.OffsetMs;
            if (endTime > maxDuration)
            {
                maxDuration = endTime;
            }
        }

        return maxDuration;
    }

    private static int ValidateLoopRegion(int start, int end, int length, int loopCount) =>
        start < 0 || end > length || start >= end ? 0 : loopCount;

    private static int[] MixVoices(
        Patch patch,
        int voiceFilter,
        int sampleCount,
        int totalSampleCount,
        CancellationToken ct
    )
    {
        var buffer = AudioBufferPool.Acquire(totalSampleCount);
        var voices = patch.Voices;

        for (var voiceIndex = 0; voiceIndex < voices.Count; voiceIndex++)
        {
            if (voiceFilter >= 0 && voiceIndex != voiceFilter)
            {
                continue;
            }

            ct.ThrowIfCancellationRequested();
            var voice = voices[voiceIndex];
            if (voice == null)
            {
                continue;
            }

            var voiceBuffer = VoiceSynthesizer.SynthesizePooledCore(voice, ct);
            try
            {
                var startOffset = (int)(voice.OffsetMs * AudioConstants.SampleRatePerMillisecond);
                var mixStart = Math.Max(0, -startOffset);
                var sampleRoom = sampleCount - startOffset;
                var mixEnd = voiceBuffer.Length < sampleRoom ? voiceBuffer.Length : sampleRoom;
                var voiceSamples = voiceBuffer.Samples;

                for (var i = mixStart; i < mixEnd; i++)
                {
                    if ((i & 0x1FF) == 0)
                    {
                        ct.ThrowIfCancellationRequested();
                    }

                    buffer[i + startOffset] += voiceSamples[i];
                }
            }
            finally
            {
                AudioBufferPool.Release(voiceBuffer.Samples);
            }
        }

        return buffer;
    }

    private static void ApplyLoopExpansion(
        int[] buffer,
        int sampleCount,
        int totalSampleCount,
        int loopStart,
        int loopStop,
        int loopCount
    )
    {
        var endOffset = totalSampleCount - sampleCount;

        for (var sample = sampleCount - 1; sample >= loopStop; sample--)
        {
            buffer[sample + endOffset] = buffer[sample];
        }

        for (var loop = 1; loop < loopCount; loop++)
        {
            var offset = (loopStop - loopStart) * loop;
            for (var sample = loopStart; sample < loopStop; sample++)
            {
                buffer[sample + offset] = buffer[sample];
            }
        }
    }
}
