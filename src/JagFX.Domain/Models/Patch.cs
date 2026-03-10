using System.Collections.Immutable;

namespace JagFx.Domain.Models;

public record Patch(
    ImmutableList<Voice?> Voices,
    LoopSegment Loop,
    ImmutableList<string> ValidationWarnings = default!
)
{
    public Patch(ImmutableList<Voice?> voices, LoopSegment loop, IEnumerable<string>? warnings = null)
        : this(voices, loop, warnings?.ToImmutableList() ?? ImmutableList<string>.Empty)
    {
    }

    public ImmutableList<(int Index, Voice Voice)> ActiveVoices =>
        [.. Voices
            .Select((voice, index) => (voice, index))
            .Where(x => x.voice != null)
            .Select(x => (x.index, x.voice!))];
}
