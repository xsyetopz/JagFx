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
    public static string DisplayName(this SignalChainSlot slot) => slot switch
    {
        SignalChainSlot.Pitch => "PITCH",
        SignalChainSlot.VibratoRate => "V.RATE",
        SignalChainSlot.VibratoDepth => "V.DEPTH",
        SignalChainSlot.Volume => "VOLUME",
        SignalChainSlot.TremoloRate => "T.RATE",
        SignalChainSlot.TremoloDepth => "T.DEPTH",
        SignalChainSlot.GapOff => "GAP OFF",
        SignalChainSlot.GapOn => "GAP ON",
        SignalChainSlot.Filter => "FILTER",
        SignalChainSlot.PoleZero => "P/Z",
        SignalChainSlot.Output => "OUT",
        SignalChainSlot.Bode => "BODE",
        _ => slot.ToString(),
    };

}
