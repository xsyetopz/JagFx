namespace JagFx.Domain.Models;

public record Echo(
    /// <summary>Echo delay in milliseconds.</summary>
    int DelayMilliseconds,
    /// <summary>Echo feedback as a percentage (0–100).</summary>
    int FeedbackPercent
);
