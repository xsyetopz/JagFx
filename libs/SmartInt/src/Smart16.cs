using System;
using System.Buffers.Binary;

namespace SmartInt;

/// <summary>
/// Represents a signed 16-bit smart integer value (-32768 to 16383).
/// </summary>
public readonly struct Smart16 : IEquatable<Smart16>, IComparable<Smart16>,
    IFormattable, ISpanFormattable, ISpanParsable<Smart16>
{
    /// <summary>
    /// The maximum value that can be represented by Smart16.
    /// </summary>
    public const short MaxValue = 16383;

    /// <summary>
    /// The minimum value that can be represented by Smart16.
    /// </summary>
    public const short MinValue = -32768;

    /// <summary>
    /// The threshold for single-byte encoding (bytes 0-127).
    /// </summary>
    /// <remarks>
    /// Java reference: peek &lt; 128 ? readUnsignedByte() - 64 : readUnsignedShort() - 49152
    /// </remarks>
    public const int SmartOneByteThreshold = 128;

    /// <summary>
    /// The offset for single-byte decoding.
    /// </summary>
    /// <remarks>
    /// Java reference: readUnsignedByte() - 64
    /// </remarks>
    public const int SmartOneByteOffset = 64;

    /// <summary>
    /// The offset for two-byte encoding.
    /// </summary>
    public const int SmartTwoByteOffset = 49152;

    private readonly short _value;

    /// <summary>
    /// Gets the underlying short value.
    /// </summary>
    public short Value => _value;

    /// <summary>
    /// Initializes a new instance of the Smart16 struct with the specified value.
    /// </summary>
    /// <param name="value">The value to store.</param>
    /// <exception cref="ArgumentOutOfRangeException">Thrown when value is outside the valid range.</exception>
    public Smart16(short value)
    {
        if (value < MinValue || value > MaxValue)
            throw new ArgumentOutOfRangeException(nameof(value), value, $"Value must be between {MinValue} and {MaxValue}.");
        _value = value;
    }

    #region Encoding/Decoding

    /// <summary>
    /// Decodes a Smart16 value from a read-only span of bytes.
    /// </summary>
    /// <param name="data">The data to decode.</param>
    /// <param name="bytesRead">When this method returns, contains the number of bytes read.</param>
    /// <returns>A new Smart16 instance.</returns>
    /// <exception cref="ArgumentOutOfRangeException">Thrown when data is empty or too short.</exception>
    public static Smart16 FromEncoded(ReadOnlySpan<byte> data, out int bytesRead)
    {
        if (data.IsEmpty)
            throw new ArgumentOutOfRangeException(nameof(data), "Data cannot be empty.");

        var b = data[0];
        if (b < SmartOneByteThreshold)
        {
            bytesRead = 1;
            return new Smart16((short)(b - SmartOneByteOffset));
        }
        else
        {
            if (data.Length < 2)
                throw new ArgumentOutOfRangeException(nameof(data), "Data is too short for 2-byte encoding.");

            bytesRead = 2;
            var encoded = BinaryPrimitives.ReadUInt16BigEndian(data);
            return new Smart16((short)(encoded - SmartTwoByteOffset));
        }
    }

    /// <summary>
    /// Encodes this Smart16 value to a span of bytes.
    /// </summary>
    /// <param name="buffer">The buffer to write to.</param>
    /// <returns>The number of bytes written.</returns>
    /// <exception cref="ArgumentOutOfRangeException">Thrown when buffer is empty or too small.</exception>
    public int Encode(Span<byte> buffer)
    {
        if (buffer.IsEmpty)
            throw new ArgumentOutOfRangeException(nameof(buffer), "Buffer cannot be empty.");

        var value = _value;
        if (value >= -SmartOneByteOffset && value < SmartOneByteOffset)
        {
            buffer[0] = (byte)(value + SmartOneByteOffset);
            return 1;
        }
        else
        {
            if (buffer.Length < 2)
                throw new ArgumentOutOfRangeException(nameof(buffer), "Buffer is too small for 2-byte encoding.");

            var encoded = (ushort)(value + SmartTwoByteOffset);
            BinaryPrimitives.WriteUInt16BigEndian(buffer, encoded);
            return 2;
        }
    }

    /// <summary>
    /// Gets the encoded length in bytes for this Smart16 value.
    /// </summary>
    /// <returns>The encoded length (1 or 2 bytes).</returns>
    public int GetEncodedLength()
    {
        var value = _value;
        return value >= -SmartOneByteThreshold && value < 0 ? 1 : 2;
    }

    /// <summary>
    /// Encodes this Smart16 value to a byte array.
    /// </summary>
    /// <returns>A byte array containing the encoded value.</returns>
    public byte[] ToByteArray()
    {
        var buffer = new byte[GetEncodedLength()];
        Encode(buffer);
        return buffer;
    }

    #endregion

    #region Parsing

    /// <summary>
    /// Parses a string to a Smart16 value.
    /// </summary>
    /// <param name="s">The string to parse.</param>
    /// <returns>A new Smart16 instance.</returns>
    /// <exception cref="ArgumentNullException">Thrown when s is null.</exception>
    /// <exception cref="FormatException">Thrown when s cannot be parsed.</exception>
    public static Smart16 Parse(string s)
    {
        return s == null ? throw new ArgumentNullException(nameof(s)) : Parse(s.AsSpan());
    }

    /// <summary>
    /// Parses a character span to a Smart16 value.
    /// </summary>
    /// <param name="s">The span to parse.</param>
    /// <returns>A new Smart16 instance.</returns>
    /// <exception cref="FormatException">Thrown when s cannot be parsed.</exception>
    public static Smart16 Parse(ReadOnlySpan<char> s)
    {
        return TryParse(s, out var result) ? result : throw new FormatException($"Cannot parse '{s.ToString()}' as a Smart16.");
    }

    /// <summary>
    /// Tries to parse a string to a Smart16 value.
    /// </summary>
    /// <param name="s">The string to parse.</param>
    /// <param name="result">When this method returns, contains the parsed value if successful.</param>
    /// <returns>True if parsing succeeded; otherwise, false.</returns>
    public static bool TryParse(string? s, out Smart16 result)
    {
        if (s == null)
        {
            result = default;
            return false;
        }
        return TryParse(s.AsSpan(), out result);
    }

    /// <summary>
    /// Tries to parse a character span to a Smart16 value.
    /// </summary>
    /// <param name="s">The span to parse.</param>
    /// <param name="result">When this method returns, contains the parsed value if successful.</param>
    /// <returns>True if parsing succeeded; otherwise, false.</returns>
    public static bool TryParse(ReadOnlySpan<char> s, out Smart16 result)
    {
        result = default;
        if (s.IsEmpty)
            return false;

        if (short.TryParse(s, out var shortValue))
        {
            if (shortValue >= MinValue && shortValue <= MaxValue)
            {
                result = new Smart16(shortValue);
                return true;
            }
        }
        return false;
    }

    #endregion

    #region Interface Implementations

    /// <summary>
    /// Compares this instance to another Smart16 instance.
    /// </summary>
    /// <param name="other">The other instance.</param>
    /// <returns>A value indicating relative ordering.</returns>
    public int CompareTo(Smart16 other)
    {
        return _value.CompareTo(other._value);
    }

    /// <summary>
    /// Compares this instance to another object.
    /// </summary>
    /// <param name="obj">The object to compare to.</param>
    /// <returns>A value indicating relative ordering.</returns>
    public int CompareTo(object? obj)
    {
        return obj is Smart16 other ? CompareTo(other) : throw new ArgumentException($"Object must be of type {nameof(Smart16)}.");
    }

    /// <summary>
    /// Determines whether this instance is equal to another Smart16 instance.
    /// </summary>
    /// <param name="other">The other instance.</param>
    /// <returns>True if equal; otherwise, false.</returns>
    public bool Equals(Smart16 other)
    {
        return _value == other._value;
    }

    /// <summary>
    /// Determines whether this instance is equal to another object.
    /// </summary>
    /// <param name="obj">The object to compare to.</param>
    /// <returns>True if equal; otherwise, false.</returns>
    public override bool Equals(object? obj)
    {
        return obj is Smart16 other && Equals(other);
    }

    /// <summary>
    /// Gets the hash code for this instance.
    /// </summary>
    /// <returns>The hash code.</returns>
    public override int GetHashCode()
    {
        return _value.GetHashCode();
    }

    /// <summary>
    /// Converts this instance to a string.
    /// </summary>
    /// <returns>The string representation.</returns>
    public override string ToString()
    {
        return _value.ToString();
    }

    /// <summary>
    /// Converts this instance to a string using the specified format.
    /// </summary>
    /// <param name="format">The format string.</param>
    /// <returns>The string representation.</returns>
    public string ToString(string? format)
    {
        return _value.ToString(format);
    }

    /// <summary>
    /// Converts this instance to a string using the specified format provider.
    /// </summary>
    /// <param name="provider">The format provider.</param>
    /// <returns>The string representation.</returns>
    public string ToString(IFormatProvider? provider)
    {
        return _value.ToString(provider);
    }

    /// <summary>
    /// Converts this instance to a string using the specified format and format provider.
    /// </summary>
    /// <param name="format">The format string.</param>
    /// <param name="formatProvider">The format provider.</param>
    /// <returns>The string representation.</returns>
    public string ToString(string? format, IFormatProvider? formatProvider)
    {
        return _value.ToString(format, formatProvider);
    }

    /// <summary>
    /// Tries to format this instance into a character span.
    /// </summary>
    /// <param name="destination">The destination buffer.</param>
    /// <param name="charsWritten">When this method returns, contains the number of characters written.</param>
    /// <param name="format">The format string.</param>
    /// <param name="provider">The format provider.</param>
    /// <returns>True if formatting succeeded; otherwise, false.</returns>
    public bool TryFormat(Span<char> destination, out int charsWritten, ReadOnlySpan<char> format = default, IFormatProvider? provider = null)
    {
#if NET8_0_OR_GREATER
        var spanFormattable = (ISpanFormattable)_value;
        return spanFormattable.TryFormat(destination, out charsWritten, format, provider);
#else
        var result = _value.ToString(format.ToString(), provider);
        if (result.Length > destination.Length)
        {
            charsWritten = 0;
            return false;
        }
        result.CopyTo(destination);
        charsWritten = result.Length;
        return true;
#endif
    }

#if NET8_0_OR_GREATER
    static Smart16 ISpanParsable<Smart16>.Parse(ReadOnlySpan<char> s, IFormatProvider? provider)
    {
        return Parse(s);
    }

    static bool ISpanParsable<Smart16>.TryParse(ReadOnlySpan<char> s, IFormatProvider? provider, out Smart16 result)
    {
        return TryParse(s, out result);
    }

    static Smart16 IParsable<Smart16>.Parse(string s, IFormatProvider? provider)
    {
        return Parse(s);
    }

    static bool IParsable<Smart16>.TryParse(string? s, IFormatProvider? provider, out Smart16 result)
    {
        return TryParse(s ?? string.Empty, out result);
    }
#endif

    #endregion

    #region Operators

    /// <summary>
    /// Equality operator.
    /// </summary>
    public static bool operator ==(Smart16 left, Smart16 right)
    {
        return left.Equals(right);
    }

    /// <summary>
    /// Inequality operator.
    /// </summary>
    public static bool operator !=(Smart16 left, Smart16 right)
    {
        return !left.Equals(right);
    }

    /// <summary>
    /// Less than operator.
    /// </summary>
    public static bool operator <(Smart16 left, Smart16 right)
    {
        return left.CompareTo(right) < 0;
    }

    /// <summary>
    /// Greater than operator.
    /// </summary>
    public static bool operator >(Smart16 left, Smart16 right)
    {
        return left.CompareTo(right) > 0;
    }

    /// <summary>
    /// Less than or equal operator.
    /// </summary>
    public static bool operator <=(Smart16 left, Smart16 right)
    {
        return left.CompareTo(right) <= 0;
    }

    /// <summary>
    /// Greater than or equal operator.
    /// </summary>
    public static bool operator >=(Smart16 left, Smart16 right)
    {
        return left.CompareTo(right) >= 0;
    }

    #endregion

    #region Conversions

    /// <summary>
    /// Explicit conversion from Smart16 to short.
    /// </summary>
    public static explicit operator short(Smart16 value) => value._value;

    /// <summary>
    /// Explicit conversion from Smart16 to int.
    /// </summary>
    public static explicit operator int(Smart16 value) => value._value;

    /// <summary>
    /// Explicit conversion from Smart16 to long.
    /// </summary>
    public static explicit operator long(Smart16 value) => value._value;

    /// <summary>
    /// Implicit conversion from short to Smart16.
    /// </summary>
    public static implicit operator Smart16(short value) => new Smart16(value);

    #endregion
}
