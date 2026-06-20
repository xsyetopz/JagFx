using System.Buffers;
using JagFx.Core.Constants;
using JagFx.Domain.Models;
using JagFx.Synthesis.Audio;

namespace JagFx.Synthesis.Processing;

public static partial class AudioFilter
{
    private const int MaxCoefficients = 8;
    private const int ChunkSize = 128;
    private const int AudioFilterCoefficientCacheSize = AudioConstants.FixedPoint.Scale + 1;
    private const float AmplitudeDbScaleFactor = 0.0015258789f;
    private const float PhaseScaleFactor = 1.2207031e-4f;
    private const double C1BaseFrequencyHz = 32.703197;
    private const float GainScaleFactor = 0.0030517578f;

    private ref struct AudioFilterProcessingFrame
    {
        public Span<int> InputSpan;
        public Span<int> OutputSpan;
        public AudioFilterCoefficientState Coefficients;
        public AudioFilterCoefficientCache CoefficientCache;
        public EnvelopeGenerator? ModulationEnvelope;
        public int SampleCount;
    }

    public static void Apply(
        int[] buffer,
        Filter filter,
        EnvelopeGenerator? envelopeEval,
        int sampleCount,
        CancellationToken ct = default
    )
    {
        ct.ThrowIfCancellationRequested();
        if (filter.PoleCounts[0] == 0 && filter.PoleCounts[1] == 0)
        {
            return;
        }

        envelopeEval?.Reset();

        var preFilterSampleBuffer = AudioBufferPool.Acquire(sampleCount);
        var cacheKeysBuffer = ArrayPool<int>.Shared.Rent(AudioFilterCoefficientCacheSize);
        var cacheInverseA0Buffer = ArrayPool<int>.Shared.Rent(AudioFilterCoefficientCacheSize);
        var cacheFfCountsBuffer = ArrayPool<int>.Shared.Rent(AudioFilterCoefficientCacheSize);
        var cacheFbCountsBuffer = ArrayPool<int>.Shared.Rent(AudioFilterCoefficientCacheSize);
        var cacheFeedforwardBuffer = ArrayPool<int>.Shared.Rent(
            AudioFilterCoefficientCacheSize * MaxCoefficients
        );
        var cacheFeedbackBuffer = ArrayPool<int>.Shared.Rent(
            AudioFilterCoefficientCacheSize * MaxCoefficients
        );
        try
        {
            var inputCopySpan = preFilterSampleBuffer.AsSpan(0, sampleCount);
            var outSpan = buffer.AsSpan(0, sampleCount);
            outSpan.CopyTo(inputCopySpan);

            Span<float> sosCoefficients = stackalloc float[2 * MaxCoefficients];
            Span<int> feedforward = stackalloc int[MaxCoefficients];
            Span<int> feedback = stackalloc int[MaxCoefficients];
            var cacheKeys = cacheKeysBuffer.AsSpan(0, AudioFilterCoefficientCacheSize);
            var cacheInverseA0 = cacheInverseA0Buffer.AsSpan(0, AudioFilterCoefficientCacheSize);
            var cacheFfCounts = cacheFfCountsBuffer.AsSpan(0, AudioFilterCoefficientCacheSize);
            var cacheFbCounts = cacheFbCountsBuffer.AsSpan(0, AudioFilterCoefficientCacheSize);
            var cacheFeedforward = cacheFeedforwardBuffer.AsSpan(
                0,
                AudioFilterCoefficientCacheSize * MaxCoefficients
            );
            var cacheFeedback = cacheFeedbackBuffer.AsSpan(
                0,
                AudioFilterCoefficientCacheSize * MaxCoefficients
            );
            cacheKeys.Fill(-1);

            var filterCoefficients = new AudioFilterCoefficientState(
                filter,
                sosCoefficients,
                feedforward,
                feedback
            );
            var coefficientCache = new AudioFilterCoefficientCache(
                cacheKeys,
                cacheInverseA0,
                cacheFfCounts,
                cacheFbCounts,
                cacheFeedforward,
                cacheFeedback
            );
            var envelopeValue =
                envelopeEval?.Evaluate(sampleCount) ?? AudioConstants.FixedPoint.Scale;
            var ffCount = 0;
            var fbCount = 0;
            var filterFrame = new AudioFilterProcessingFrame
            {
                InputSpan = inputCopySpan,
                OutputSpan = outSpan,
                Coefficients = filterCoefficients,
                CoefficientCache = coefficientCache,
                ModulationEnvelope = envelopeEval,
                SampleCount = sampleCount,
            };
            ApplyCachedCoefficients(ref filterFrame, envelopeValue, ref ffCount, ref fbCount);

            if (sampleCount < ffCount + fbCount)
            {
                return;
            }

            ProcessInitialBlock(ref filterFrame, ffCount, fbCount, ct);
            ProcessMainBlocks(ref filterFrame, ref ffCount, ref fbCount, ct);
            ProcessFinalBlock(ref filterFrame, ffCount, fbCount, ct);
        }
        finally
        {
            ArrayPool<int>.Shared.Return(cacheFeedbackBuffer);
            ArrayPool<int>.Shared.Return(cacheFeedforwardBuffer);
            ArrayPool<int>.Shared.Return(cacheFbCountsBuffer);
            ArrayPool<int>.Shared.Return(cacheFfCountsBuffer);
            ArrayPool<int>.Shared.Return(cacheInverseA0Buffer);
            ArrayPool<int>.Shared.Return(cacheKeysBuffer);
            AudioBufferPool.Release(preFilterSampleBuffer);
        }
    }

