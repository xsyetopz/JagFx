# JagFx `.synth` binary format

JagFx reads and writes the Old School RuneScape sound-effect synthesizer format. Files are byte streams. Multi-byte integers are big-endian. Rendered audio is mono at 22,050 Hz.

This document specifies the binary layout and the meaning of each encoded value as implemented by `SynthFileReader`, `SynthFileWriter`, and the JagFx synthesis engine.

## 1. Notation

| Term | Meaning |
| --- | --- |
| `u8` | Unsigned 8-bit integer. |
| `u16` | Unsigned 16-bit integer, big-endian. |
| `s32` | Signed 32-bit integer, big-endian. |
| `usmart16` | Unsigned variable-length integer. See §2.1. |
| `smart16` | Signed variable-length integer. See §2.2. |
| `[]` | Repeated field. |
| `?` | Optional field. |
| `N` | Count read from the stream. |

Numeric ranges are inclusive.

## 2. Scalar encodings

### 2.1 `usmart16`

`usmart16` stores values from `0` to `32767` in one or two bytes.

$$
\operatorname{DecodeUSmart16}(b, w) =
\begin{cases}
b & b < 128 \\
w - 32768 & b \ge 128
\end{cases}
$$

When `b < 128`, the decoder consumes one byte. When `b >= 128`, the decoder reads a big-endian `u16` named `w` starting at the same byte and consumes two bytes.

$$
\operatorname{EncodeUSmart16}(v) =
\begin{cases}
\text{u8}(v) & 0 \le v < 128 \\
\text{u16}(v + 32768) & 128 \le v \le 32767
\end{cases}
$$

| Form | First byte | Decoded range |
| --- | --- | --- |
| One byte | `0x00`-`0x7f` | `0`-`127` |
| Two bytes | `0x80`-`0xff` | `0`-`32767` |

### 2.2 `smart16`

`smart16` stores values from `-32768` to `16383` in one or two bytes.

$$
\operatorname{DecodeSmart16}(b, w) =
\begin{cases}
b - 64 & b < 128 \\
w - 49152 & b \ge 128
\end{cases}
$$

When `b < 128`, the decoder consumes one byte. When `b >= 128`, the decoder reads a big-endian `u16` named `w` starting at the same byte and consumes two bytes.

$$
\operatorname{EncodeSmart16}(v) =
\begin{cases}
\text{u8}(v + 64) & -64 \le v < 64 \\
\text{u16}(v + 49152) & -32768 \le v < -64 \text{ or } 64 \le v \le 16383
\end{cases}
$$

| Form | First byte | Decoded range |
| --- | --- | --- |
| One byte | `0x00`-`0x7f` | `-64`-`63` |
| Two bytes | `0x80`-`0xff` | `-32768`-`16383` |

### 2.3 Fixed-point constants

JagFx uses these fixed-point constants while interpreting encoded fields:

| Constant | Value | Meaning |
| --- | --- | --- |
| `Q16` | `65536` | `1 << 16` |
| `Q15` | `32768` | `1 << 15` |
| `Q14` | `16384` | `1 << 14` |

## 3. Top-level file layout

A `.synth` file contains ten voice slots followed by a loop footer.

```text
SynthFile = VoiceSlot[10] LoopFooter
LoopFooter = LoopBegin:u16 LoopEnd:u16
```

The slot bodies are variable length. Empty slots are one byte. Non-empty slots start with a waveform marker that also begins the first envelope.

### 3.1 Voice slot dispatch

For slot index `i` from `0` through `9`, read the next byte without advancing the stream.

| Marker | Action |
| --- | --- |
| `0x00` | Empty slot. Consume one byte and continue with slot `i + 1`. |
| `0x01`-`0x04` | Voice slot. Decode `VoiceBody`; the marker is consumed as the first byte of `FrequencyEnvelope.Waveform`. |
| Any other value | Invalid voice marker. Set slot `i` and every later slot to empty. Do not consume the marker as a voice. |

