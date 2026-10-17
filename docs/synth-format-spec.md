# JagFx `.synth` format: formal specification

| Item | Value |
| --- | --- |
| Specification version | 3.0.0 |
| Subject | Old School RuneScape sound-effect synthesizer files (`.synth`) |
| Reference implementation | JagFx 3.0.0 (Kotlin) |
| Earlier editor | A C# editor for .NET 8. Section 12 lists its deviations. |

## 0. Document conventions

### 0.1 Normative and informative text

Sections 1 to 11 are normative. Sections 12 and 13 are informative. Text with the label "Note" is informative in all sections.

### 0.2 Requirement words

| Word | Meaning |
| --- | --- |
| must | The item is a requirement. |
| must not | The item is a prohibition. |
| can | The item is a permitted option. |

### 0.3 Notation

All numeric ranges include their two end values.

| Symbol | Meaning |
| --- | --- |
| `i32`, `i64` | Two's-complement integers with 32 and 64 bits. |
| `u8`, `u16` | Unsigned integers with 8 and 16 bits. |
| `f32`, `f64` | IEEE 754 binary32 and binary64 values. |
| `wrap32(x)` | The unique `i32` value that is congruent to `x` modulo `2^32`. |
| `x >> k` | Arithmetic right shift. The result is `floor(x / 2^k)`. |
| `x ÷ y` | Integer division. The result rounds toward zero. |
| `x & y`, `x \| y` | Bitwise AND and bitwise OR on two's-complement values. |
| `~x` | Bitwise NOT. The result is `-x - 1`. |
| `clamp(x, a, b)` | `a` if `x < a`, `b` if `x > b`, else `x`. |
| `toI32(x)` | Conversion of `x` to `i32`. The conversion rounds toward zero. A NaN gives `0`. A value outside the `i32` range gives the nearest `i32` limit. |
| `fl32(e)` | The expression `e`, with each operation done in `f32` arithmetic. |
| `B[k]` | Byte `k` of the byte string `B`. The first byte has the index `0`. |
| `\|B\|` | The length of `B` in bytes. |
| `a ++ b` | Concatenation of two sequences. |
| `x[a..b)` | The elements of `x` with an index from `a` to `b - 1`. |

Integer arithmetic on `i32` operands gives an `i32` result through `wrap32`, unless a section states a different width. Arithmetic on `f64` operands follows IEEE 754 round-to-nearest-even, one operation at a time. Implementations must not fuse a multiplication and an addition into one operation.

### 0.4 Terms

| Term | Definition |
| --- | --- |
| Patch | The abstract value that one `.synth` file encodes. Section 4 defines it. |
| Voice | One of ten independent synthesizer channels in a patch. |
| Slot | The position of a voice in the patch, from `0` to `9`. |
| Envelope | A piecewise-linear control curve with a waveform id. |
| Pair | Two envelopes that have one function together, for example an LFO. |
| Partial | One oscillator inside a voice. Other documents use the word "harmonic". |
| Pole | One resonance of the filter. Each pole has a phase and a magnitude. |
| Direction | The feedforward side (`d = 0`) or the feedback side (`d = 1`) of the filter. |
| Filter phase | The filter state at the start of the voice (`φ = 0`) or at the end of the voice (`φ = 1`). |
| Decoder | A function from byte strings to patches. Section 6 defines it. |
| Encoder | A function from patches to byte strings. Section 7 defines it. |
| Renderer | A function from patches to PCM samples. Sections 8 and 9 define it. |

## 1. Scope

This specification defines these four items:

1. The abstract patch model and its invariants.
1. The binary encoding of a patch.
1. The decoder and the encoder as functions.
1. The rendering of a patch to 16-bit PCM audio at 22,050 Hz.

Section 5.2 also defines the container of the recorded sample that some sound effects use, and its loop playback.

This specification does not define the JSON interchange format. `schemas/synth.schema.json` defines it.

## 2. Conformance

| Class | Requirement |
| --- | --- |
| Conforming file | A byte string `B` is a conforming file if `B = encode(decode(B))`. |
| Conforming decoder | The decoder must give the same patch, warnings, and errors as Section 6 for all byte strings. |
| Conforming encoder | The encoder must give the same bytes as Section 7 for all valid patches. The encoder must reject all patches that are not valid. |
| Conforming renderer | The renderer must give the same samples as Sections 8 and 9 for all valid patches. |

A patch is valid if it satisfies all invariants in Section 4.3.

## 3. Primitive encodings

### 3.1 Fixed-width integers

All multi-byte integers are big-endian.

| Type | Width | Value from bytes `b0, b1, ...` |
| --- | --- | --- |
| `u8` | 1 | `b0` |
| `u16` | 2 | `b0 × 256 + b1` |
| `s32` | 4 | `wrap32(b0 × 2^24 + b1 × 2^16 + b2 × 2^8 + b3)` |

### 3.2 `usmart16`

Domain: `0` to `32767`.

```text
decode:  b0 < 128  ->  value = b0                        (1 byte)
         b0 >= 128 ->  value = u16(b0, b1) - 32768       (2 bytes)

encode:  0 <= v <= 127      ->  u8(v)
         128 <= v <= 32767  ->  u16(v + 32768)
```

### 3.3 `smart16`

Domain: `-16384` to `16383`.

```text
decode:  b0 < 128  ->  value = b0 - 64                   (1 byte)
         b0 >= 128 ->  value = u16(b0, b1) - 49152       (2 bytes)

encode:  -64 <= v <= 63                         ->  u8(v + 64)
         -16384 <= v <= -65 or 64 <= v <= 16383  ->  u16(v + 49152)
```

