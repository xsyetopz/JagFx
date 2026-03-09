using JagFX.Domain.Models;
using JagFX.Synthesis.Core;
using JagFX.Synthesis.Data;

namespace JagFX.Desktop.Services;

public static class SynthesisService
{
    public static async Task<AudioBuffer> RenderAsync(
        Patch patch, int loopCount = 1, int voiceFilter = -1,
        CancellationToken ct = default)
    {
        return await Task.Run(() => PatchRenderer.Synthesize(patch, loopCount, voiceFilter), ct);
    }
}
