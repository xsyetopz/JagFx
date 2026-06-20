using JagFx.Io;
using JagFx.Synthesis.Core;

namespace JagFx.Native;

public static class JagFxNativeRenderer
{
    public static JagFxNativeStatus RenderPcm16Le(
        ReadOnlySpan<byte> synthData,
        Span<byte> destination,
        out int bytesWritten,
        int loopCount = 1,
        int voiceFilter = -1
    )
    {
        bytesWritten = 0;

        if (synthData.IsEmpty || loopCount < 1)
        {
            return JagFxNativeStatus.InvalidArgument;
        }

        try
        {
            var patch = SynthFileReader.Read(synthData.ToArray());
            using var audio = PatchRenderer.SynthesizePooled(patch, loopCount, voiceFilter);

            bytesWritten = audio.Length * sizeof(short);
            if (destination.Length < bytesWritten)
            {
                return JagFxNativeStatus.BufferTooSmall;
            }

            var samples = audio.Span;
            for (var i = 0; i < samples.Length; i++)
            {
                var sample = samples[i];
                var byteOffset = i * sizeof(short);
                destination[byteOffset] = (byte)sample;
                destination[byteOffset + 1] = (byte)(sample >> 8);
            }
            return JagFxNativeStatus.Success;
        }
        catch (Exception ex) when (ex is InvalidDataException or SynthFileException)
        {
            return JagFxNativeStatus.DecodeError;
        }
        catch (Exception)
        {
            return JagFxNativeStatus.RenderError;
        }
    }
}