Note: The decoder accepts a two-byte form for a value that has a one-byte form. The encoder always writes the shortest form. A file that contains a longer form is therefore not a conforming file.

## 4. Abstract model

### 4.1 Types

```text
Patch     = (voices: Voice?[10], loop: Loop)
Loop      = (LoopBegin: u16, LoopEnd: u16)

Voice     = (FrequencyEnvelope: Envelope,
             AmplitudeEnvelope: Envelope,
             PitchLfo:          Pair?,
             AmplitudeLfo:      Pair?,
             Gate:              Pair?,
             Partials:          Partial[0..5],
             EchoDelay:         0..32767,
             EchoFeedback:      0..32767,
             Duration:          u16,
             Offset:            u16,
             Filter:            Filter?)

Pair      = (first: Envelope, second: Envelope)
Envelope  = (Waveform: Waveform, StartValue: i32, EndValue: i32,
             Segments: Segment[0..255])
Segment   = (Duration: u16, TargetLevel: u16)
Waveform  = Off (0) | Square (1) | Sine (2) | Saw (3) | Noise (4) | Undefined (5..255)

Partial   = (Amplitude: 1..32767, PitchOffset: -16384..16383,
             Delay: 0..32767)

Filter    = (PoleCount: [0..4, 0..4],
             UnityGain0: u16, UnityGain1: u16,
             Phase:     u16[d][φ][p],
             Magnitude: u16[d][φ][p],
             ModulationEnvelope: Segment[0..255]?)
```

In `Filter`, `d` is the direction, `φ` is the filter phase, and `p` is the pole index. The indices have these ranges: `d` in `0..1`, `φ` in `0..1`, `p` in `0..PoleCount[d] - 1`.

### 4.2 Field meanings

| Field | Unit | Meaning |
| --- | --- | --- |
| `FrequencyEnvelope` | raw | Pitch curve. Its `Waveform` selects the oscillator waveform of all partials. |
| `AmplitudeEnvelope` | raw | Loudness curve. Its `Waveform` has no effect on rendering. |
| `PitchLfo.first` | raw | Vibrato rate envelope. Its `Waveform` selects the LFO waveform. |
| `PitchLfo.second` | raw | Vibrato depth envelope. |
| `AmplitudeLfo.first` | raw | Tremolo rate envelope. Its `Waveform` selects the LFO waveform. |
| `AmplitudeLfo.second` | raw | Tremolo depth envelope. |
| `Gate.first` | raw | `GapOff`. The gate threshold envelope for the muted state. |
| `Gate.second` | raw | `GapOn`. The gate threshold envelope for the open state. |
| `Partial.Amplitude` | 1/100 of full scale | Partial gain. |
| `Partial.PitchOffset` | 1/10 semitone | Pitch offset of the partial. Ten units are one semitone. |
| `Partial.Delay` | ms | Start delay of the partial in the voice. |
| `EchoDelay` | ms | Echo delay. |
| `EchoFeedback` | 1/100 | Echo feedback gain. |
| `Duration` | ms | Voice length. |
| `Offset` | ms | Voice start time in the patch. |
| `Segment.Duration` | 1/65536 of the voice | End time of the segment. |
| `Segment.TargetLevel` | raw | Envelope value at the end time of the segment. |
| `UnityGain0`, `UnityGain1` | raw | Filter gain at the start and at the end of the voice. |
| `LoopBegin`, `LoopEnd` | ms | Loop region of the patch. |

### 4.3 Invariants

A patch is valid if each of these invariants is true.

1. **I1.** For each voice, `FrequencyEnvelope.Waveform ≠ Off`.
1. **I2.** For each pair, `first.Waveform ≠ Off`.
1. **I3.** For each filter, `PoleCount[0] + PoleCount[1] ≥ 1`.
1. **I4.** For each filter, `ModulationEnvelope` is present if and only if `Mask(F) ≠ 0` or `UnityGain0 ≠ UnityGain1`.
This definition applies to I4:

```text
Mask(F) = sum over d in 0..1, p in 0..PoleCount[d]-1 of
          2^(4d + p)   if (Phase[d][1][p], Magnitude[d][1][p])
                            ≠ (Phase[d][0][p], Magnitude[d][0][p])
          0            otherwise
```

Note: I1 and I2 exist because the waveform byte is also the presence marker of a voice and of a pair.

## 5. Concrete syntax

This grammar uses EBNF. A quoted value is one byte with that value. `X{n}` is exactly `n` repetitions of `X`.

```text
File          = Slot{10} Loop
Slot          = '0x00' | Voice
Loop          = LoopBegin:u16 LoopEnd:u16

Voice         = FrequencyEnvelope:Envelope AmplitudeEnvelope:Envelope
                PitchLfo:OptPair AmplitudeLfo:OptPair Gate:OptPair
                PartialList
                EchoDelay:usmart16 EchoFeedback:usmart16
                Duration:u16 Offset:u16
                OptFilter

Envelope      = Waveform:u8 StartValue:s32 EndValue:s32
                SegmentCount:u8 Segment{SegmentCount}
Segment       = Duration:u16 TargetLevel:u16

OptPair       = '0x00' | Envelope Envelope

PartialList   = Partial{k} '0x00'                 (k from 0 to 5)
Partial       = Amplitude:usmart16 PitchOffset:smart16 Delay:usmart16

OptFilter     = '0x00' | Filter
Filter        = PackedPoleCounts:u8 UnityGain0:u16 UnityGain1:u16
                ModulationMask:u8
                Coefficient{P0 + P1}              (filter phase 0)
                Coefficient{popcount(ModulationMask)}   (filter phase 1)
                [ModulationEnvelope]
Coefficient   = Phase:u16 Magnitude:u16
ModulationEnvelope = SegmentCount:u8 Segment{SegmentCount}
```

