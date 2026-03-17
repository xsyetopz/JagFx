using System;
using System.Buffers.Binary;

namespace SmartInt;

/// <summary>
/// Represents an unsigned 16-bit smart integer value (0 to 32767).
/// </summary>
/// <remarks>
/// Initializes a new instance of the USmart16 struct with the specified value.
/// </remarks>
/// <param name="value">The value to store.</param>
/// <exception cref="ArgumentOutOfRangeException">Thrown when value is outside the valid range.</exception>
public readonly struct USmart16(ushort value) : IEquatable<USmart16>, IComparable<USmart16>,
    IFormattable, ISpanFormattable, ISpanParsable<USmart16>
{
    /// <summary>
    /// The maximum value that can be represented by USmart16.
    /// </summary>
    public const ushort MaxValue = 32767;

    /// <summary>
    /// The minimum value that can be represented by USmart16.
    /// </summary>
    public const ushort MinValue = 0;

    /// <summary>
    /// The threshold for single-byte encoding (values 0 to 127).
    /// </summary>
    public const int USmartOneByteThreshold = 128;

    /// <summary>
    /// The offset for two-byte encoding (values 0 to 32768).
    /// </summary>
    public const int USmartTwoByteOffset = 32768;

    private readonly ushort _value = value;

    /// <summary>
    /// Gets the underlying ushort value.
    /// </summary>
    public ushort Value => _value;

    #region Encoding/Decoding

    /// <summary>
    /// Decodes a USmart16 value from a read-only span of bytes.
    /// </summary>
    /// <param name="data">The data to decode.</param>
    /// <param name="bytesRead">When this method returns, contains the number of bytes read.</param>
    /// <returns>A new USmart16 instance.</returns>
    /// <exception cref="ArgumentOutOfRangeException">Thrown when data is empty.</exception>
    public static USmart16 FromEncoded(ReadOnlySpan<byte> data, out int bytesRead)
    {
        if (data.IsEmpty)
            throw new ArgumentOutOfRangeException(nameof(data), "Data cannot be empty.");

        var b = data[0];
        if (b < USmartOneByteThreshold)
        {
            bytesRead = 1;
            return new USmart16(b);
        }
        else
        {
            if (data.Length < 2)
                throw new ArgumentOutOfRangeException(nameof(data), "Data is too short for 2-byte encoding.");

            bytesRead = 2;
            var encoded = BinaryPrimitives.ReadUInt16BigEndian(data);
            return new USmart16((ushort)(encoded - USmartTwoByteOffset));
        }
    }

    /// <summary>
    /// Encodes this USmart16 value to a span of bytes.
    /// </summary>
    /// <param name="buffer">The buffer to write to.</param>
    /// <returns>The number of bytes written.</returns>
    /// <exception cref="ArgumentOutOfRangeException">Thrown when buffer is empty or too small.</exception>
    public int Encode(Span<byte> buffer)
    {
        if (buffer.IsEmpty)
            throw new ArgumentOutOfRangeException(nameof(buffer), "Buffer cannot be empty.");

        var value = _value;
        if (value < USmartOneByteThreshold)
        {
            buffer[0] = (byte)value;
            return 1;
        }
        else
        {
            if (buffer.Length < 2)
                throw new ArgumentOutOfRangeException(nameof(buffer), "Buffer is too small for 2-byte encoding.");

            var encoded = (ushort)(value + USmartTwoByteOffset);
            BinaryPrimitives.WriteUInt16BigEndian(buffer, encoded);
            return 2;
        }
    }

    /// <summary>
    /// Gets the encoded length in bytes for this USmart16 value.
    /// </summary>
    /// <returns>The encoded length (1 or 2 bytes).</returns>
    public int GetEncodedLength()
    {
        return _value < USmartOneByteThreshold ? 1 : 2;
    }

    /// <summary>
    /// Encodes this USmart16 value to a byte array.
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
    /// Parses a string to a USmart16 value.
    /// </summary>
    /// <param name="s">The string to parse.</param>
    /// <returns>A new USmart16 instance.</returns>
    /// <exception cref="ArgumentNullException">Thrown when s is null.</exception>
    /// <exception cref="FormatException">Thrown when s cannot be parsed.</exception>
    public static USmart16 Parse(string s)
    {
        return s == null ? throw new ArgumentNullException(nameof(s)) : Parse(s.AsSpan());
    }

    /// <summary>
    /// Parses a character span to a USmart16 value.
    /// </summary>
    /// <param name="s">The span to parse.</param>
    /// <returns>A new USmart16 instance.</returns>
    /// <exception cref="FormatException">Thrown when s cannot be parsed.</exception>
    public static USmart16 Parse(ReadOnlySpan<char> s)
    {
        return TryParse(s, out var result) ? result : throw new FormatException($"Cannot parse '{s.ToString()}' as a USmart16.");
    }

    /// <summary>
    /// Tries to parse a string to a USmart16 value.
    /// </summary>
    /// <param name="s">The string to parse.</param>
    /// <param name="result">When this method returns, contains the parsed value if successful.</param>
    /// <returns>True if parsing succeeded; otherwise, false.</returns>
    public static bool TryParse(string? s, out USmart16 result)
    {
        if (s == null)
        {
            result = default;
            return false;
        }
        return TryParse(s.AsSpan(), out result);
    }

    /// <summary>
    /// Tries to parse a character span to a USmart16 value.
    /// </summary>
    /// <param name="s">The span to parse.</param>
    /// <param name="result">When this method returns, contains the parsed value if successful.</param>
    /// <returns>True if parsing succeeded; otherwise, false.</returns>
    public static bool TryParse(ReadOnlySpan<char> s, out USmart16 result)
    {
        result = default;
        if (s.IsEmpty)
            return false;

        if (ushort.TryParse(s, out var ushortValue))
        {
            if (ushortValue >= MinValue && ushortValue <= MaxValue)
            {
                result = new USmart16(ushortValue);
                return true;
            }
        }
        return false;
    }

    #endregion

    #region Interface Implementations

    /// <summary>
    /// Compares this instance to another USmart16 instance.
    /// </summary>
    /// <param name="other">The other instance.</param>
    /// <returns>A value indicating relative ordering.</returns>
    public int CompareTo(USmart16 other)
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
        return obj is USmart16 other ? CompareTo(other) : throw new ArgumentException($"Object must be of type {nameof(USmart16)}.");
    }

    /// <summary>
    /// Determines whether this instance is equal to another USmart16 instance.
    /// </summary>
    /// <param name="other">The other instance.</param>
    /// <returns>True if equal; otherwise, false.</returns>
    public bool Equals(USmart16 other)
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
        return obj is USmart16 other && Equals(other);
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
    static USmart16 ISpanParsable<USmart16>.Parse(ReadOnlySpan<char> s, IFormatProvider? provider)
    {
        return Parse(s);
    }

    static bool ISpanParsable<USmart16>.TryParse(ReadOnlySpan<char> s, IFormatProvider? provider, out USmart16 result)
    {
        return TryParse(s, out result);
    }

    static USmart16 IParsable<USmart16>.Parse(string s, IFormatProvider? provider)
    {
        return Parse(s);
    }

    static bool IParsable<USmart16>.TryParse(string? s, IFormatProvider? provider, out USmart16 result)
    {
        return TryParse(s ?? string.Empty, out result);
    }