The reader requires at least 30 remaining bytes before it decodes a voice. If fewer than 30 bytes remain, the reader sets the current and later slots to empty.

The writer always emits exactly ten slots. A null slot is encoded as `0x00`.

### 3.2 Loop footer

The loop footer is four bytes after the voice slots.

| Field | Type | Unit | Meaning |
| --- | --- | --- | --- |
| `LoopBegin` | `u16` | milliseconds | Start of loop region. |
| `LoopEnd` | `u16` | milliseconds | End of loop region. |

A renderer may repeat the loop region when `LoopBegin < LoopEnd`. JagFx disables loop expansion when the converted sample range is outside the rendered buffer.

## 4. Voice body

```text
VoiceBody =
  FrequencyEnvelope:Envelope
  AmplitudeEnvelope:Envelope
  PitchLfo:OptionalEnvelopePair
  AmplitudeLfo:OptionalEnvelopePair
  GatePair:OptionalEnvelopePair
  Partials:PartialList
  EchoDelay:usmart16
  EchoFeedback:usmart16
  Duration:u16
  Offset:u16
  Filter:OptionalFilter
```

| Field | Type | Unit | Meaning |
| --- | --- | --- | --- |
| `FrequencyEnvelope` | `Envelope` | raw | Pitch contour and waveform selector. |
| `AmplitudeEnvelope` | `Envelope` | raw | Amplitude contour. |
| `PitchLfo` | `OptionalEnvelopePair` | raw | Vibrato rate and depth. |
| `AmplitudeLfo` | `OptionalEnvelopePair` | raw | Tremolo rate and depth. |
| `GatePair` | `OptionalEnvelopePair` | raw | Muted and audible gate spans. |
| `Partials` | `PartialList` | mixed | Additive oscillator sources. |
| `EchoDelay` | `usmart16` | milliseconds | Echo tap delay. |
| `EchoFeedback` | `usmart16` | percent-like raw value | Echo feedback multiplier uses `EchoFeedback / 100`. |
| `Duration` | `u16` | milliseconds | Voice render length. |
| `Offset` | `u16` | milliseconds | Voice start offset in patch mix. |
| `Filter` | `OptionalFilter` | raw | Time-varying IIR filter. |

## 5. Envelopes

### 5.1 Full envelope

```text
Envelope =
  Waveform:u8
  StartValue:s32
  EndValue:s32
  SegmentCount:u8
  Segment[SegmentCount]

Segment = Duration:u16 TargetLevel:u16
```

| Field | Type | Meaning |
| --- | --- | --- |
| `Waveform` | `u8` | Waveform id. See §5.2. |
| `StartValue` | `s32` | Initial generator value. |
| `EndValue` | `s32` | Final/range value used by synthesis setup. |
| `SegmentCount` | `u8` | Number of segment pairs that follow. |
| `Segment.Duration` | `u16` | Position in units of `1 / 65536` of the voice duration. |
| `Segment.TargetLevel` | `u16` | Raw target value for the envelope generator. |

Envelope segment timing uses the rendered voice sample count `P`:

$$
\operatorname{SegmentSample}(d, P) = \left\lfloor \frac{d \cdot P}{65536} \right\rfloor
$$

`TargetLevel` is not remapped through `StartValue` and `EndValue` by the JagFx reader. The envelope generator uses `TargetLevel` as the next raw amplitude value.

For the two required voice envelopes, the reader creates one implicit segment when `SegmentCount = 0` and `StartValue \ne EndValue`:

$$
\operatorname{ImplicitSegment}(E, D) = (\text{Duration}=D,\ \text{TargetLevel}=E.\text{EndValue})
$$

where `D` is the voice duration in milliseconds. The writer emits the domain envelope as stored; it does not recreate the original zero-segment encoding after the reader adds the implicit segment.

### 5.2 Waveform ids

