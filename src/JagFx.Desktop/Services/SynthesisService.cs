using JagFx.Domain.Models;
using JagFx.Synthesis.Audio;
using JagFx.Synthesis.Core;

namespace JagFx.Desktop.Services;

public static class SynthesisService
{
    public static async Task<AudioBuffer> RenderAsync(
        Patch patch,
        int loopCount = 1,
        int voiceFilter = -1,
        CancellationToken ct = default
    )
    {
        return await Task.Run(
                () =>
                {
                    using var pooled = PatchRenderer.SynthesizePooled(
                        patch,
                        loopCount,
                        voiceFilter,
                        ct
                    );
                    return pooled.ToAudioBuffer();
                },
                ct
            )
            .ConfigureAwait(false);
    }
}