#endif

    #endregion

    #region Operators

    /// <summary>
    /// Equality operator.
    /// </summary>
    public static bool operator ==(USmart16 left, USmart16 right)
    {
        return left.Equals(right);
    }

    /// <summary>
    /// Inequality operator.
    /// </summary>
    public static bool operator !=(USmart16 left, USmart16 right)
    {
        return !left.Equals(right);
    }

    /// <summary>
    /// Less than operator.
    /// </summary>
    public static bool operator <(USmart16 left, USmart16 right)
    {
        return left.CompareTo(right) < 0;
    }

    /// <summary>
    /// Greater than operator.
    /// </summary>
    public static bool operator >(USmart16 left, USmart16 right)
    {
        return left.CompareTo(right) > 0;
    }

    /// <summary>
    /// Less than or equal operator.
    /// </summary>
    public static bool operator <=(USmart16 left, USmart16 right)
    {
        return left.CompareTo(right) <= 0;
    }

    /// <summary>
    /// Greater than or equal operator.
    /// </summary>
    public static bool operator >=(USmart16 left, USmart16 right)
    {
        return left.CompareTo(right) >= 0;
    }

    #endregion

    #region Conversions

    /// <summary>
    /// Explicit conversion from USmart16 to ushort.
    /// </summary>
    public static explicit operator ushort(USmart16 value) => value._value;

    /// <summary>
    /// Explicit conversion from USmart16 to int.
    /// </summary>
    public static explicit operator int(USmart16 value) => value._value;

    /// <summary>
    /// Explicit conversion from USmart16 to uint.
    /// </summary>
    public static explicit operator uint(USmart16 value) => value._value;

    /// <summary>
    /// Implicit conversion from ushort to USmart16.
    /// </summary>
    public static implicit operator USmart16(ushort value) => new USmart16(value);

    #endregion
}
