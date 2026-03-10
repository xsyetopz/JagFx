namespace JagFx.JavaRandom;

/// <summary>
/// Java-compatible Random implementation using java.util.Random algorithm.
/// Provides identical random sequences to Java's Random class for cross-platform compatibility.
/// </summary>
/// <remarks>
/// Creates a new random number generator using a single long seed.
/// </remarks>
public class JavaRandom(long seed)
{
    private long _seed = (seed ^ Multiplier) & Mask;

    private const long Multiplier = 0x5DEECE66DL;
    private const long Addend = 0xBL;
    private const long Mask = (1L << 48) - 1;

    /// <summary>
    /// Creates a new random number generator using a seed based on the current time.
    /// </summary>
    public JavaRandom() : this(DateTime.UtcNow.Ticks)
    {
    }

    /// <summary>
    /// Returns the next pseudorandom, uniformly distributed int value from this random number generator's sequence.
    /// </summary>
    public int Next()
    {
        return NextInt() & int.MaxValue;
    }

    /// <summary>
    /// Returns a pseudorandom, uniformly distributed int value between 0 (inclusive) and the specified value (exclusive).
    /// </summary>
    public int Next(int maxValue)
    {
        if (maxValue <= 0)
            throw new ArgumentOutOfRangeException(nameof(maxValue), "maxValue must be positive");

        if ((maxValue & -maxValue) == maxValue)
            return (int)((maxValue * (long)NextInt()) >> 31) & int.MaxValue;

        int bits, val;
        do
        {
            bits = NextInt() >>> 1;
            val = bits % maxValue;
        } while (bits - val + (maxValue - 1) < 0);

        return val;
    }

    /// <summary>
    /// Returns a pseudorandom, uniformly distributed int value between minValue (inclusive) and maxValue (exclusive).
    /// </summary>
    public int Next(int minValue, int maxValue)
    {
        if (minValue >= maxValue)
            throw new ArgumentOutOfRangeException(nameof(minValue), "minValue must be less than maxValue");

        return minValue + Next(maxValue - minValue);
    }

    /// <summary>
    /// Returns the next pseudorandom, uniformly distributed double value between 0.0 and 1.0.
    /// </summary>
    public double NextDouble()
    {
        return ((NextInt() << 5) + Next(1 << 5)) / (double)(1L << 53);
    }

    /// <summary>
    /// Returns the next pseudorandom, uniformly distributed float value between 0.0 and 1.0.
    /// </summary>
    public float NextSingle()
    {
        return Next(1 << 24) / ((float)(1 << 24));
    }

    /// <summary>
    /// Returns the next pseudorandom, uniformly distributed boolean value.
    /// </summary>
    public bool NextBoolean()
    {
        return (NextInt() & 1) != 0;
    }

    /// <summary>
    /// Generates random bytes and places them into the specified byte array.
    /// </summary>
    public void NextBytes(byte[] buffer)
    {
        for (var i = 0; i < buffer.Length;)
        {
            var rnd = NextInt();
            var n = Math.Min(buffer.Length - i, 4);
            while (n-- > 0)
            {
                buffer[i++] = (byte)rnd;
                rnd >>= 8;
            }
        }
    }

    /// <summary>
    /// Returns the next pseudorandom, uniformly distributed int value.
    /// This is the raw Java-style NextInt that can return negative values.
    /// </summary>
    public int NextInt()
    {
        _seed = (_seed * Multiplier + Addend) & Mask;
        return (int)(_seed >> 16);
    }

    /// <summary>
    /// Sets the seed of this random number generator using a single long seed.
    /// </summary>
    public void SetSeed(long seed)
    {
        _seed = (seed ^ Multiplier) & Mask;
    }
}
