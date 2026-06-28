# SmartInt

Jagex Smart integer encoding helpers for .NET.

`SmartInt` provides small value types for the variable-length integer encodings
used in OSRS cache and synthesizer data. Values encode to one or two bytes depending on their range.

## Usage

```csharp
using SmartInt;

var value = new USmart16(64);
Span<byte> buffer = stackalloc byte[2];
int bytesWritten = value.Encode(buffer);

var decoded = USmart16.FromEncoded(buffer[..bytesWritten], out int bytesRead);
```

## Types

- `USmart16` stores unsigned Smart values from `0` through `32767`.
- `Smart16` stores signed Smart values from `-32768` through `16383`.

Both types support parsing, formatting, comparison, byte-array conversion, and span-based encoding.
