using JagFx.Core.Constants;
using System.Collections.Concurrent;

namespace JagFx.Synthesis.Data;

public static class AudioBufferPool
{
    private const int MaxCapacity = 20;
    private static readonly ConcurrentQueue<int[]> Pool = new();

    public static int[] Acquire(int minSize)
    {
        if (minSize > AudioConstants.MaxBufferSize)
            return new int[minSize];

        var result = TryAcquireFromPool(minSize);
        return result ?? new int[minSize];
    }

    public static void Release(int[] buffer)
    {
        if (buffer != null && buffer.Length == AudioConstants.MaxBufferSize)
        {
            Array.Clear(buffer, 0, buffer.Length);
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
        var attempts = new List<int[]>();
        int[]? result = null;

        while (Pool.TryDequeue(out var buf))
        {
            if (buf.Length >= minSize && result == null)
                result = buf;
            else
                attempts.Add(buf);
        }

        foreach (var buf in attempts)
            Pool.Enqueue(buf);

        if (result != null)
            Array.Clear(result);

        return result;
    }
}