These context rules apply to the grammar:

1. The first byte of a `Voice` is `FrequencyEnvelope.Waveform`. Its value is from `1` to `255`.
1. The first byte of a present pair is `first.Waveform`. Its value is from `1` to `255`.
1. `P0 = PackedPoleCounts >> 4` and `P1 = PackedPoleCounts & 0x0F`.
1. Coefficients of filter phase 0 occur in this order: direction `0` poles `0..P0-1`, then direction `1` poles `0..P1-1`.
1. Coefficients of filter phase 1 occur in the same order. A coefficient occurs only if mask bit `4d + p` has the value `1`.
1. `ModulationEnvelope` occurs if and only if `ModulationMask ≠ 0` or `UnityGain0 ≠ UnityGain1`.
1. A `PartialList` always ends with a terminator byte.
1. No byte follows `Loop`.

Note: The game client defines no shape for a waveform id from `5` to `255` and renders it as silence (Section 8.3). Game files store such ids, for example `245` as the first byte of a voice, so this specification keeps every id.

Note: In the game cache, a sound effect group can hold two files: this `.synth` file as file `0`, and a recorded sample in the Vorbis sample layout of the game client as file `1`. The game client plays only the last file of the group. A whole group therefore has bytes after `Loop` and is not a `.synth` file. Section 5.2 defines the sample file.

### 5.1 Tone and filter files

A `.tone` file holds one `Voice`, with the bytes that the voice has in a slot of a `.synth` file.
A `.filter` file holds one `OptFilter`, with the bytes that the filter field has in a voice.
A decoder reads the filter field of these files as Section 6.7 describes.
A decoder adds a warning for bytes after the data.
An encoder rejects a voice or a filter that breaks an invariant of Section 4.3, because the data must stay valid when it is put into a patch.

### 5.2 Sample files

A `.jagvorbis` file holds file `1` of a sound effect group: one mono Vorbis sample in a container of the game client.
The container is not an Ogg stream.
All integers are big-endian (Section 3.1).

```text
Sample        := s32 SetupLength, u8 Setup[SetupLength],
                 s32 SampleRate, s32 SampleCount, s32 LoopBegin, s32 StoredLoopEnd,
                 s32 PacketCount, Packet[PacketCount]
Packet        := Lacing, u8 Data[PacketLength]
Lacing        := u8 255 (repeated), u8 last      (last < 255)
```

| Field | Meaning |
| --- | --- |
| `Setup` | A Vorbis I setup header without the packet type byte, the `vorbis` signature, and the framing bit. Two 4-bit blocksize exponents, short then long, come first; the codebook, time, floor, residue, mapping, and mode configurations of Vorbis I follow. No identification header exists. |
| `SampleRate` | Samples per second. |
| `SampleCount` | Number of decoded samples that the game client keeps. |
| `LoopBegin` | First sample of the loop region. |
| `StoredLoopEnd` | `LoopEnd`, the sample after the loop region, when the loop plays forward. `~LoopEnd` (bitwise NOT), a negative value, when the loop is ping-pong. |
| `PacketLength` | The sum of the `Lacing` bytes. |
| `Data` | One Vorbis I audio packet for one channel. |

Decoding rules:

1. A negative `SetupLength`, `SampleCount`, or `PacketCount` is an error.
1. Input that ends inside a field, the setup, or a packet is an error.
1. Bytes after the last packet are an error. Thus a conforming file `B` satisfies `B = encode(decode(B))`.
1. `PingPong = StoredLoopEnd < 0`, and `LoopEnd = ~StoredLoopEnd` if `PingPong`, else `StoredLoopEnd`.
1. The encoder writes the fields in the order above and writes each packet length as `PacketLength ÷ 255` bytes `255` and then one byte `PacketLength - 255 × (PacketLength ÷ 255)`.

The game client decodes the packets as Vorbis I does, with these restrictions and rules:

1. Only floor type 1 occurs.
1. The first packet gives no samples, because it has no previous block to overlap.
1. Each operation of the decoder occurs in `f32` arithmetic.
1. A decoded value `x` gives the sample `clamp(toI32(fl32(32768 × x)), -32768, 32767)`.
1. The client keeps the first `SampleCount` samples. If the packets give fewer, the remaining samples are `0`.

The game client has no encoder for the sample. It only reads samples from the cache.

Loop playback: let `C ≥ 1` be the loop count of the sound, `R = C - 1` the number of repetitions, and `n = SampleCount`.
`e = clamp(LoopEnd, 0, n)` and `b = clamp(LoopBegin, 0, e)`.
If `R = 0` or `b = e`, the samples `[0..n)` play.
Otherwise:

1. Without `PingPong`: the samples `[0..e)`, then `R` times `[b..e)`, then `[e..n)`.
1. With `PingPong`: the samples `[0..e)`, then `R` passes over the loop region that alternate in direction, starting with a backward pass `e - 1` down to `b`.
   If `R` is even, the last pass is forward, and `[e..n)` follows.
   If `R` is odd, the last pass is backward, and it continues down to sample `0`. Playback then stops, and `[e..n)` does not play.

## 6. Decoding

### 6.1 Decoder state

