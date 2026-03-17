namespace JagFx.Domain.Models;

/// <summary>Echo effect configuration.</summary>
/// <param name="DelayMilliseconds">Echo delay in milliseconds.</param>
/// <param name="FeedbackPercent">Echo feedback as a percentage (0-100).</param>
public record Echo(
    int DelayMilliseconds,
    int FeedbackPercent
);
