using SmartInt;
using System.Buffers.Binary;

namespace JagFx.Io.Buffers;

public class BinaryBuffer
{
    private int _position;
    private bool _truncated;

    public BinaryBuffer(byte[] data) => Data = data ?? throw new ArgumentNullException(nameof(data));
    public BinaryBuffer(int size) => Data = new byte[size];

    public byte[] Data { get; }
    public int Position => _position;
    public bool IsTruncated => _truncated;
    public int Remaining => Data.Length - _position;

    public void Skip(int byteCount) => _position += byteCount;

    public void SetPosition(int newPosition)
    {
        if (newPosition < 0)
            _position = 0;
        else if (newPosition > Data.Length)
            _position = Data.Length;
        else
            _position = newPosition;
    }

    public int Peek() => _position >= Data.Length ? 0 : Data[_position] & 0xFF;

    public int PeekAt(int offset)
    {
        var peekPosition = _position + offset;
        return peekPosition >= Data.Length ? 0 : Data[peekPosition] & 0xFF;
    }

    public int ReadUInt8()
    {
        if (CheckTruncation(1)) return 0;

        var unsignedByte = Data[_position] & 0xFF;
        _position++;

        return unsignedByte;
    }

    public int ReadInt8()
    {
        if (CheckTruncation(1)) return 0;

        var signedByte = Data[_position];
        _position++;

        return signedByte;
    }

    public int ReadUInt16BigEndian()
    {
        if (CheckTruncation(2)) return 0;

        var unsignedShort = BinaryPrimitives.ReadUInt16BigEndian(Data.AsSpan(_position, 2));
        _position += 2;

        return unsignedShort;
    }

    public int ReadUInt16LittleEndian()
    {
        if (CheckTruncation(2)) return 0;

        var unsignedShort = BinaryPrimitives.ReadUInt16LittleEndian(Data.AsSpan(_position, 2));
        _position += 2;

        return unsignedShort;
    }

    public int ReadInt16BigEndian()
    {
        if (CheckTruncation(2)) return 0;

        var signedShort = BinaryPrimitives.ReadInt16BigEndian(Data.AsSpan(_position, 2));
        _position += 2;

        return signedShort;
    }

    public int ReadInt32BigEndian()
    {
        if (CheckTruncation(4)) return 0;

        var signedInt = BinaryPrimitives.ReadInt32BigEndian(Data.AsSpan(_position, 4));
        _position += 4;

        return signedInt;
    }

    public short ReadSmart()
    {
        return ReadSmartBase(
            static (span) => (Smart16.FromEncoded(span, out var bytesRead).Value, bytesRead),
            Smart16.SmartOneByteThreshold
        );
    }

    public ushort ReadUSmart()
    {
        return ReadSmartBase(
            static (span) => (USmart16.FromEncoded(span, out var bytesRead).Value, bytesRead),
            USmart16.USmartOneByteThreshold
        );
    }

    public void WriteInt32BigEndian(int value)
    {
        BinaryPrimitives.WriteInt32BigEndian(Data.AsSpan(_position, 4), value);
        _position += 4;
    }

    public void WriteInt32LittleEndian(int value)
    {
        BinaryPrimitives.WriteInt32LittleEndian(Data.AsSpan(_position, 4), value);
        _position += 4;
    }

    public void WriteInt16LittleEndian(int value)
    {
        BinaryPrimitives.WriteInt16LittleEndian(Data.AsSpan(_position, 2), (short)value);
        _position += 2;
    }

    public void WriteUInt8(int value)
    {
        Data[_position] = (byte)value;
        _position++;
    }

    public void WriteUInt16BigEndian(int value)
    {
        BinaryPrimitives.WriteUInt16BigEndian(Data.AsSpan(_position, 2), (ushort)value);
        _position += 2;
    }

    public void WriteSmart16(short value)
    {
        var smart = new Smart16(value);
        _position += smart.Encode(Data.AsSpan(_position));
    }

    public void WriteUSmart16(ushort value)
    {
        var smart = new USmart16(value);
        _position += smart.Encode(Data.AsSpan(_position));
    }

    private delegate (T Smart, int BytesRead) SmartDecoder<T>(ReadOnlySpan<byte> span);

    private T ReadSmartBase<T>(SmartDecoder<T> decodeSmart, int oneByteThreshold)
    {
        if (_position >= Data.Length)
            return default!;

        var firstByte = Data[_position] & 0xFF;
        var bytesNeeded = firstByte < oneByteThreshold ? 1 : 2;

        if (CheckTruncation(bytesNeeded))
            return default!;

        var (smartValue, bytesRead) = decodeSmart(Data.AsSpan(_position));
        _position += bytesRead;

        return smartValue;
    }

    private bool CheckTruncation(int bytesRequired)
    {
        if (_position + bytesRequired > Data.Length)
        {
            _truncated = true;
            _position += bytesRequired;
            return true;
        }
        return false;
    }
}