The decoder has this state:

| Name | Type | Initial value | Meaning |
| --- | --- | --- | --- |
| `B` | byte string | the input | Input bytes. |
| `c` | integer | `0` | Cursor. The value can be larger than `\|B\|`. |
| `T` | boolean | false | Truncation flag. After the flag is true, it stays true. |
| `W` | list of strings | empty | Warnings. |

`R = |B| - c` is the number of remaining bytes. `R` can be negative.

### 6.2 Read primitives

| Primitive | Result |
| --- | --- |
| `peek(k)` | `B[c + k]` if `c + k < \|B\|`. Otherwise `0`. The primitive does not change `c` or `T`. |
| `skip(k)` | Sets `c ← c + k`. |
| `fixed(w)` | If `c + w > \|B\|`, sets `T ← true` and `c ← c + w`, and gives `0`. Otherwise, gives the integer in `B[c..c+w)` and sets `c ← c + w`. |
| `u8`, `u16`, `s32` | `fixed(1)`, `fixed(2)`, and `fixed(4)`, with the value rules of Section 3.1. |
| `usmart16`, `smart16` | If `c ≥ \|B\|`, gives `0` and changes nothing. Otherwise, the width is `1` if `B[c] < 128` and `2` if not. The read uses `fixed` with that width. If `fixed` is past the end of `B`, the result is `0`. Otherwise, the value rules of Section 3 apply. |

### 6.3 Errors

The decoder must stop and report an error in these conditions:

| Error | Condition |
| --- | --- |
| `E-LFS` | `B` starts with the ASCII text `version https://git-lfs.github.com/spec/`. |
| `E-PARTIALS` | A voice has more than five partials. |
| `E-POLES` | A filter has `P0 > 4` or `P1 > 4`. |
| `E-TRAILING` | Bytes follow a `Loop` that step 3 of Section 6.4 read after ten slots. |

An empty `B` is not an error. It decodes to ten empty slots and the loop `(0, 0)`.

### 6.4 Patch

```text
decode(B):
  1. If E-LFS applies, stop with E-LFS.
  2. For slot i from 0 to 9:
     a. If R < 1, slot i is empty. Continue with the next slot.
     b. Let m = peek(0).
     c. If m = 0, do skip(1). Slot i is empty.
     d. If R >= 30, slot i = decodeVoice().
     e. Otherwise, slots i to 9 are empty. Go to step 3.
  3. If R >= 4, LoopBegin = u16 and LoopEnd = u16.
     Otherwise, the loop is (0, 0), and if R > 0, T ← true.
  4. If step 2 read all ten slots, step 3 read the loop, and R > 0, stop with E-TRAILING.
  5. If T is true, add a warning with the text
     "File truncated at byte offset <c>" to W.
  6. The result is the patch, with W.
```

### 6.5 Voice

```text
decodeVoice():
  1.  f  = decodeEnvelope()
  2.  a  = decodeEnvelope()
  3.  pitchLfo     = decodePair()
  4.  amplitudeLfo = decodePair()
  5.  gate         = decodePair()
  6.  partials     = decodePartials()
  7.  EchoDelay    = usmart16
  8.  EchoFeedback = usmart16
  9.  Duration     = u16
  10. Offset       = u16
  11. filter       = decodeFilter()
```

### 6.6 Envelope, pair, and partials

```text
decodeEnvelope():
  1. w = u8
  2. StartValue = s32
  3. EndValue = s32
  4. n = u8
  5. Segments = n × (Duration = u16, TargetLevel = u16)

decodePair():
  1. If peek(0) = 0, do skip(1). The result is "absent".
  2. Otherwise, first = decodeEnvelope() and second = decodeEnvelope().

decodePartials():
  1. Let list be empty.
  2. Repeat:
     a. If R = 0, stop.
     b. amp = usmart16. If amp = 0, stop.
     c. If the list has 5 partials, stop with E-PARTIALS.
     d. PitchOffset = smart16
     e. Delay = usmart16
     f. Add (amp, PitchOffset, Delay) to the list.
  3. The result is the list.
```

### 6.7 Filter field

The byte after `Offset` is always the filter field, as in the game client:

```text
decodeFilter():
  1. If R = 0, the result is "absent".
  2. If peek(0) = 0, do skip(1). The result is "absent".
  3. Otherwise, the result is readFilter().
```

### 6.8 Filter body

```text
readFilter():
  1.  packed = u8. P0 = packed >> 4. P1 = packed & 0x0F.
  2.  If P0 > 4 or P1 > 4, stop with E-POLES.
  3.  UnityGain0 = u16. UnityGain1 = u16.
  4.  mask = u8
  5.  For d in 0..1, for p in 0..P(d)-1:
        Phase[d][0][p] = u16. Magnitude[d][0][p] = u16.
  6.  For d in 0..1, for p in 0..P(d)-1:
        If mask & 2^(4d + p) ≠ 0:
          Phase[d][1][p] = u16. Magnitude[d][1][p] = u16.
        Otherwise:
          Phase[d][1][p] = Phase[d][0][p].
          Magnitude[d][1][p] = Magnitude[d][0][p].
  7.  If mask ≠ 0 or UnityGain0 ≠ UnityGain1:
        n = u8. env = n × (Duration = u16, TargetLevel = u16).
      Otherwise, env is absent.
  8.  If T is true, add the warning
      "Filter truncated (discarding partial filter payload)" to W.
      The result is "absent".
  9.  If Mask(F) = 0 and UnityGain0 = UnityGain1, set env to absent.
  10. The result is the filter F with ModulationEnvelope = env.
```

