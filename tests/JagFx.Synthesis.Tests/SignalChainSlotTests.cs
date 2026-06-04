using JagFx.Domain.Models;
using Xunit;

namespace JagFx.Synthesis.Tests;

public class SignalChainSlotTests
{
    [Theory]
    [InlineData(SignalChainSlot.Pitch, "Pitch")]
    [InlineData(SignalChainSlot.VibratoRate, "Vibrato Rate")]
    [InlineData(SignalChainSlot.VibratoDepth, "Vibrato Depth")]
    [InlineData(SignalChainSlot.Volume, "Volume")]
    [InlineData(SignalChainSlot.TremoloRate, "Tremolo Rate")]
    [InlineData(SignalChainSlot.TremoloDepth, "Tremolo Depth")]
    [InlineData(SignalChainSlot.GapOff, "Gap Off")]
    [InlineData(SignalChainSlot.GapOn, "Gap On")]
    [InlineData(SignalChainSlot.Filter, "Filter")]
    [InlineData(SignalChainSlot.PoleZero, "Pole / Zero")]
    [InlineData(SignalChainSlot.Output, "Output")]
    [InlineData(SignalChainSlot.Bode, "Frequency Response")]
    public void DisplayNameReturnsHumanReadableLabels(SignalChainSlot slot, string expected) =>
        Assert.Equal(expected, slot.DisplayName());
}
