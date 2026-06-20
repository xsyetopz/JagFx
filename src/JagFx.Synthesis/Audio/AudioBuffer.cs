using JagFx.Core.Constants;
using JagFx.Domain.Utilities;

namespace JagFx.Synthesis.Audio;

public class AudioBuffer(int[] samples, int sampleRate = AudioConstants.SampleRate)
{
    public int[] Samples { get; } = samples;

    public int SampleRate { get; } = sampleRate;

    public int Length => Samples.Length;

    public static AudioBuffer Empty(int sampleCount, int sampleRate = AudioConstants.SampleRate) =>
        new(sampleCount == 0 ? [] : new int[sampleCount], sampleRate);

    public AudioBuffer Mix(AudioBuffer other, int offset)
    {
        var mixedSampleCount = Math.Max(Samples.Length, other.Samples.Length + offset);
        var mixedSamples = new int[mixedSampleCount];
        Array.Copy(Samples, 0, mixedSamples, 0, Samples.Length);

        for (var i = 0; i < other.Samples.Length; i++)
        {
            var pos = i + offset;
            if (pos >= 0 && pos < mixedSampleCount)
            {
                mixedSamples[pos] += other.Samples[i];
            }
        }

        return new AudioBuffer(mixedSamples, SampleRate);
    }

    public AudioBuffer Clip()
    {
        var newSamples = (int[])Samples.Clone();
        AudioMath.ClipInt16(newSamples);
        return new AudioBuffer(newSamples, SampleRate);
    }

    public byte[] ToUBytes()
    {
        var unsignedPcmBytes = new byte[Samples.Length];
        for (var i = 0; i < Samples.Length; i++)
        {
            unsignedPcmBytes[i] = (byte)((Samples[i] >> 8) + 128);
        }
        return unsignedPcmBytes;
    }

    public byte[] ToBytes()
    {
        var signedPcmBytes = new byte[Samples.Length];
        for (var i = 0; i < Samples.Length; i++)
        {
            signedPcmBytes[i] = (byte)(Samples[i] >> 8);
        }
        return signedPcmBytes;
    }

    public byte[] ToBytes16BE()
    {
        var bigEndianPcmBytes = new byte[Samples.Length * 2];
        for (var i = 0; i < Samples.Length; i++)
        {
            bigEndianPcmBytes[i * 2] = (byte)(Samples[i] >> 8);
            bigEndianPcmBytes[i * 2 + 1] = (byte)Samples[i];
        }
        return bigEndianPcmBytes;
    }

    public byte[] ToBytes16LE()
    {
        var littleEndianPcmBytes = new byte[Samples.Length * 2];
        for (var i = 0; i < Samples.Length; i++)
        {
            littleEndianPcmBytes[i * 2] = (byte)Samples[i];
            littleEndianPcmBytes[i * 2 + 1] = (byte)(Samples[i] >> 8);
        }
        return littleEndianPcmBytes;
    }
}