Mask bits for poles that do not exist have no effect on step 6. Step 9 removes an envelope that has no effect on rendering. Steps 7 and 9 together make the decoded filter satisfy I4.

### 6.9 Postconditions

For each `B` that does not cause an error, `decode(B)` is a valid patch. The decoder never reads a byte at an index of `|B|` or more.

## 7. Encoding

### 7.1 Preconditions

The encoder must reject a patch that does not satisfy all invariants in Section 4.3. The error must name the invariant and the slot.

### 7.2 Procedure

```text
encode(P):
  1. For slot i from 0 to 9:
       If voices[i] is empty, write '0x00'. Otherwise, write encodeVoice(voices[i]).
  2. Write LoopBegin as u16. Write LoopEnd as u16.

encodeVoice(V):
  1. Write encodeEnvelope(FrequencyEnvelope) and encodeEnvelope(AmplitudeEnvelope).
  2. For each of PitchLfo, AmplitudeLfo, and Gate:
       If the pair is absent, write '0x00'.
       Otherwise, write encodeEnvelope(first) and encodeEnvelope(second).
  3. For each partial, write Amplitude as usmart16, PitchOffset as smart16,
     and Delay as usmart16.
  4. Write '0x00'.
  5. Write EchoDelay and EchoFeedback as usmart16.
  6. Write Duration and Offset as u16.
  7. If Filter is absent, write '0x00'. Otherwise, write encodeFilter(Filter).

encodeEnvelope(E):
  Write Waveform as u8, StartValue as s32, EndValue as s32,
  the segment count as u8, and each segment as Duration:u16 TargetLevel:u16.

encodeFilter(F):
  1. Write u8(PoleCount[0] × 16 + PoleCount[1]).
  2. Write UnityGain0 and UnityGain1 as u16.
  3. Let m = Mask(F). Write m as u8.
  4. For d in 0..1, for p in 0..PoleCount[d]-1:
       write Phase[d][0][p] and Magnitude[d][0][p] as u16.
  5. For d in 0..1, for p in 0..PoleCount[d]-1:
       if m & 2^(4d + p) ≠ 0, write Phase[d][1][p] and Magnitude[d][1][p] as u16.
  6. If ModulationEnvelope is present, write its segment count as u8.
     Then write each segment as Duration:u16 TargetLevel:u16.
```

The encoder has no limit on the output length.

### 7.3 Postconditions

For each valid patch `P`, these statements are true:

1. `decode(encode(P)) = P`.
1. `decode(encode(P))` gives no warnings.
1. `encode(P)` is a conforming file.

## 8. Voice synthesis

### 8.1 Global constants

| Name | Value |
| --- | --- |
| Sample rate | `22050` samples per second |
| Channels | `1` |
| `N(ms)` | `(ms × 22050) ÷ 1000`, in integer arithmetic |

Note: `N(ms)` gives the same result as `toI32(f64(ms) × 22.05)` for all `ms` from `0` to `131070`.

### 8.2 Tables

Each table has 32,768 entries. The index `i` is from `0` to `32767`.

```text
sine[i]  = toI32(sin(f64(i) / 5215.1903) × 16384)          (f64 arithmetic)

noise[i] = (next() & 2) - 1
  with the 48-bit linear congruential generator of java.util.Random:
    seed0  = 0x5DEECE66D
    seed'  = (seed × 0x5DEECE66D + 0xB) mod 2^48
    next() = wrap32(seed' >> 16)
  next() advances the seed once for each entry, in index order.

mult(k)  = pow(1.0057929410678534, f64(k))                 (f64 arithmetic)
```

The tables must have these SHA-256 values. Each value is a hash of the 32,768 entries, encoded as big-endian `s32`.

| Table | SHA-256 |
| --- | --- |
| `sine` | `235defb8b6d7c1db0137caec395de88f299393de4c5a3f95a2b5536c525c3513` |
| `noise` | `09b13db6e95d0057e3e6a6f342285ccd00d4b34cb08b7855b3ab02393e7f8f25` |

Note: `mult(120) = 2`. Thus `PitchOffset` has the unit 1/10 semitone.

### 8.3 Oscillator function

Let `A` be an `i32` amplitude, `ph` an `i32` phase, and `w` a waveform. Let `q = ph & 0x7FFF`.

| `w` | `Wave(A, ph, w)` |
| --- | --- |
| Off | `0` |
| Square | `A` if `q < 16384`. Otherwise `-A`. |
| Sine | `(sine[q] × A) >> 14` |
| Saw | `((q × A) >> 14) - A` |
| Noise | `noise[(ph ÷ 2607) & 0x7FFF] × A` |
| Undefined | `0` |

All operations in this table are `i32` operations.

### 8.4 Envelope generator

The generator uses the effective segment list `Q(E)` of envelope `E`:

1. If `E` is the `FrequencyEnvelope` or the `AmplitudeEnvelope` of voice `V`, `E.Segments` is empty, and `E.StartValue ≠ E.EndValue`, then `Q(E) = [(Duration = V.Duration, TargetLevel = E.EndValue)]`.
1. Otherwise, `Q(E) = E.Segments`.

In rule 1, `TargetLevel` is an `i32` value. Rule 1 uses the voice length in milliseconds as a duration in 1/65536 of the voice. The earlier editor has this behavior, and a conforming renderer must keep it.

An envelope generator `G` for envelope `E` has this initial state:

| State | Initial value |
| --- | --- |
| `threshold` | `1` |
| `pos` | `0` |
| `delta` | `0` |
| `amp` | `wrap32(E.StartValue × 2^15)` |
| `ticks` | `0` |

`G.eval(P)` uses the voice length `P` in samples. It gives one `i32` value for each call:

```text
eval(P):
  1. If Q(E) is empty, the result is E.StartValue.
  2. If ticks >= threshold, do advance(P).
  3. amp ← amp + delta
  4. ticks ← ticks + 1
  5. The result is (amp - delta) >> 15.

advance(P):
  1. amp ← wrap32(Q(E)[pos].TargetLevel × 2^15)
  2. pos ← min(pos + 1, |Q(E)| - 1)
  3. threshold ← floor(Q(E)[pos].Duration × P / 65536)      (exact integer arithmetic)
  4. If threshold > ticks:
       delta ← (wrap32(Q(E)[pos].TargetLevel × 2^15) - amp) ÷ (threshold - ticks)
     Otherwise:
       delta ← 0
```

Note: `threshold` is an absolute sample count. The generator does not use `Q(E)[0].Duration`. After the last segment, the value stays at the last `TargetLevel`.

### 8.5 Voice setup

Let `V` be a voice and `D = V.Duration`.

1. Let `L = N(D)`.
1. If `D < 10` or `L ≤ 0`, the voice gives zero samples. Stop.
1. Let `S = f64(L) / f64(D)`.
1. Let `F = V.FrequencyEnvelope`.
1. For each partial `k` in list order with `Amplitude ≠ 0`, compute these values:

```text
delay[k]        = toI32(f64(Delay) × S)
volume[k]       = (Amplitude × 2^14) ÷ 100
semitoneStep[k] = toI32(f64(wrap32(F.EndValue - F.StartValue)) × 32.768
                        × mult(PitchOffset) / S)
startStep[k]    = toI32(f64(F.StartValue) × 32.768 / S)
```

The `f64` operations in `semitoneStep` occur from left to right.

An LFO pair `(Rt, Dp)` is active if the pair is present. An active LFO has these values:

```text
lfoStep = toI32(f64(wrap32(Rt.EndValue - Rt.StartValue)) × 32.768 / S)
lfoBase = toI32(f64(Rt.StartValue) × 32.768 / S)
```

Each envelope has its own generator. All generators use `P = L`.

### 8.6 Gate mask

If `V.Gate` is absent, `mask[n] = 1` for all `n` from `0` to `L - 1`.

If `V.Gate` is present, let `g0 = Gate.first` and `g1 = Gate.second`. Set `muted = true` and `counter = 0`. For each `n` from `0` to `L - 1`, do these steps:

1. `v0 = G(g0).eval(L)`.
1. `v1 = G(g1).eval(L)`.
1. `T = g0.StartValue + (((g0.EndValue - g0.StartValue) × (muted ? v0 : v1)) >> 8)`.
1. `counter ← counter + 256`.
1. If `counter ≥ T`, set `counter ← 0` and `muted ← not muted`.
1. `mask[n] = 0` if `muted`. Otherwise `mask[n] = 1`.

Note: Both branches of step 3 use the `StartValue` and `EndValue` of `g0`.

### 8.7 Oscillator loop

Let `buf` be `L` samples of `i32` with the value `0`. Set each partial phase `ph[k] = 0` and each LFO phase to `0`. For each `n` from `0` to `L - 1`, do these steps:

1. `f = G(F).eval(L)`.
1. `a = G(V.AmplitudeEnvelope).eval(L)`.
1. If `PitchLfo` is active, do these steps:
   1. `r = G(Rt).eval(L)` and `m = G(Dp).eval(L)`.
   1. `f ← f + (Wave(m, lfoPhase, Rt.Waveform) >> 1)`.
   1. `lfoPhase ← lfoPhase + lfoBase + ((r × lfoStep) >> 16)`.
1. If `AmplitudeLfo` is active, do these steps:
   1. Compute `r`, `m`, and the next `lfoPhase` as in step 3 with the `AmplitudeLfo` values.
   1. Let `t = Wave(m, lfoPhase, Rt.Waveform) >> 1`, with `lfoPhase` before the update.
   1. `a ← (a × (t + 32768)) >> 15`.
1. For each partial `k` with `n + delay[k] < L`, do these steps:
   1. Let `j = n + delay[k]`.
   1. If `mask[j] ≠ 0`, set `buf[j] ← buf[j] + Wave((a × volume[k]) >> 15, ph[k], F.Waveform)`.
   1. `ph[k] ← ph[k] + ((f × semitoneStep[k]) >> 16) + startStep[k]`.

All operations in this section are `i32` operations.

### 8.8 Echo

If `EchoDelay > 0` and `EchoFeedback > 0`, let `e = toI32(f64(EchoDelay) × S)`. For each `n` from `e` to `L - 1`, in increasing order, set:

```text
buf[n] ← buf[n] + (buf[n - e] × EchoFeedback) ÷ 100
```

The update uses the values that earlier iterations wrote. Thus the echo is recursive.

### 8.9 Filter

The filter applies if `V.Filter` is present and `PoleCount[0] + PoleCount[1] ≥ 1`. Let `P0 = PoleCount[0]`, `P1 = PoleCount[1]`, `Kf = 2 × P0`, and `Kb = 2 × P1`. If `L < Kf + Kb`, the filter does not apply.

**Modulation value.** The filter uses a new generator for `ModulationEnvelope`. For sample `n`, `v_n` is the value of call number `n` (from `0`) of `eval(L)`. If the envelope is absent, `v_n = 65536`. Let `x_n = fl32(f32(v_n) / 65536)`.

