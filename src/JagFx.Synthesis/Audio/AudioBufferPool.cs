using System.Collections.Concurrent;
using JagFx.Core.Constants;

namespace JagFx.Synthesis.Audio;

public static class AudioBufferPool
{
    private const int MaxCapacity = 20;
    private static readonly ConcurrentQueue<int[]> Pool = new();

    public static int[] Acquire(int minSize)
    {
        if (minSize > AudioConstants.MaxBufferSize)
        {
            return new int[minSize];
        }

        var pooledSamples = TryAcquireFromPool(minSize);
        return pooledSamples ?? new int[AudioConstants.MaxBufferSize];
    }

    public static void Release(int[] buffer)
    {
        if (buffer != null && buffer.Length == AudioConstants.MaxBufferSize)
        {
            if (Pool.Count < MaxCapacity)
            {
                Pool.Enqueue(buffer);
            }
        }
    }

    public static void Clear()
    {
        while (Pool.TryDequeue(out _)) { }
    }

    private static int[]? TryAcquireFromPool(int minSize)
    {
        if (!Pool.TryDequeue(out var pooledSamples))
        {
            return null;
        }

        Array.Clear(pooledSamples, 0, minSize);
        return pooledSamples;
    }
}
