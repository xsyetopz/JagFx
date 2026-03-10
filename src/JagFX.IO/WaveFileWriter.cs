using JagFx.Core.Constants;
using System.Buffers.Binary;

namespace JagFx.Io;

public static class WaveFileWriter
{
    private const int RiffMagic = 0x52494646; // "RIFF"
    private const int WaveMagic = 0x57415645; // "WAVE"
    private const int FmtMagic = 0x666d7420; // "fmt "
    private const int DataMagic = 0x64617461; // "data"

    private const int HeaderSize = 44;
    private const int FmtChunkSize = 16;
    private const int PcmFormat = 1;

    private const int RiffHeaderOffset = 0;
    private const int FileSizeOffset = 4;
    private const int WaveFormatOffset = 8;
    private const int FmtMagicOffset = 12;
    private const int FmtSizeOffset = 16;
    private const int PcmFormatOffset = 20;
    private const int ChannelsOffset = 22;
    private const int SampleRateOffset = 24;
    private const int ByteRateOffset = 28;
    private const int BlockAlignOffset = 32;
    private const int BitsPerSampleOffset = 34;
    private const int DataMagicOffset = 36;
    private const int DataSizeOffset = 40;
    private const int SampleDataOffset = 44;

    public static byte[] Write(byte[] samples, int bitsPerSample = 8)
    {
        var dataSize = samples.Length;
        var fileSize = HeaderSize - 8 + dataSize;

        var buffer = new byte[HeaderSize + dataSize];

        WriteRiffHeader(buffer, fileSize);
        WriteFmtChunk(buffer, bitsPerSample);
        WriteDataChunk(buffer, dataSize);
        Array.Copy(samples, 0, buffer, SampleDataOffset, dataSize);

        return buffer;
    }

    public static void WriteToPath(byte[] samples, string path, int bitsPerSample = 8)
    {
        var wavData = Write(samples, bitsPerSample);
        File.WriteAllBytes(path, wavData);
    }

    private static void WriteRiffHeader(byte[] buffer, int fileSize)
    {
        BinaryPrimitives.WriteInt32BigEndian(buffer.AsSpan(RiffHeaderOffset), RiffMagic);
        BinaryPrimitives.WriteInt32LittleEndian(buffer.AsSpan(FileSizeOffset), fileSize);
        BinaryPrimitives.WriteInt32BigEndian(buffer.AsSpan(WaveFormatOffset), WaveMagic);
    }

    private static void WriteFmtChunk(byte[] buffer, int bitsPerSample)
    {
        BinaryPrimitives.WriteInt32BigEndian(buffer.AsSpan(FmtMagicOffset), FmtMagic);
        BinaryPrimitives.WriteInt32LittleEndian(buffer.AsSpan(FmtSizeOffset), FmtChunkSize);
        BinaryPrimitives.WriteInt16LittleEndian(buffer.AsSpan(PcmFormatOffset), PcmFormat);
        BinaryPrimitives.WriteInt16LittleEndian(buffer.AsSpan(ChannelsOffset), AudioConstants.AudioChannelCount);
        BinaryPrimitives.WriteInt32LittleEndian(buffer.AsSpan(SampleRateOffset), AudioConstants.SampleRate);
        BinaryPrimitives.WriteInt32LittleEndian(buffer.AsSpan(ByteRateOffset), AudioConstants.SampleRate * AudioConstants.AudioChannelCount * bitsPerSample / 8);
        BinaryPrimitives.WriteInt16LittleEndian(buffer.AsSpan(BlockAlignOffset), (short)(AudioConstants.AudioChannelCount * bitsPerSample / 8));
        BinaryPrimitives.WriteInt16LittleEndian(buffer.AsSpan(BitsPerSampleOffset), (short)bitsPerSample);
    }

    private static void WriteDataChunk(byte[] buffer, int dataSize)
    {
        BinaryPrimitives.WriteInt32BigEndian(buffer.AsSpan(DataMagicOffset), DataMagic);
        BinaryPrimitives.WriteInt32LittleEndian(buffer.AsSpan(DataSizeOffset), dataSize);
    }
}