**Interpolation.** `lerp(a, b, x) = fl32(f32(a) + x × (f32(b) - f32(a)))`.

**Pole values.** For direction `d`, pole `p`, and modulation value `x`:

```text
M     = lerp(Magnitude[d][0][p], Magnitude[d][1][p], x)
r     = fl32(1 - f32(exp(f64(-fl32(M × 0.0015258789)) / 20 × ln(10))))
Ψ     = lerp(Phase[d][0][p], Phase[d][1][p], x)
θ     = f32(pow(2, f64(fl32(Ψ × 1.2207031e-4))) × 32.703197 × 6.283185307179586 / 22050)
t1    = fl32(-2 × r × f32(cos(f64(θ))))
t2    = fl32(r × r)
```

The constants `0.0015258789` and `1.2207031e-4` are `f32` values. The other constants are `f64` values. `ln(10)` is the `f64` result of the natural logarithm of `10`.

**Gain.**

```text
G        = lerp(UnityGain0, UnityGain1, x)
invA0Q16 = toI32(fl32(f32(pow(0.1, f64(fl32(G × 0.0030517578)) / 20)) × 65536))
```

**Polynomial.** For direction `d` with `P = P(d) ≥ 1`, compute `f32` values `c[0..2P)`:

```text
c[0] = t1(pole 0)
c[1] = t2(pole 0)
for s from 1 to P - 1, with t1 and t2 of pole s:
  1. c[2s + 1] = c[2s - 1] × t2
  2. c[2s]     = c[2s - 1] × t1 + c[2s - 2] × t2
  3. for i from 2s - 1 down to 2:  c[i] = c[i] + (c[i - 1] × t1 + c[i - 2] × t2)
  4. c[1] = c[1] + (c[0] × t1 + t2)
  5. c[0] = c[0] + t1
```

All operations in the polynomial are `f32` operations.

**Fixed-point coefficients.**

```text
ff[k] = toI32(fl32(c_0[k] × fl32(f32(invA0Q16) / 65536) × 65536))   for k in 0..Kf-1
fb[k] = toI32(fl32(c_1[k] × 65536))                                  for k in 0..Kb-1
```

`c_0` is the polynomial of direction `0`. `c_1` is the polynomial of direction `1`. In `ff[k]`, the first multiplication occurs first.

**Difference equation.** Let `x` be a copy of `buf` before the filter. For each `n` from `0` to `L - 1`, compute the coefficients from `x_n`. Then compute an `i64` sum `y`:

```text
if n < L - Kf:
  y = ((x[n + Kf] × invA0Q16) >> 16)
      + sum for k in 0..Kf-1 of ((x[n + Kf - 1 - k] × ff[k]) >> 16)
otherwise:
  y = sum for k in (n + Kf - L)..Kf-1 of ((x[n + Kf - 1 - k] × ff[k]) >> 16)

y = y - sum for k in 0..min(n, Kb)-1 of ((buf[n - 1 - k] × fb[k]) >> 16)
buf[n] ← wrap32(y)
```

Each product and each shift in the difference equation is an `i64` operation.

### 8.10 Voice result

The voice result is `buf` after these steps, in this order:

1. The oscillator loop (Section 8.7).
1. The echo (Section 8.8).
1. The filter (Section 8.9).
1. A clamp of each sample to the range `-32768` to `32767`.

## 9. Patch rendering

### 9.1 Parameters

| Parameter | Domain | Default |
| --- | --- | --- |
| `C` | loop count, integer `≥ 0` | `1` |
| `sel` | one slot index, or "all slots" | all slots |

### 9.2 Procedure

1. Let `U` be the set of slots that `sel` includes and that contain a voice.
1. Let `maxDur = max(V.Duration + V.Offset)` for `V` in `U`. Voices with `Duration < 10` count.
1. If `U` is empty or `maxDur = 0`, the result is an empty sequence. Stop.
1. Let `Lp = N(maxDur)`.
1. Let `ls = N(LoopBegin)` and `le = N(LoopEnd)`.
1. If `le > Lp` or `ls ≥ le`, let `C' = 0`. Otherwise, let `C' = C`.
1. Let `out` be `Lp` samples of `i32` with the value `0`.
1. For each voice `V` in `U`, in slot order:
   1. Let `o = N(V.Offset)` and `v` be the voice result.
   1. For each `i` from `0` to `|v| - 1` with `o + i < Lp`, set `out[o + i] ← out[o + i] + v[i]`.
1. If `C' > 1`, set `out ← out[0..le) ++ (out[ls..le) repeated C' - 1 times) ++ out[le..Lp)`.
1. Clamp each sample of `out` to the range `-32768` to `32767`.
1. The result is `out` as signed 16-bit samples.

The result length is `Lp + (le - ls) × max(0, C' - 1)`.

## 10. Output encodings

### 10.1 PCM

| Encoding | Bytes for one sample `s` |
| --- | --- |
| PCM16 | `s` as a signed 16-bit little-endian integer. |
| PCM8 | `(s >> 8) + 128` as one unsigned byte. |

### 10.2 WAV container

A WAV file has a 44-byte header and then the PCM data. All header integers are little-endian.