    private static void ProcessInitialBlock(
        ref AudioFilterProcessingFrame filterFrame,
        int ffCount,
        int fbCount,
        CancellationToken ct
    ) => ProcessSampleRange(ref filterFrame, 0, fbCount, ffCount, fbCount, ct);

    private static void ProcessMainBlocks(
        ref AudioFilterProcessingFrame filterFrame,
        ref int ffCount,
        ref int fbCount,
        CancellationToken ct
    )
    {
        var pos = fbCount;
        var mainEnd = filterFrame.SampleCount - ffCount;
        var steadyEnd = mainEnd - ChunkSize;

        while (pos <= steadyEnd)
        {
            var chunkEnd = pos + ChunkSize;
            ProcessSampleRange(ref filterFrame, pos, chunkEnd, ffCount, fbCount, ct);
            pos = chunkEnd;
        }

        if (pos < mainEnd)
        {
            ProcessSampleRange(ref filterFrame, pos, mainEnd, ffCount, fbCount, ct);
        }
    }

    private static void ProcessFinalBlock(
        ref AudioFilterProcessingFrame filterFrame,
        int ffCount,
        int fbCount,
        CancellationToken ct
    ) =>
        ProcessFinalSampleRange(
            ref filterFrame,
            filterFrame.SampleCount - ffCount,
            filterFrame.SampleCount,
            ffCount,
            fbCount,
            ct
        );

    private static void ProcessSampleRange(
        ref AudioFilterProcessingFrame filterFrame,
        int start,
        int end,
        int ffCount,
        int fbCount,
        CancellationToken ct
    )
    {
        if (filterFrame.ModulationEnvelope == null)
        {
            for (var n = start; n < end; n++)
            {
                if ((n & 0x3F) == 0)
                {
                    ct.ThrowIfCancellationRequested();
                }

                ApplyFilterToSample(
                    filterFrame.InputSpan,
                    filterFrame.OutputSpan,
                    filterFrame.Coefficients,
                    n,
                    ffCount,
                    fbCount
                );
            }

            return;
        }

        var envelopeEval = filterFrame.ModulationEnvelope;
        for (var n = start; n < end; n++)
        {
            if ((n & 0x3F) == 0)
            {
                ct.ThrowIfCancellationRequested();
            }

            ApplyFilterToSample(
                filterFrame.InputSpan,
                filterFrame.OutputSpan,
                filterFrame.Coefficients,
                n,
                ffCount,
                fbCount
            );

            var lastEnvelopeValue = envelopeEval.Evaluate(filterFrame.SampleCount);
            if (n + 1 < filterFrame.SampleCount)
            {
                ApplyCachedCoefficients(
                    ref filterFrame,
                    lastEnvelopeValue,
                    ref ffCount,
                    ref fbCount
                );
            }
        }
    }

