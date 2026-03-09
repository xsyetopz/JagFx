using System.Collections.Immutable;
namespace JagFX.Domain.Models;

public record class Envelope(Waveform Waveform, int StartValue, int EndValue, ImmutableList<Segment> Segments);
public record struct Segment(int Duration, int TargetLevel);
