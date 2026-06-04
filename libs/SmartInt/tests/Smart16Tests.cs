using Xunit;

namespace SmartInt.Tests;

public class Smart16Tests
{
    [Fact]
    public void EncodeDecodeSmallValueWorks()
    {
        var smart = new Smart16(42);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = Smart16.FromEncoded(buffer, out var bytesRead);

        Assert.Equal(bytes, bytesRead);
        Assert.Equal((short)42, decoded.Value);
    }

    [Fact]
    public void EncodeDecodeNegativeValueWorks()
    {
        var smart = new Smart16(-100);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = Smart16.FromEncoded(buffer, out var bytesRead);

        Assert.Equal(bytes, bytesRead);
        Assert.Equal((short)-100, decoded.Value);
    }

    [Fact]
    public void EncodeDecodeLargeValueWorks()
    {
        var smart = new Smart16(Smart16.MaxValue);
        var buffer = new byte[3];
        var bytes = smart.Encode(buffer);
        var decoded = Smart16.FromEncoded(buffer, out var bytesRead);

        Assert.Equal(bytes, bytesRead);
        Assert.Equal(Smart16.MaxValue, decoded.Value);
    }
}
