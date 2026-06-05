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
            var audio = PatchRenderer.Synthesize(patch, loopCount, voiceFilter);
            var pcm = audio.ToBytes16LE();

            bytesWritten = pcm.Length;
            if (destination.Length < pcm.Length)
            {
                return JagFxNativeStatus.BufferTooSmall;
            }

            pcm.CopyTo(destination);
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