    private static void ProcessFinalSampleRange(
        ref AudioFilterProcessingFrame filterFrame,
        int start,
        int end,
        int ffCount,
        int fbCount,
        CancellationToken ct
    )
    {
        if (filterFrame.ModulationEnvelope == null)
        {
            for (var n = start; n < end; n++)
            {
                if ((n & 0x3F) == 0)
                {
                    ct.ThrowIfCancellationRequested();
                }

                ApplyFilterToFinalSample(
                    filterFrame.InputSpan,
                    filterFrame.OutputSpan,
                    filterFrame.Coefficients,
                    n,
                    ffCount,
                    fbCount,
                    filterFrame.SampleCount
                );
            }

            return;
        }

        var envelopeEval = filterFrame.ModulationEnvelope;
        for (var n = start; n < end; n++)
        {
            if ((n & 0x3F) == 0)
            {
                ct.ThrowIfCancellationRequested();
            }

            ApplyFilterToFinalSample(
                filterFrame.InputSpan,
                filterFrame.OutputSpan,
                filterFrame.Coefficients,
                n,
                ffCount,
                fbCount,
                filterFrame.SampleCount
            );

            var lastEnvelopeValue = envelopeEval.Evaluate(filterFrame.SampleCount);
            if (n + 1 < filterFrame.SampleCount)
            {
                ApplyCachedCoefficients(
                    ref filterFrame,
                    lastEnvelopeValue,
                    ref ffCount,
                    ref fbCount
                );
            }
        }
    }

    private static void ApplyFilterToSample(
        Span<int> inputCopySpan,
        Span<int> outSpan,
        AudioFilterCoefficientState state,
        int sampleIndex,
        int ffCount,
        int fbCount
    )
    {
        var acc = 0L;
        AddFeedforward(inputCopySpan, state, sampleIndex, ffCount, ref acc);
        AddFeedback(outSpan, state, sampleIndex, fbCount, ref acc);
        outSpan[sampleIndex] = (int)acc;
    }

    private static void ApplyFilterToFinalSample(
        Span<int> inputCopySpan,
        Span<int> outSpan,
        AudioFilterCoefficientState state,
        int sampleIndex,
        int ffCount,
        int fbCount,
        int sampleCount
    )
    {
        var acc = 0L;
        AddFeedforwardFinal(inputCopySpan, state, sampleIndex, ffCount, sampleCount, ref acc);
        AddFeedback(outSpan, state, sampleIndex, fbCount, ref acc);
        outSpan[sampleIndex] = (int)acc;
    }

    private static void AddFeedforward(
        Span<int> inputCopySpan,
        AudioFilterCoefficientState state,
        int sampleIndex,
        int ffCount,
        ref long acc
    )
    {
        acc += ((long)inputCopySpan[sampleIndex + ffCount] * state.InverseA0) >> 16;
        for (var k = 0; k < ffCount; k++)
        {
            acc +=
                ((long)inputCopySpan[sampleIndex + ffCount - 1 - k] * state.Feedforward[k]) >> 16;
        }
    }

    private static void AddFeedforwardFinal(
        Span<int> inputCopySpan,
        AudioFilterCoefficientState state,
        int sampleIndex,
        int ffCount,
        int sampleCount,
        ref long acc
    )
    {
        var startK = sampleIndex + ffCount - sampleCount;
        for (var k = startK; k < ffCount; k++)
        {
            var inputIndex = sampleIndex + ffCount - 1 - k;
            acc += ((long)inputCopySpan[inputIndex] * state.Feedforward[k]) >> 16;
        }
    }

    private static void AddFeedback(
        Span<int> outSpan,
        AudioFilterCoefficientState state,
        int sampleIndex,
        int fbCount,
        ref long acc
    )
    {
        if (sampleIndex < fbCount)
        {
            for (var k = 0; k < sampleIndex; k++)
            {
                var bufferIndex = sampleIndex - 1 - k;
                var fbTerm = ((long)outSpan[bufferIndex] * state.Feedback[k]) >> 16;
                acc -= fbTerm;
            }

            return;
        }

        for (var k = 0; k < fbCount; k++)
        {
            var bufferIndex = sampleIndex - 1 - k;
            var fbTerm = ((long)outSpan[bufferIndex] * state.Feedback[k]) >> 16;
            acc -= fbTerm;
        }
    }
}
