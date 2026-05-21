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
        var audio = PatchRenderer.Synthesize(patch, loopCount, voiceFilter);
        return audio.ToBytes16LE();
    }
}
