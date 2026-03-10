namespace JagFx.Domain.Models;

public record LoopSegment(int BeginMs, int EndMs)
{
    public bool IsActive => BeginMs < EndMs;
}