| Id | Name | Generator |
| --- | --- | --- |
| `0` | Off | Silence. |
| `1` | Square | Band-unlimited square wave. |
| `2` | Sine | Lookup-table sine. |
| `3` | Saw | Band-unlimited saw. |
| `4` | Noise | Java-compatible pseudo-random noise table. |

Voice slot markers accept only `1` through `4`. Full envelopes outside the slot marker position may encode `0` through `4`; JagFx maps other values to `Off` on read.

### 5.3 Optional envelope pair

```text
OptionalEnvelopePair =
  Absent:u8(0)
  | First:Envelope Second:Envelope
```

The reader peeks the next byte. If the byte is zero, the reader consumes one byte and returns no pair. If the byte is non-zero, the reader decodes two full envelopes without a separate presence marker.

| Use | First envelope | Second envelope |
| --- | --- | --- |
| Pitch LFO | Rate envelope | Modulation-depth envelope |
| Amplitude LFO | Rate envelope | Modulation-depth envelope |
| Gate pair | Muted-span envelope | Audible-span envelope |

## 6. Partials

```text
PartialList = Partial* Terminator
Partial = Amplitude:usmart16 PitchOffset:smart16 Delay:usmart16
Terminator = Amplitude:usmart16(0)
```

The reader stops after ten partials or when it reads `Amplitude = 0`, whichever comes first.

| Field | Type | Unit | Meaning |
| --- | --- | --- | --- |
| `Amplitude` | `usmart16` | raw | Partial gain. Zero terminates the list. |
| `PitchOffset` | `smart16` | decicents | Pitch offset for this partial. |
| `Delay` | `usmart16` | milliseconds | Start delay inside the voice buffer. |

JagFx scales partial amplitude during synthesis as:

$$
\operatorname{PartialVolume}(a) = \left\lfloor \frac{a \cdot 2^{14}}{100} \right\rfloor
$$

The binary range permits `1` through `32767` for stored partial amplitudes. The value `0` is reserved for the terminator.

The writer emits all partials in the domain list and then writes the zero terminator.

## 7. Echo

Echo has no explicit presence byte. Echo is active only when both fields are non-zero.

| Field | Type | Unit | Meaning |
| --- | --- | --- | --- |
| `EchoDelay` | `usmart16` | milliseconds | Feedback delay. |
| `EchoFeedback` | `usmart16` | raw percent divisor | Feedback amount. |

For a voice buffer of length `N` and samples-per-millisecond value `S`, JagFx applies echo in place:

$$
D = \left\lfloor \text{EchoDelay} \cdot S \right\rfloor
$$

$$
\forall n \in [D, N):\quad x_n \leftarrow x_n + \left\lfloor \frac{x_{n-D} \cdot \text{EchoFeedback}}{100} \right\rfloor
$$

## 8. Filter

A filter immediately follows the voice `Offset` field.

```text
OptionalFilter = NoFilter | Filter
NoFilter = u8(0)
Filter = FilterHeader FilterCoefficientPayload FilterModulationEnvelope?
```

### 8.1 Filter detection

The filter has no unambiguous magic byte. A non-zero filter header can collide with the next voice slot marker. JagFx detects filters with this rule:

1. If no bytes remain, the filter is absent.
2. Let `b = peek(0)`.
3. If `b = 0`, consume `b`; the filter is absent.
4. If `b \notin [1, 4]`, decode a filter.
5. If `b \in [1, 4]`, test whether the bytes look like a following envelope:

$$
\text{possibleStart} = (\operatorname{peek}(1) \ll 24) \;|\; (\operatorname{peek}(2) \ll 16) \;|\; (\operatorname{peek}(3) \ll 8) \;|\; \operatorname{peek}(4)
$$

$$
\operatorname{LooksLikeEnvelope} = |\text{possibleStart}| \le 10{,}000{,}000 \land \operatorname{peek}(9) \le 15
$$

