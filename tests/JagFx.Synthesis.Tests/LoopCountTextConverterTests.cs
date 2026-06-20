using System.Globalization;
using JagFx.Desktop.Converters;
using Xunit;

namespace JagFx.Synthesis.Tests;

public class LoopCountTextConverterTests
{
    private static readonly LoopCountTextConverter Converter = new();

    [Fact]
    public void ConvertFormatsZeroAsZero()
    {
        var loopCountText = Converter.Convert(
            0,
            typeof(string),
            null,
            CultureInfo.InvariantCulture
        );

        Assert.Equal("0", loopCountText);
    }

    [Fact]
    public void ConvertBackParsesZeroAsZero()
    {
        var parsedLoopCount = Converter.ConvertBack(
            "0",
            typeof(int),
            null,
            CultureInfo.InvariantCulture
        );

        Assert.Equal(0, parsedLoopCount);
    }

    [Fact]
    public void ConvertBackClampsLargeValues()
    {
        var cappedLoopCount = Converter.ConvertBack(
            "1000",
            typeof(int),
            null,
            CultureInfo.InvariantCulture
        );

        Assert.Equal(999, cappedLoopCount);
    }
}
