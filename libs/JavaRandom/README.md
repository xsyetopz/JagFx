# JagFx.JavaRandom

Java-compatible pseudorandom number generation for .NET.

`JagFx.JavaRandom` implements the same linear congruential generator used by
`java.util.Random`. Use it when a .NET tool needs to reproduce Java or OSRS client random sequences exactly.

## Usage

```csharp
using JagFx.JavaRandom;

var random = new JavaRandom(12345L);

int value = random.NextInt();
int bounded = random.Next(0, 10);
float sample = random.NextSingle();
```

## API

- `JavaRandom(long seed)` creates a generator from a Java-style seed.
- `NextInt()` returns the next signed 32-bit Java random value.
- `Next(int minValue, int maxValue)` returns a bounded integer.
- `NextSingle()` returns a `float` in the range `[0.0, 1.0)`.

The implementation is deterministic. The same seed produces the same sequence as Java's `java.util.Random`.