If `LooksLikeEnvelope` is true, the filter is absent and the next slot parser sees `b` as a voice marker. Otherwise, decode a filter.

### 8.2 Filter header

```text
FilterHeader =
  PackedPoleCounts:u8
  UnityGain0:u16
  UnityGain1:u16
  ModulationMask:u8
```

| Field | Type | Meaning |
| --- | --- | --- |
| `PackedPoleCounts` | `u8` | High nibble is direction 0 pole count; low nibble is direction 1 pole count. |
| `UnityGain0` | `u16` | Unity-gain start value. |
| `UnityGain1` | `u16` | Unity-gain end value. |
| `ModulationMask` | `u8` | Phase-1 coefficient presence bits. |

Pole counts decode as:

$$
P_0 = \text{PackedPoleCounts} \gg 4
$$

$$
P_1 = \text{PackedPoleCounts} \;\&\; 0x0f
$$

Mask bit `d * 4 + p` controls direction `d`, pole `p`, phase `1`.

| Bit | Coefficient |
| --- | --- |
| `0` | Direction 0, pole 0 |
| `1` | Direction 0, pole 1 |
| `2` | Direction 0, pole 2 |
| `3` | Direction 0, pole 3 |
| `4` | Direction 1, pole 0 |
| `5` | Direction 1, pole 1 |
| `6` | Direction 1, pole 2 |
| `7` | Direction 1, pole 3 |

The binary header can encode `0` through `15` poles per direction. The OSRS/JagFx runtime coefficient path is designed for up to four pole pairs per direction (`8` coefficients per direction). Reference files use that range.

### 8.3 Filter coefficients

Each stored coefficient pair is:

```text
FilterCoefficient = Phase:u16 Magnitude:u16
```

The reader iterates in this order:

```text
for phase in [0, 1]
  for direction in [0, 1]
    for pole in [0, poleCount[direction])
```

For phase `0`, every pole stores a pair. For phase `1`, a pair is stored only when the matching modulation-mask bit is set. If the bit is clear, phase `1` copies phase `0`.

$$
C_{d,1,p} =
\begin{cases}
\operatorname{readPair}() & \text{if } \text{ModulationMask} \;\&\; (1 \ll (4d+p)) \ne 0 \\
C_{d,0,p} & \text{otherwise}
\end{cases}
$$

The writer calculates the modulation mask by comparing phase values only:

$$
\text{bit}(d,p) = [\text{Phase}_{d,0,p} \ne \text{Phase}_{d,1,p}]
$$

The writer then emits phase-1 coefficient pairs only for set bits.

### 8.4 Filter modulation envelope

A filter modulation envelope is present when:

$$
\text{ModulationMask} \ne 0 \quad \lor \quad \text{UnityGain0} \ne \text{UnityGain1}
$$

The encoded structure contains segment data only:

```text
FilterModulationEnvelope = SegmentCount:u8 Segment[SegmentCount]
```

JagFx maps the decoded value to an envelope with:

```text
Waveform = 0
StartValue = 0
EndValue = 0
Segments = decoded segment list
```

If the reader overruns the buffer while reading a filter, the decoded voice keeps no filter and records a warning.

## 9. Reader behavior for truncated input

JagFx does not throw for most mid-structure truncation cases. `BinaryBuffer` records truncation and returns zeroes.

| Operation | Out-of-range behavior |
| --- | --- |
| Fixed-size read | Sets truncation flag, advances by requested width, returns `0`. |
| `peek(offset)` | Returns `0`; does not set truncation flag. |
| `usmart16` / `smart16` read | Determines width from first byte, then applies fixed-size truncation behavior. |
| Filter read | If truncation was observed by the end of filter parsing, discard the whole filter. |
| Loop footer | If fewer than four bytes remain after voices, loop values are `0, 0`. |

JagFx rejects Git LFS pointer text before parsing binary data.

## 10. Canonical writer behavior

`SynthFileWriter` writes a canonical stream for a JagFx domain patch:

