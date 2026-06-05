using System.Collections.Immutable;
using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Domain.Utilities;
using JagFx.Synthesis.Data;

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
        ct.ThrowIfCancellationRequested();
        var voicesToMix =
            voiceFilter < 0
                ? patch.ActiveVoices
                : [.. patch.ActiveVoices.Where(v => v.Index == voiceFilter)];

        ct.ThrowIfCancellationRequested();
        var maxDuration = CalculateMaxDuration(voicesToMix);
        if (maxDuration == 0)
        {
            return AudioBuffer.Empty(0);
        }

        var sampleCount = (int)(maxDuration * AudioConstants.SampleRatePerMillisecond);
        var loopStart = (int)(patch.Loop.BeginMs * AudioConstants.SampleRatePerMillisecond);
        var loopStop = (int)(patch.Loop.EndMs * AudioConstants.SampleRatePerMillisecond);

        var effectiveLoopCount = ValidateLoopRegion(loopStart, loopStop, sampleCount, loopCount);
        var totalSampleCount =
            sampleCount + (loopStop - loopStart) * Math.Max(0, effectiveLoopCount - 1);

        var buffer = MixVoices(voicesToMix, sampleCount, totalSampleCount, ct);
        if (effectiveLoopCount > 1)
        {
            ct.ThrowIfCancellationRequested();
            ApplyLoopExpansion(buffer, sampleCount, loopStart, loopStop, effectiveLoopCount);
        }

        ct.ThrowIfCancellationRequested();
        AudioMath.ClipInt16(buffer, totalSampleCount);

        var output = new int[totalSampleCount];
        Array.Copy(buffer, 0, output, 0, totalSampleCount);
        AudioBufferPool.Release(buffer);

        return new AudioBuffer(output, AudioConstants.SampleRate);
    }

    private static int CalculateMaxDuration(ImmutableList<(int Index, Voice Voice)> voices)
    {
        var maxDuration = 0;
        foreach (var (_, voice) in voices)
        {
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
        ImmutableList<(int Index, Voice Voice)> voices,
        int sampleCount,
        int totalSampleCount,
        CancellationToken ct
    )
    {
        var buffer = AudioBufferPool.Acquire(totalSampleCount);

        foreach (var (_, voice) in voices)
        {
            ct.ThrowIfCancellationRequested();
            var voiceBuffer = VoiceSynthesizer.Synthesize(voice, ct);
            var startOffset = (int)(voice.OffsetMs * AudioConstants.SampleRatePerMillisecond);

            for (var i = 0; i < voiceBuffer.Length; i++)
            {
                if ((i & 0x1FF) == 0)
                {
                    ct.ThrowIfCancellationRequested();
                }

                var pos = i + startOffset;
                if (pos >= 0 && pos < sampleCount)
                {
                    buffer[pos] += voiceBuffer.Samples[i];
                }
            }
        }

        return buffer;
    }

    private static void ApplyLoopExpansion(
        int[] buffer,
        int sampleCount,
        int loopStart,
        int loopStop,
        int loopCount
    )
    {
        var totalSampleCount = buffer.Length;
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
