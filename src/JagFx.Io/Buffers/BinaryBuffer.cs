using System.Buffers.Binary;
using SmartInt;

namespace JagFx.Io.Buffers;

public class BinaryBuffer
{
    public BinaryBuffer(byte[] bytes)
    {
        Bytes = bytes ?? throw new ArgumentNullException(nameof(bytes));
    }

    public BinaryBuffer(int size)
    {
        Bytes = new byte[size];
    }

    public byte[] Bytes { get; }
    public int Position { get; private set; }
    public bool IsTruncated { get; private set; }
    public int Remaining => Bytes.Length - Position;

    public void Skip(int byteCount) => Position += byteCount;

    public void SetPosition(int newPosition)
    {
        Position =
            newPosition < 0 ? 0
            : newPosition > Bytes.Length ? Bytes.Length
            : newPosition;
    }

    public int Peek() => Position >= Bytes.Length ? 0 : Bytes[Position] & 0xFF;

    public int PeekAt(int offset)
    {
        var peekPosition = Position + offset;
        return peekPosition >= Bytes.Length ? 0 : Bytes[peekPosition] & 0xFF;
    }

    public int ReadUInt8()
    {
        if (CheckTruncation(1))
        {
            return 0;
        }

        var unsignedByte = Bytes[Position] & 0xFF;
        Position++;

        return unsignedByte;
    }

    public int ReadInt8()
    {
        if (CheckTruncation(1))
        {
            return 0;
        }

        var signedByte = Bytes[Position];
        Position++;

        return signedByte;
    }

    public int ReadUInt16BigEndian()
    {
        if (CheckTruncation(2))
        {
            return 0;
        }

        var unsignedShort = BinaryPrimitives.ReadUInt16BigEndian(Bytes.AsSpan(Position, 2));
        Position += 2;

        return unsignedShort;
    }

    public int ReadUInt16LittleEndian()
    {
        if (CheckTruncation(2))
        {
            return 0;
        }

        var unsignedShort = BinaryPrimitives.ReadUInt16LittleEndian(Bytes.AsSpan(Position, 2));
        Position += 2;

        return unsignedShort;
    }

    public int ReadInt16BigEndian()
    {
        if (CheckTruncation(2))
        {
            return 0;
        }

        var signedShort = BinaryPrimitives.ReadInt16BigEndian(Bytes.AsSpan(Position, 2));
        Position += 2;

        return signedShort;
    }

    public int ReadInt32BigEndian()
    {
        if (CheckTruncation(4))
        {
            return 0;
        }

        var signedInt = BinaryPrimitives.ReadInt32BigEndian(Bytes.AsSpan(Position, 4));
        Position += 4;

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
        BinaryPrimitives.WriteInt32BigEndian(Bytes.AsSpan(Position, 4), value);
        Position += 4;
    }

    public void WriteInt32LittleEndian(int value)
    {
        BinaryPrimitives.WriteInt32LittleEndian(Bytes.AsSpan(Position, 4), value);
        Position += 4;
    }

    public void WriteInt16LittleEndian(int value)
    {
        BinaryPrimitives.WriteInt16LittleEndian(Bytes.AsSpan(Position, 2), (short)value);
        Position += 2;
    }

    public void WriteUInt8(int value)
    {
        Bytes[Position] = (byte)value;
        Position++;
    }

    public void WriteUInt16BigEndian(int value)
    {
        BinaryPrimitives.WriteUInt16BigEndian(Bytes.AsSpan(Position, 2), (ushort)value);
        Position += 2;
    }

    public void WriteSmart16(short value)
    {
        var smart = new Smart16(value);
        Position += smart.Encode(Bytes.AsSpan(Position));
    }

    public void WriteUSmart16(ushort value)
    {
        var smart = new USmart16(value);
        Position += smart.Encode(Bytes.AsSpan(Position));
    }

    private delegate (T Smart, int BytesRead) SmartDecoder<T>(ReadOnlySpan<byte> span);

    private T ReadSmartBase<T>(SmartDecoder<T> decodeSmart, int oneByteThreshold)
    {
        if (Position >= Bytes.Length)
        {
            return default!;
        }

        var firstByte = Bytes[Position] & 0xFF;
        var bytesNeeded = firstByte < oneByteThreshold ? 1 : 2;

        if (CheckTruncation(bytesNeeded))
        {
            return default!;
        }

        var (smartValue, bytesRead) = decodeSmart(Bytes.AsSpan(Position));
        Position += bytesRead;

        return smartValue;
    }

    private bool CheckTruncation(int bytesRequired)
    {
        if (Position + bytesRequired > Bytes.Length)
        {
            IsTruncated = true;
            Position += bytesRequired;
            return true;
        }
        return false;
    }
}
