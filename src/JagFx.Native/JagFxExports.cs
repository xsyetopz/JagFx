using System.Runtime.InteropServices;

namespace JagFx.Native;

public static unsafe class JagFxExports
{
    [UnmanagedCallersOnly(EntryPoint = "jagfx_render_pcm16_le")]
    public static int RenderPcm16Le(
        byte* synthData,
        nuint synthLength,
        int loopCount,
        int voiceFilter,
        byte* outputBuffer,
        nuint outputBufferLength,
        nuint* bytesWritten
    )
    {
        if (bytesWritten is null)
            return (int)JagFxNativeStatus.InvalidArgument;

        *bytesWritten = 0;

        if (synthData is null || synthLength == 0)
            return (int)JagFxNativeStatus.InvalidArgument;

        if (synthLength > int.MaxValue || outputBufferLength > int.MaxValue)
            return (int)JagFxNativeStatus.InvalidArgument;

        var input = new ReadOnlySpan<byte>(synthData, (int)synthLength);
        var output = outputBuffer is null
            ? []
            : new Span<byte>(outputBuffer, (int)outputBufferLength);

        var status = JagFxNativeRenderer.RenderPcm16Le(
            input,
            output,
            out var managedBytesWritten,
            loopCount,
            voiceFilter
        );

        *bytesWritten = (nuint)managedBytesWritten;
        return (int)status;
    }
}
