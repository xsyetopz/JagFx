using Xunit;

namespace SmartInt.Tests;

public class USmart16Tests
{
    [Fact]
    public void EncodeDecode_SmallValue_Works()
    {
        var smart = new USmart16(42);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = USmart16.FromEncoded(buffer, out var bytesRead);

        Assert.Equal(bytes, bytesRead);
        Assert.Equal((ushort)42, decoded.Value);
    }

    [Fact]
    public void EncodeDecode_LargeValue_Works()
    {
        var smart = new USmart16(USmart16.MaxValue);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = USmart16.FromEncoded(buffer, out var bytesRead);

        Assert.Equal(bytes, bytesRead);
        Assert.Equal(USmart16.MaxValue, decoded.Value);
    }
}