1. Emit ten voice slots.
2. Emit `0x00` for null voice slots.
3. Emit full envelope records as stored in the domain model.
4. Emit `0x00` for absent optional envelope pairs.
5. Emit partials followed by `usmart16(0)`.
6. Emit `0x00` for absent filters.
7. Emit a filter modulation envelope only when the filter has one.
8. Emit `LoopBegin` and `LoopEnd` as `u16`.

Some valid input byte streams are not byte-stable after read/write because the reader normalizes ambiguous or missing data. Tests compare JSON round-trips against `SynthFileWriter.Write(SynthFileReader.Read(bytes))` for files with known asymmetries.

## 11. Synthesis semantics

The sections below define how JagFx interprets the encoded values during rendering. These formulas are not extra bytes in the file.

### 11.1 Waveform tables

JagFx precomputes tables with 32,768 entries unless stated otherwise.

| Table | Formula |
| --- | --- |
| Sine | $\operatorname{sine}[i] = \left\lfloor \sin(i / 5215.1903) \cdot 16384 \right\rfloor$ |
| Noise | Java RNG seed `0`; $\operatorname{noise}[i] = (\operatorname{nextInt}() \& 2) - 1$ |
| Pitch multiplier cache | $1.0057929410678534^{d}$ for decicents `d` from `-120` through `120` |
| Unit circle | $x_i = \cos(2\pi i / 64)$, $y_i = \sin(2\pi i / 64)$ for `0 <= i <= 64` |

### 11.2 Waveform sample function

Let `A` be amplitude, `φ` be a signed 32-bit phase accumulator, and `p = φ & 0x7fff`.

$$
\operatorname{Wave}(A, \varphi, w) =
\begin{cases}
0 & w = 0 \\
A & w = 1 \land p < 16384 \\
-A & w = 1 \land p \ge 16384 \\
\left\lfloor \frac{\operatorname{sine}[p] \cdot A}{2^{14}} \right\rfloor & w = 2 \\
\left\lfloor \frac{p \cdot A}{2^{14}} \right\rfloor - A & w = 3 \\
\operatorname{noise}\left[\left\lfloor \frac{\varphi}{2607} \right\rfloor \& 0x7fff\right] \cdot A & w = 4
\end{cases}
$$

Phase accumulators wrap through signed 32-bit integer overflow.

### 11.3 Envelope generator

Initial generator state for envelope `E`:

| State | Initial value |
| --- | --- |
| `amplitude` | `E.StartValue << 15` |
| `delta` | `0` |
| `position` | `0` |
| `threshold` | `1` |
| `ticks` | `0` |

If an envelope has no segments, evaluation returns `StartValue`.

For a segmented envelope, `Evaluate(P)` advances the segment when `ticks >= threshold`, then increments `amplitude` by `delta`, increments `ticks`, and returns:

$$
\left\lfloor \frac{\text{amplitude} - \text{delta}}{2^{15}} \right\rfloor
$$

Segment advance uses:

$$
\text{amplitude} \leftarrow \text{Segment}_{\text{position}}.\text{TargetLevel} \cdot 2^{15}
$$

$$
\text{position} \leftarrow \min(\text{position} + 1, |\text{Segments}| - 1)
$$

$$
\text{threshold} \leftarrow \left\lfloor \frac{\text{Segment}_{\text{position}}.\text{Duration} \cdot P}{65536} \right\rfloor
$$

$$
\text{delta} \leftarrow
\begin{cases}
\left\lfloor \frac{\text{Segment}_{\text{position}}.\text{TargetLevel} \cdot 2^{15} - \text{amplitude}}{\text{threshold} - \text{ticks}} \right\rfloor & \text{threshold} > \text{ticks} \\
0 & \text{otherwise}
\end{cases}
$$

### 11.4 Voice rendering

For voice duration `D` milliseconds:

$$
N = \left\lfloor \frac{D \cdot 22050}{1000} \right\rfloor
$$