| Offset | Size | Value |
| --- | --- | --- |
| 0 | 4 | ASCII `RIFF` |
| 4 | 4 | `36 + data length` |
| 8 | 4 | ASCII `WAVE` |
| 12 | 4 | ASCII `fmt` |
| 16 | 4 | `16` |
| 20 | 2 | `1` (PCM) |
| 22 | 2 | `1` (mono) |
| 24 | 4 | `22050` |
| 28 | 4 | `22050 × bytes per sample` |
| 32 | 2 | bytes per sample |
| 34 | 2 | `8` or `16` |
| 36 | 4 | ASCII `data` |
| 40 | 4 | data length in bytes |

## 11. Constants

| Name | Value | Section |
| --- | --- | --- |
| Voice slots | `10` | 4.1 |
| Maximum partials in a voice | `5` | 4.1 |
| Maximum segments in an envelope | `255` | 4.1 |
| Maximum poles in a direction | `4` | 4.1 |
| Minimum remaining bytes to decode a voice | `30` | 6.4 |
| Sample rate | `22050` | 8.1 |
| Minimum rendered voice duration | `10` ms | 8.5 |
| Table size | `32768` | 8.2 |
| Phase mask | `0x7FFF` | 8.3 |
| Phase scale | `32.768` | 8.5 |
| Noise phase divisor | `2607` | 8.3 |
| Gate counter step | `256` | 8.6 |

## 12. Known deviations of the earlier editor (informative)

The earlier C# editor differs from this specification in the items below. Items a to f have a reproduction. Item c occurs in five files in `references/synths/`. Items a, b, d, e, and f occur in no reference file.

| Item | Behavior of the earlier editor | Result |
| --- | --- | --- |
| a | The reader accepts up to ten partials, and the writer always writes a partial terminator. A voice with ten partials gets one byte too many. | The next read moves each later field by one byte. This specification allows five partials, as the game client does, and reports `E-PARTIALS` for more. |
| b | The `smart16` writer accepts values below `-16384`. | `-20000` encodes as `0x71E0`. The decoder reads `49` and one byte. |
| c | If no phase value changes, the mask is `0`. The writer then writes all phase-1 coefficients. The reader reads none of them. | A test filter grew from `486` bytes to `487` bytes after a second write. Five reference files grow by 4 to 20 bytes after one write: `noa_screech_melee_3`, `relic_unlock_pulsing`, `toa_akkha_footstep_right_01`, `toa_zebak_attack_melee_enranged_jaw_snap_01`, and `toa_zebak_ranged_enranged_gulp_02`. |
| d | The writer accepts a pair with `first.Waveform = Off`. It writes byte `0` as the first byte, so the reader sees "absent". | The pitch LFO disappeared, and the next fields moved. |
| e | The writer computes the mask from phase values only. | A change to a phase-1 magnitude alone disappeared after a write and a read. |
| f | The reader decodes an empty slot only if 30 or more bytes remain. Trailing empty slots then leave their `0x00` bytes unread. | A write and a read of `cow_death.synth` with the loop `(100, 200)` gave the loop `(0, 0)`. |
| g | The reader maps waveform ids larger than `4` to `Off`, and it reads a slot only if its first byte is from `1` to `5`. | Game files use waveform ids `5` and `245`. A file whose first voice starts with `245` lost all its voices. This specification keeps every id. |
| m | The reader adds a segment to a frequency or amplitude envelope that has no segments and different start and end values. The writer then writes that segment. | Such a file grows by 4 bytes for each envelope after one write. This specification adds the segment only in the renderer (Section 8.4). |
| h | The reader accepts pole counts from `5` to `15`. It reads up to 15 coefficients for each direction, but it stores them with a stride of four. | Coefficients of different poles overwrite each other. This specification reports `E-POLES`. |
| i | If fewer than two bytes remain before a filter phase or magnitude value, the reader stops the coefficient loop without the truncation flag. | The reader can keep a filter with zero coefficients. This specification discards the filter. |
| j | The writer uses a fixed 4096-byte buffer. | Larger patches fail. This specification has no limit. |
| k | The previous version of this document gave wrong values. | The `PitchOffset` unit is 1/10 semitone, not decicents. The `smart16` lower limit is `-16384`. The sine table truncates. "Maximum input file size" was the size of an audio buffer pool. |
| l | `BitsPerSample = 8` and `FilterUpdateRate = 256` exist in the code, but no code uses them. | No effect. |
| m | The reader reads a nonzero byte after `Offset` as the next voice when the bytes after it look like an envelope. | 177 of the 11,203 game files decode with wrong voices. This specification reads the filter field as the game client does, and all 11,203 files decode and encode to the same bytes. |

These behaviors of the earlier editor are part of this specification. Implementations must keep them:

1. Rule 1 of Section 8.4 uses the voice length in milliseconds as a `Segment.Duration`.
1. The gate of Section 8.6 uses `g0` values in both branches.
1. The envelope generator of Section 8.4 does not use `Q(E)[0].Duration`.

## 13. Conformance risks (informative)

1. Sections 8.2 and 8.9 use `sin`, `cos`, `exp`, and `pow`. IEEE 754 does not require exact rounding for these functions. Two platforms can give results that differ by one unit in the last place. A difference of this size can change one fixed-point coefficient.
1. The golden WAV files come from the renderer of the earlier editor on .NET 8. A Kotlin/JVM, Kotlin/Native, or Kotlin/Wasm renderer must agree with these files. The test suite must check each target.
1. The SHA-256 values in Section 8.2 detect a table difference. They do not detect a difference in the filter calculation.
1. `toI32` saturates. .NET 8 on x64 gives `-2147483648` for an `f32` or `f64` value outside the `i32` range. For such values, the output of the earlier editor on x64 can differ from this specification.
