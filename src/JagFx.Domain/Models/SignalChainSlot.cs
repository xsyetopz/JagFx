namespace JagFx.Domain.Models;

public enum SignalChainSlot
{
    Pitch,
    VibratoRate,
    VibratoDepth,
    Volume,
    TremoloRate,
    TremoloDepth,
    GapOff,
    GapOn,
    Filter,

    // Non-envelope slots (view-only, not in SignalChain array)
    PoleZero,
    Output,
    Bode,
}

public static class SignalChainSlotExtensions
{
    public static bool UsesLogFrequencyScale(this SignalChainSlot slot) =>
        slot is SignalChainSlot.Pitch or SignalChainSlot.VibratoRate or SignalChainSlot.TremoloRate;

    public static bool UsesGateDurationScale(this SignalChainSlot slot) =>
        slot is SignalChainSlot.GapOff or SignalChainSlot.GapOn;

    public static bool UsesPercentScale(this SignalChainSlot slot) =>
        slot
            is SignalChainSlot.Volume
                or SignalChainSlot.VibratoDepth
                or SignalChainSlot.TremoloDepth
                or SignalChainSlot.Filter;

    public static string DisplayName(this SignalChainSlot slot) =>
        slot switch
        {
            SignalChainSlot.Pitch => "Pitch",
            SignalChainSlot.VibratoRate => "Vibrato Rate",
            SignalChainSlot.VibratoDepth => "Vibrato Depth",
            SignalChainSlot.Volume => "Volume",
            SignalChainSlot.TremoloRate => "Tremolo Rate",
            SignalChainSlot.TremoloDepth => "Tremolo Depth",
            SignalChainSlot.GapOff => "Gap Off",
            SignalChainSlot.GapOn => "Gap On",
            SignalChainSlot.Filter => "Filter",
            SignalChainSlot.PoleZero => "Pole / Zero",
            SignalChainSlot.Output => "Output",
            SignalChainSlot.Bode => "Frequency Response",
            _ => slot.ToString(),
        };
}
