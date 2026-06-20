using System.Runtime.InteropServices.JavaScript;
using JagFx.Io;
using JagFx.Synthesis.Core;

namespace JagFx.Wasm;

public static partial class JagFxWasmExports
{
    [JSExport]
    public static byte[] RenderPcm16Le(byte[] synthData, int loopCount, int voiceFilter)
    {
        var patch = SynthFileReader.Read(synthData);
        using var audio = PatchRenderer.SynthesizePooled(patch, loopCount, voiceFilter);
        var bytes = new byte[audio.Length * sizeof(short)];
        var samples = audio.Span;
        for (var i = 0; i < samples.Length; i++)
        {
            var sample = samples[i];
            var byteOffset = i * sizeof(short);
            bytes[byteOffset] = (byte)sample;
            bytes[byteOffset + 1] = (byte)(sample >> 8);
        }

        return bytes;
    }
}
