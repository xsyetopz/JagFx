using JagFx.Core.Constants;

namespace JagFx.Synthesis.Audio;

public sealed class PooledAudioBuffer : IDisposable
{
    private int[]? _samples;

    internal PooledAudioBuffer(
        int[] samples,
        int length,
        int sampleRate = AudioConstants.SampleRate
    )
    {
        _samples = samples;
        Length = length;
        SampleRate = sampleRate;
    }

    public int[] Samples =>
        _samples ?? throw new ObjectDisposedException(nameof(PooledAudioBuffer));

    public int Length { get; }

    public int SampleRate { get; }

    public Span<int> Span => Samples.AsSpan(0, Length);

    public AudioBuffer ToAudioBuffer()
    {
        var output = new int[Length];
        Array.Copy(Samples, 0, output, 0, Length);
        return new AudioBuffer(output, SampleRate);
    }

    public void Dispose()
    {
        var samples = _samples;
        if (samples == null)
        {
            return;
        }

        _samples = null;
        AudioBufferPool.Release(samples);
    }
}