JagFx renders no samples when `D < 10` or `N <= 0`.

$$
S = \frac{N}{D}
$$

For each active partial `p`:

$$
\operatorname{delay}_p = \left\lfloor \text{Delay}_p \cdot S \right\rfloor
$$

$$
\operatorname{volume}_p = \left\lfloor \frac{\text{Amplitude}_p \cdot 2^{14}}{100} \right\rfloor
$$

$$
\operatorname{semitoneStep}_p = \left\lfloor \frac{(F_{end} - F_{start}) \cdot 32.768 \cdot 1.0057929410678534^{\text{PitchOffset}_p}}{S} \right\rfloor
$$

$$
\operatorname{startStep} = \left\lfloor \frac{F_{start} \cdot 32.768}{S} \right\rfloor
$$

For each sample `n`, JagFx evaluates frequency and amplitude envelopes, applies pitch and amplitude LFOs when present, and adds each partial at `n + delay_p` when the target index is inside the voice buffer.

### 11.5 Gate envelopes

Gate rendering uses the two gate envelopes when both are present. The muted flag starts as true. A counter starts at zero and increases by `256` per sample.

Let `g0` be the first gate envelope and `g1` be the second. Let `v0` and `v1` be their evaluated values for the current sample.

$$
T =
\begin{cases}
g0.StartValue + ((g0.EndValue - g0.StartValue) \cdot v0 \gg 8) & \text{muted} \\
g0.StartValue + ((g0.EndValue - g0.StartValue) \cdot v1 \gg 8) & \text{not muted}
\end{cases}
$$

When the counter reaches `T`, JagFx resets the counter to zero and toggles `muted`. Muted samples are set to zero.

### 11.6 Filter coefficient math

Let `f` be the filter modulation factor. If no modulation envelope exists, `f = 1`. Otherwise:

$$
f = \frac{\operatorname{FilterEnvelope.Evaluate}(N)}{65536}
$$

For direction `d` and pole `p`:

$$
M = M_{d,0,p} + f(M_{d,1,p} - M_{d,0,p})
$$

$$
r = 1 - 10^{-M \cdot 0.0015258789 / 20}
$$

$$
\Psi = \Psi_{d,0,p} + f(\Psi_{d,1,p} - \Psi_{d,0,p})
$$

$$
\theta = \frac{2\pi \cdot 32.703197 \cdot 2^{\Psi \cdot 1.2207031 \times 10^{-4}}}{22050}
$$

Unity gain interpolation:

$$
G = G_0 + f(G_1 - G_0)
$$

$$
a_0^{-1} = 10^{-G \cdot 0.0030517578 / 20}
$$

$$
\operatorname{inverseA0Q16} = \left\lfloor a_0^{-1} \cdot 65536 \right\rfloor
$$

The first second-order section for a pole uses:

$$
\operatorname{sos}_0 = -2r\cos(\theta)
$$

$$
\operatorname{sos}_1 = r^2
$$

For later poles, the SOS polynomial is multiplied by each new section. Feedforward coefficients are scaled by `a_0^{-1}` before Q16 conversion. Feedback coefficients are converted directly to Q16.

## 12. Constants

| Name | Value |
| --- | --- |
| Sample rate | `22050` |
| Channels | `1` |
| Bits per sample | `8` |
| Voice slots | `10` |
| Minimum rendered voice duration | `10 ms` |
| Maximum partials per voice | `10` |
| Maximum encoded filter poles per direction | `15` |
| Runtime filter coefficients per direction | `8` |
| Filter coefficient chunk size | `128` |
| Phase mask | `0x7fff` |
| Phase scale | `32.768` |
| Noise phase divisor | `2607` |
| Maximum input file size | `1048576` bytes |
| Envelope/filter disambiguation start threshold | `10000000` |
| Envelope/filter disambiguation segment-count threshold | `15` |
| Minimum voice bytes before decode | `30` |
