# Jagex Synthesizer File Format and Engine Specification

- **Version**: 2.0
- **Origin**: OldSchool RuneScape client audio subsystem
- **Sample Rate**: 22,050 Hz (fixed)
- **Channels**: 1 (mono)
- **Byte Order**: Big-endian throughout

---

## 1. Notation and Data Types

### 1.1 Primitive Types

| Type  | Size    | Description                                            |
| ----- | ------- | ------------------------------------------------------ |
| `u8`  | 1 byte  | Unsigned 8-bit integer, range [0, 255]                 |
| `u16` | 2 bytes | Unsigned 16-bit integer, big-endian, range [0, 65535]  |
| `s32` | 4 bytes | Signed 32-bit integer, big-endian, range [-2³¹, 2³¹-1] |

### 1.2 Variable-Length Types

#### 1.2.1 USmart16 (Unsigned Smart Integer)

A variable-length unsigned integer encoding values in the range [0, 32767].

**Decoding algorithm:**

> **procedure** DecodeUSmart16(*stream*) \
> &emsp; *b* ← peek first byte of *stream* \
> &emsp; **if** *b* < 128 **then** \
> &emsp;&emsp; consume 1 byte from *stream* \
> &emsp;&emsp; **return** *b* \
> &emsp; **else** \
> &emsp;&emsp; consume 2 bytes from *stream*; interpret as *w* (u16, big-endian) \
> &emsp;&emsp; **return** *w* − 32768

**Encoding algorithm:**

> **procedure** EncodeUSmart16(*v*) \
> &emsp; **if** *v* < 128 **then** \
> &emsp;&emsp; emit 1 byte: *v* \
> &emsp; **else** \
> &emsp;&emsp; emit 2 bytes: (*v* + 32768) as u16 big-endian

| Byte Form | Byte 0 Range | Value Range |
| --------- | ------------ | ----------- |
| 1-byte    | 0x00–0x7F    | 0–127       |
| 2-byte    | 0x80–0xFF    | 0–32767     |

#### 1.2.2 Smart16 (Signed Smart Integer)

A variable-length signed integer encoding values in the range [-32768, 16383].

**Decoding algorithm:**

> **procedure** DecodeSmart16(*stream*) \
> &emsp; *b* ← peek first byte of *stream* \
> &emsp; **if** *b* < 128 **then** \
> &emsp;&emsp; consume 1 byte from *stream* \
> &emsp;&emsp; **return** *b* − 64 \
> &emsp; **else** \
> &emsp;&emsp; consume 2 bytes from *stream*; interpret as *w* (u16, big-endian) \
> &emsp;&emsp; **return** *w* − 49152

**Encoding algorithm:**

> **procedure** EncodeSmart16(*v*) \
> &emsp; **if** −64 ≤ *v* < 64 **then** \
> &emsp;&emsp; emit 1 byte: (*v* + 64) \
> &emsp; **else** \
> &emsp;&emsp; emit 2 bytes: (*v* + 49152) as u16 big-endian

| Byte Form | Byte 0 Range | Value Range     |
| --------- | ------------ | --------------- |
| 1-byte    | 0x00–0x7F    | -64 to 63       |
| 2-byte    | 0x80–0xFF    | -32768 to 16383 |

### 1.3 Fixed-Point Arithmetic

The format uses Q16.16 fixed-point representation extensively:

| Constant  | Value | Description                           |
| --------- | ----- | ------------------------------------- |
| `SCALE`   | 65536 | Full fixed-point multiplier (1 << 16) |
| `OFFSET`  | 32768 | Signed/unsigned midpoint (1 << 15)    |
| `QUARTER` | 16384 | Quarter-scale for waveforms (1 << 14) |

---

## 2. File Layout

A `.synth` file consists of up to 10 sequential **voice slots** followed by a 4-byte **loop footer**:

```text
┌──────────────────────────────────┐
│  Voice Slot 0                    │
│  Voice Slot 1                    │
│  ...                             │
│  Voice Slot 9                    │
├──────────────────────────────────┤
│  Loop Start (u16)                │
│  Loop End   (u16)                │
└──────────────────────────────────┘
```

### 2.1 Voice Slot Processing

Each slot is processed sequentially. The parser peeks the first byte as a **marker**:

| Marker Value | Action                                                                              |
| ------------ | ----------------------------------------------------------------------------------- |
| `0x00`       | Empty slot. Consume 1 byte, advance to next slot.                                   |
| `1`–`4`      | Voice present. Marker doubles as the pitch envelope's waveform ID. Read voice body. |
| Other        | Invalid marker. Treat slot as null. **Stop processing** all remaining slots.        |

**Minimum size check**: Before reading a voice, the parser verifies at least **30 bytes** remain in the buffer. If fewer bytes remain and the marker is non-zero, the slot and all subsequent slots are set to null.

---

## 3. Voice Structure

When a valid marker (1–4) is encountered with sufficient remaining bytes, the voice body is read in the following strict order:

| #   | Field              | Type                   | Description                               |
| --- | ------------------ | ---------------------- | ----------------------------------------- |
| 1   | Pitch Envelope     | Envelope               | Frequency contour over time               |
| 2   | Amplitude Envelope | Envelope               | Volume contour over time                  |
| 3   | Vibrato LFO        | Optional Envelope Pair | Pitch modulation (rate + depth)           |
| 4   | Tremolo LFO        | Optional Envelope Pair | Amplitude modulation (rate + depth)       |
| 5   | Gate Envelopes     | Optional Envelope Pair | Silence duration + sound duration         |
| 6   | Partials           | Partial[]              | Additive synthesis sources (max 10)       |
| 7   | Echo Delay         | `usmart16`             | Feedback delay in milliseconds            |
| 8   | Echo Feedback      | `usmart16`             | Feedback gain as percentage (0–100)       |
| 9   | Duration           | `u16`                  | Total voice duration in milliseconds      |
| 10  | Offset             | `u16`                  | Delay before voice starts in milliseconds |
| 11  | Filter             | Filter (optional)      | IIR pole-zero filter                      |

### 3.1 Envelope Structure

| Field            | Type      | Description                                           |
| ---------------- | --------- | ----------------------------------------------------- |
| Waveform         | `u8`      | Waveform ID (0=Off, 1=Square, 2=Sine, 3=Saw, 4=Noise) |
| Start Value      | `s32`     | Initial envelope value (fixed-point)                  |
| End Value        | `s32`     | Final envelope value (fixed-point)                    |
| Segment Count    | `u8`      | Number of segments (N)                                |
| Segments[0..N-1] | Segment[] | Linear interpolation waypoints                        |

Each **segment** consists of:

| Field        | Type  | Description                                               |
| ------------ | ----- | --------------------------------------------------------- |
| Duration     | `u16` | Time to reach target (units of 1/65536 of voice duration) |
| Target Level | `u16` | Interpolation target (0–65535)                            |

**Note**: The `TargetLevel` field represents a position in the range [0, 65535] that maps linearly between `StartValue` and `EndValue`. A value of 0 maps to `StartValue`; 65535 maps to `EndValue`.

#### 3.1.1 Implicit Segment Synthesis

When an envelope has **zero segments** but *startValue* ≠ *endValue*, the parser synthesizes a single implicit segment with *duration* = voice duration (ms) and *targetLevel* = *endValue*. This ensures the envelope interpolates linearly from start to end over the voice's full duration.

### 3.2 Optional Envelope Pair

Three voice features (vibrato, tremolo, gating) use paired envelopes. Detection:

1. **Peek** the next byte (without consuming).
2. If byte = `0x00`: consume 1 byte, pair is **absent**.
3. If byte ≠ `0x00`: read **two consecutive envelopes** (rate/depth or silence/sound).

### 3.3 Partial (Oscillator) List

Read up to 10 partials. Each partial:

| Field        | Type       | Description                   |
| ------------ | ---------- | ----------------------------- |
| Amplitude    | `usmart16` | Relative volume as percentage |
| Pitch Offset | `smart16`  | Semitone offset in decicents  |
| Delay        | `usmart16` | Start delay in milliseconds   |

**Termination**: When the decoded `Amplitude` value equals 0, stop reading. Since `usmart16` encodes 0 as byte `0x00`, this is equivalent to encountering a zero byte.

### 3.4 Echo Parameters

| Field    | Type       | Description                   |
| -------- | ---------- | ----------------------------- |
| Delay    | `usmart16` | Echo delay in milliseconds    |
| Feedback | `usmart16` | Feedback gain (0–100 percent) |

Echo is only active when both `Delay > 0` and `Feedback > 0`.

---

## 4. Filter Structure

The IIR filter immediately follows the voice's duration and offset fields.

### 4.1 Filter Detection Heuristic

The first byte of the filter header (pole count packing) can collide with valid voice markers (1–4), creating ambiguity when the byte starts a new voice versus a filter. The parser resolves this by peeking ahead:

1. Let `b` = peeked byte at current position.
2. If `b == 0`: consume 1 byte, **no filter**. Return.
3. If `b` is in [1, 4] **AND** at least 10 bytes remain in the buffer:
   a. Reconstruct bytes at offsets 1–4 as a big-endian `s32`.
   b. If `|s32 value| ≤ 10,000,000` **AND** byte at offset 9 is `≤ 15`:
      → Sequence looks like a valid envelope header. Treat `b` as a **voice marker**, not a filter. Return null.
4. Otherwise: treat `b` as the **filter header byte**. Proceed to read filter.

### 4.2 Filter Header

| Field           | Type  | Description                                                  |
| --------------- | ----- | ------------------------------------------------------------ |
| Pole Config     | `u8`  | Packed: `poleCount0 = byte >> 4`, `poleCount1 = byte & 0x0F` |
| Unity Gain Ch0  | `u16` | Gain normalization for feedforward channel                   |
| Unity Gain Ch1  | `u16` | Gain normalization for feedback channel                      |
| Modulation Mask | `u8`  | Bit flags for per-pole phase-1 coefficient presence          |

The pole config byte packs two 4-bit pole counts. Each count specifies the number of second-order sections for that filter direction. Maximum is 15 per direction, though typical files use 1–4.

**Modulation mask** layout (8 bits):

| Bit | Pole                |
| --- | ------------------- |
| 0   | Direction 0, pole 0 |
| 1   | Direction 0, pole 1 |
| 2   | Direction 0, pole 2 |
| 3   | Direction 0, pole 3 |
| 4   | Direction 1, pole 0 |
| 5   | Direction 1, pole 1 |
| 6   | Direction 1, pole 2 |
| 7   | Direction 1, pole 3 |

### 4.3 Filter Coefficients

Coefficients are read in two phases across both directions (ch0 then ch1):

**Phase 0 (baseline):** For each direction with `poleCount > 0`, read `poleCount` pairs:

| Field     | Type  | Description       |
| --------- | ----- | ----------------- |
| Frequency | `u16` | Pole center phase |
| Magnitude | `u16` | Pole magnitude    |

**Phase 1 (modulated):** For each direction, for each pole index `p`:

- If bit `(direction × 4 + p)` of `modulationMask` is **set**: read a new (frequency, magnitude) pair.
- If bit is **clear**: copy phase-0 values for this pole (no bytes consumed).

The read order iterates phases as the outer loop and directions as the inner loop:

> **for** *φ* ∈ {0, 1} **do** \
> &emsp; **for** *d* ∈ {0, 1} **do** \
> &emsp;&emsp; **for** *p* ← 0 **to** poleCount[*d*] − 1 **do** \
> &emsp;&emsp;&emsp; **if** *φ* = 1 ∧ bit (*d* · 4 + *p*) of *mask* is clear **then** \
> &emsp;&emsp;&emsp;&emsp; freq[*d*, 1, *p*] ← freq[*d*, 0, *p*]; mag[*d*, 1, *p*] ← mag[*d*, 0, *p*] \
> &emsp;&emsp;&emsp; **else** \
> &emsp;&emsp;&emsp;&emsp; freq[*d*, *φ*, *p*] ← read u16; mag[*d*, *φ*, *p*] ← read u16

### 4.4 Filter Modulation Envelope

Read **only if** `modulationMask ≠ 0` **OR** `unityGainCh0 ≠ unityGainCh1`.

This is a minimal envelope containing only segments (no waveform, start, or end fields):

| Field            | Type      | Description                  |
| ---------------- | --------- | ---------------------------- |
| Segment Count    | `u8`      | Number of segments (N)       |
| Segments[0..N-1] | Segment[] | Duration + TargetLevel pairs |

The envelope is constructed with `Waveform = Off`, `StartValue = 0`, `EndValue = 0`.

### 4.5 Truncation Safety

If the buffer is truncated during filter reading, the **entire filter is discarded** (returns null). A partial filter is never returned.

---

## 5. Loop Parameters

The final 4 bytes of the file form the loop footer:

| Field      | Type  | Description                       |
| ---------- | ----- | --------------------------------- |
| Loop Start | `u16` | Sample position where loop begins |
| Loop End   | `u16` | Sample position where loop ends   |

**Active condition**: Loop is active only when `LoopStart < LoopEnd`.

If fewer than 4 bytes remain for the loop footer, loop defaults to `(0, 0)` (inactive).

---

## 6. Truncation Behavior

The `BinaryBuffer` implements a permanent truncation flag:

1. On any read that would exceed the buffer length, the `_truncated` flag is set **permanently**.
2. Subsequent reads return `0` for all types. The position still advances by the requested byte count.
3. **Peek operations** (`Peek`, `PeekAt`) never set the truncation flag; they return `0` for out-of-bounds positions.
4. Smart integer reads check the first byte to determine if 1 or 2 bytes are needed before checking truncation.

---

## 7. Waveform Table Generation

All tables are computed once at initialization and reused across all synthesis operations.

### 7.1 Sine Table

- **Size**: 32,768 entries (indexed 0–32767)
- **Formula**: `sineTable[i] = (int)(sin(i / 5215.1903) × 16384)`
- **Range**: [-16384, +16384] (Q14 fixed-point)

### 7.2 Noise Table

- **Size**: 32,768 entries
- **RNG**: Java-compatible linear congruential generator seeded with `0`
  - State: 48-bit, multiplier `0x5DEECE66D`, addend `0xB`, mask `(1 << 48) - 1`
  - `nextInt()` returns `(int)(state >> 16)` after advancing state
- **Formula**: `noiseTable[i] = (nextInt() & 2) - 1`
- **Values**: Each entry is either `-1` or `+1`

### 7.3 Semitone Cache

- **Size**: 241 entries (indices 0–240, covering decicents -120 to +120)
- **Ratio**: `decicentRatio = 2^(1/1200) = 1.0057929410678534`
- **Formula**: `cache[i] = decicentRatio^(i - 120)`
- **Fallback**: For values outside [-120, +120], computed on-the-fly as `decicentRatio^decicents`

### 7.4 Unit Circle Tables

- **Size**: 65 entries each (indices 0–64, covering 0 to 2π in 64 segments)
- **Cosine**: `xTable[i] = cos(i × 2π / 64)`
- **Sine**: `yTable[i] = sin(i × 2π / 64)`

---

## 8. Waveform Generation

All waveforms use a 32-bit signed phase accumulator. Let *φ* denote the raw accumulator and *A* the amplitude parameter.

Define the effective phase: *φ*̃ = *φ* **and** 0x7FFF

> **function** GenerateSample(*A*, *φ*, *waveform*) → ℤ \
> &emsp; *φ*̃ ← *φ* **and** 0x7FFF \
> &emsp; **match** *waveform*: \
> &emsp;&emsp; Off &emsp;&emsp;→ 0 \
> &emsp;&emsp; Square → **if** *φ*̃ < 16384 **then** +*A* **else** −*A* \
> &emsp;&emsp; Sine &emsp;→ ⌊sineTable[*φ*̃] · *A* / 2¹⁴⌋ \
> &emsp;&emsp; Saw &emsp;&ensp;→ ⌊*φ*̃ · *A* / 2¹⁴⌋ − *A* \
> &emsp;&emsp; Noise &ensp;→ noiseTable[⌊*φ* / 2607⌋ **and** 0x7FFF] · *A*

The phase accumulator wraps naturally via 32-bit integer overflow.

---

## 9. Envelope Interpolation

The `EnvelopeGenerator` is a state machine that evaluates multi-segment envelopes sample-by-sample.

### 9.1 State Variables

| Variable    | Initial Value      | Description                        |
| ----------- | ------------------ | ---------------------------------- |
| `amplitude` | `startValue << 15` | Current value in Q15 fixed-point   |
| `delta`     | `0`                | Per-tick increment                 |
| `position`  | `0`                | Current segment index              |
| `threshold` | `1`                | Tick count to trigger next segment |
| `ticks`     | `0`                | Ticks elapsed in current segment   |

### 9.2 Evaluate (per-sample)

Called once per sample with *P* = total sample count of the voice:

> **function** Evaluate(*P*) → ℤ \
> &emsp; **if** |segments| = 0 **then return** *startValue* \
> &emsp; **if** *ticks* ≥ *threshold* **then** AdvanceSegment(*P*) \
> &emsp; *amplitude* ← *amplitude* + *delta* \
> &emsp; *ticks* ← *ticks* + 1 \
> &emsp; **return** ⌊(*amplitude* − *delta*) / 2¹⁵⌋

The return value uses (*amplitude* − *delta*) to yield the **previous step's value** (pre-increment).

### 9.3 Advance Segment

> **procedure** AdvanceSegment(*P*) \
> &emsp; *amplitude* ← segments[*position*].*targetLevel* · 2¹⁵ \
> &emsp; *position* ← *position* + 1 \
> &emsp; **if** *position* ≥ |segments| **then** *position* ← |segments| − 1 \
> &emsp; *threshold* ← ⌊segments[*position*].*duration* / 65536 · *P*⌋ \
> &emsp; **if** *threshold* > *ticks* **then** \
> &emsp;&emsp; *delta* ← (segments[*position*].*targetLevel* · 2¹⁵ − *amplitude*) / (*threshold* − *ticks*) \
> &emsp; **else** \
> &emsp;&emsp; *delta* ← 0

The duration normalization ⌊*d* / 65536 · *P*⌋ converts the segment's raw duration value (in units of 1/65536 of the total voice duration) into an absolute sample count.

---

## 10. Synthesis Signal Flow

### 10.1 Voice Synthesis Pipeline

For each voice, the synthesis pipeline proceeds:

> **procedure** SynthesizeVoice(*voice*) → buffer[0..*N*−1] \
> &emsp; **Step 1.** *N* ← ⌊*voice*.*durationMs* · (*sampleRate* / 1000)⌋ \
> &emsp; **Step 2.** **if** *N* ≤ 0 ∨ *voice*.*durationMs* < 10 **then return** empty buffer \
> &emsp; **Step 3.** *σ* ← *N* / *voice*.*durationMs* &emsp; *(samples per millisecond)* \
> &emsp; **Step 4.** Initialize envelope generators for frequency, amplitude, vibrato, tremolo, filter \
> &emsp; **Step 5.** Pre-compute per-partial arrays (for each partial *p*): \
> &emsp;&emsp; delay[*p*] ← ⌊partial.*delay* · *σ*⌋ \
> &emsp;&emsp; volume[*p*] ← ⌊partial.*amplitude* · 2¹⁴ / 100⌋ \
> &emsp;&emsp; semitones[*p*] ← ⌊(*f*_end − *f*_start) · 32.768 · pitchMultiplier(*offset*) / *σ*⌋ \
> &emsp;&emsp; starts[*p*] ← ⌊*f*_start · 32.768 / *σ*⌋ \
> &emsp; **Step 6.** **for** *n* ← 0 **to** *N* − 1 **do** \
> &emsp;&emsp; *freq* ← frequencyEnvelope.Evaluate(*N*) \
> &emsp;&emsp; *amp* ← amplitudeEnvelope.Evaluate(*N*) \
> &emsp;&emsp; Apply vibrato to *freq* (§10.2) \
> &emsp;&emsp; Apply tremolo to *amp* (§10.3) \
> &emsp;&emsp; Render all partials additively into buffer (§10.4) \
> &emsp; **Step 7.** Apply gating (§10.5) \
> &emsp; **Step 8.** Apply echo (§10.6) \
> &emsp; **Step 9.** Apply IIR filter if present (§11) \
> &emsp; **Step 10.** Clip buffer to [−32768, +32767]

### 10.2 Vibrato (Pitch Modulation)

When a pitch LFO is present, the frequency is modulated per-sample. Let *φ*_v denote the vibrato phase accumulator.

Pre-computed constants:

- *S*_v = ⌊(*rateEnd* − *rateStart*) · 32.768 / *σ*⌋
- *B*_v = ⌊*rateStart* · 32.768 / *σ*⌋

> *r* ← vibratoRateEnvelope.Evaluate(*N*) \
> *d* ← vibratoDepthEnvelope.Evaluate(*N*) \
> *m* ← ⌊GenerateSample(*d*, *φ*_v, *lfoWaveform*) / 2⌋ \
> *φ*_v ← *φ*_v + *B*_v + ⌊*r* · *S*_v / 2¹⁶⌋ \
> *freq* ← *freq* + *m*

### 10.3 Tremolo (Amplitude Modulation)

When an amplitude LFO is present, the amplitude is modulated per-sample. Let *φ*_t denote the tremolo phase accumulator.

Pre-computed constants:

- *S*_t = ⌊(*rateEnd* − *rateStart*) · 32.768 / *σ*⌋
- *B*_t = ⌊*rateStart* · 32.768 / *σ*⌋

> *r* ← tremoloRateEnvelope.Evaluate(*N*) \
> *d* ← tremoloDepthEnvelope.Evaluate(*N*) \
> *m* ← ⌊GenerateSample(*d*, *φ*_t, *lfoWaveform*) / 2⌋ \
> *amp* ← ⌊*amp* · (*m* + 32768) / 2¹⁵⌋ \
> *φ*_t ← *φ*_t + *B*_t + ⌊*r* · *S*_t / 2¹⁶⌋

The factor (*m* + 32768) / 2¹⁵ modulates around unity: when *m* = 0 the amplitude is unchanged.

### 10.4 Partial Rendering

For each partial *p* with non-zero amplitude, where *φ*_p is partial *p*'s phase accumulator:

> *pos* ← *n* + delay[*p*] \
> **if** 0 ≤ *pos* < *N* **then** \
> &emsp; *a*_p ← ⌊*amp* · volume[*p*] / 2¹⁵⌋ \
> &emsp; buffer[*pos*] ← buffer[*pos*] + GenerateSample(*a*_p, *φ*_p, *pitchWaveform*) \
> &emsp; *φ*_p ← *φ*_p + ⌊*freq* · semitones[*p*] / 2¹⁶⌋ + starts[*p*]

Partials are mixed additively. Each partial maintains its own phase accumulator. The waveform used for all partials is the waveform ID from the voice's pitch (frequency) envelope.

### 10.5 Gating

When gate envelopes are present, a counter-based on/off toggle is applied. Let *s*₀, *e*₀ denote *gapOffEnvelope*'s start and end values.

> **procedure** ApplyGating(buffer, *voice*, *N*) \
> &emsp; silenceEval ← EnvelopeGenerator(*gapOffEnvelope*); Reset \
> &emsp; durationEval ← EnvelopeGenerator(*gapOnEnvelope*); Reset \
> &emsp; *c* ← 0; *muted* ← **true** \
> &emsp; **for** *n* ← 0 **to** *N* − 1 **do** \
> &emsp;&emsp; *v*_on ← silenceEval.Evaluate(*N*) \
> &emsp;&emsp; *v*_off ← durationEval.Evaluate(*N*) \
> &emsp;&emsp; **if** *muted* **then** *T* ← *s*₀ + ⌊(*e*₀ − *s*₀) · *v*_on / 2⁸⌋ \
> &emsp;&emsp; **else** *T* ← *s*₀ + ⌊(*e*₀ − *s*₀) · *v*_off / 2⁸⌋ \
> &emsp;&emsp; *c* ← *c* + 256 \
> &emsp;&emsp; **if** *c* ≥ *T* **then** *c* ← 0; *muted* ← ¬*muted* \
> &emsp;&emsp; **if** *muted* **then** buffer[*n*] ← 0

The gate always starts in the **muted** state. The threshold computation uses the gap-off envelope's start/end values for both states, with different envelope evaluators controlling the modulation.

### 10.6 Echo (Feedback Delay)

A single-tap feedback delay line, applied in-place:

> *D* ← ⌊*delayMs* · *σ*⌋ \
> **for** *n* ← *D* **to** *N* − 1 **do** \
> &emsp; buffer[*n*] ← buffer[*n*] + ⌊buffer[*n* − *D*] · *feedbackPercent* / 100⌋

The feedback is applied as integer division by 100.

---

## 11. IIR Filter

The filter implements a pole-zero cascade using second-order sections (SOS) with envelope-modulated coefficients.

### 11.1 Coefficient Computation

For each filter evaluation, coefficients are computed from the stored pole parameters and an interpolation factor `f ∈ [0.0, 1.0]` derived from the filter's modulation envelope.

#### 11.1.1 Envelope Factor

> *e* ← filterEnvelope.Evaluate(*N*) \
> *f* ← *e* / 65536

If no modulation envelope exists, *f* defaults to 65536 / 65536 = 1.0.

#### 11.1.2 Pole Amplitude

For pole *p* in direction *d*, let *m*₀ = poleMagnitude[*d*][0][*p*] and *m*₁ = poleMagnitude[*d*][1][*p*]:

> *m̂* ← *m*₀ + *f* · (*m*₁ − *m*₀) \
> *r* ← 1 − 10^(−*m̂* · 0.0015258789)

The constant 0.0015258789 ≈ 1/655.36 scales the raw magnitude into a decibel-like domain before conversion to linear amplitude.

#### 11.1.3 Pole Phase (Frequency)

For pole *p* in direction *d*, let *ψ*₀ = polePhase[*d*][0][*p*] and *ψ*₁ = polePhase[*d*][1][*p*]:

> *ψ̂* ← *ψ*₀ + *f* · (*ψ*₁ − *ψ*₀) \
> *s* ← *ψ̂* · 1.2207031 × 10⁻⁴ \
> *f*_Hz ← 2^*s* · 32.703197 \
> *θ* ← *f*_Hz · 2π / 22050

The constant 1.2207031 × 10⁻⁴ ≈ 1/8192 maps the raw phase value to octaves above the reference frequency 32.703197 Hz (MIDI note C1).

#### 11.1.4 Unity Gain (Inverse A₀)

Let *g*₀ = unityGain[0] and *g*₁ = unityGain[1]:

> *ĝ* ← *g*₀ + *f* · (*g*₁ − *g*₀) \
> *g*_dB ← *ĝ* · 0.0030517578 \
> *a*₀⁻¹ ← 10^(−*g*_dB / 20) \
> inverseA0_Q16 ← ⌊*a*₀⁻¹ · 65536⌋

The constant 0.0030517578 ≈ 1/327.68 maps the raw gain value to decibels.

### 11.2 SOS Cascade Construction

The filter constructs its transfer function by cascading second-order sections. For direction `d` with `N` poles:

**First section (pole 0):**

> sos[0] ← −2 · *r*₀ · cos(*θ*₀) \
> sos[1] ← *r*₀²

**Cascade multiplication (poles 1 through *N*−1):**

> **for** *k* ← 1 **to** *N* − 1 **do** \
> &emsp; *t*₁ ← −2 · *r*_k · cos(*θ*_k) \
> &emsp; *t*₂ ← *r*_k² \
> &emsp; sos[2*k* + 1] ← sos[2*k* − 1] · *t*₂ \
> &emsp; sos[2*k*] ← sos[2*k* − 1] · *t*₁ + sos[2*k* − 2] · *t*₂ \
> &emsp; **for** *j* ← 2*k* − 1 **downto** 2 **do** \
> &emsp;&emsp; sos[*j*] ← sos[*j*] + sos[*j* − 1] · *t*₁ + sos[*j* − 2] · *t*₂ \
> &emsp; sos[1] ← sos[1] + sos[0] · *t*₁ + *t*₂ \
> &emsp; sos[0] ← sos[0] + *t*₁

### 11.3 Coefficient Quantization

After SOS cascade construction:

- **Feedforward coefficients** (direction 0): scaled by the floating-point inverse A₀ before quantization.
- **Feedback coefficients** (direction 1): used directly.

All coefficients are quantized to Q16 fixed-point: coeff_Q16[*i*] = ⌊sos[*d*, *i*] · 65536⌋. The total coefficient count per direction is 2 · *poleCount*.

### 11.4 Sample Processing

The filter processes the buffer in three phases:

**Phase 1 — Initial block** (samples 0 to `fbCount-1`):
Limited feedback range due to insufficient history.

**Phase 2 — Main blocks** (samples `fbCount` to `sampleCount - ffCount - 1`):
Processed in chunks of 128 samples. Full feedforward and feedback access.

**Phase 3 — Final block** (samples `sampleCount - ffCount` to `sampleCount - 1`):
Truncated feedforward window (cannot look ahead past buffer end).

For each sample *n*, with *K*_ff = feedforward count and *K*_fb = feedback count:

> *acc* ← 0 &emsp; *(64-bit accumulator)* \
> \
> ▸ Feedforward (direction 0): \
> &emsp; *acc* ← *acc* + ⌊input[*n* + *K*_ff] · inverseA0_Q16 / 2¹⁶⌋ \
> &emsp; **for** *k* ← 0 **to** *K*_ff − 1 **do** \
> &emsp;&emsp; *acc* ← *acc* + ⌊input[*n* + *K*_ff − 1 − *k*] · ff[*k*] / 2¹⁶⌋ \
> \
> ▸ Feedback (direction 1): \
> &emsp; **for** *k* ← 0 **to** min(*n*, *K*_fb) − 1 **do** \
> &emsp;&emsp; *acc* ← *acc* − ⌊output[*n* − 1 − *k*] · fb[*k*] / 2¹⁶⌋ \
> \
> output[*n*] ← (int)*acc*

After each sample, the envelope is re-evaluated and coefficients are recomputed, making the filter time-varying when a modulation envelope is present.

---

## 12. Patch Composition

### 12.1 Voice Mixing

Each voice is synthesized independently, then mixed additively into a shared output buffer:

> **for each** active voice *v* **do** \
> &emsp; *buf*_v ← SynthesizeVoice(*v*) \
> &emsp; *o* ← ⌊*v*.*offsetMs* · (*sampleRate* / 1000)⌋ \
> &emsp; **for** *i* ← 0 **to** |*buf*_v| − 1 **do** \
> &emsp;&emsp; **if** 0 ≤ *i* + *o* < *N*_total **then** \
> &emsp;&emsp;&emsp; output[*i* + *o*] ← output[*i* + *o*] + *buf*_v[*i*]

### 12.2 Loop Expansion

When `loopStart < loopEnd` and `loopCount > 1`:

1. Convert loop parameters from milliseconds to samples.
2. Compute total output length: *N*_total = *N* + (*L*_end − *L*_start) · (*loopCount* − 1).
3. Shift the tail (samples after *L*_end) to the end of the expanded buffer.
4. Copy the loop region *loopCount* − 1 additional times:

> **for** *j* ← 1 **to** *loopCount* − 1 **do** \
> &emsp; *o* ← (*L*_end − *L*_start) · *j* \
> &emsp; **for** *n* ← *L*_start **to** *L*_end − 1 **do** \
> &emsp;&emsp; buffer[*n* + *o*] ← buffer[*n*]

### 12.3 Final Clipping

After mixing and loop expansion, each sample *x* is clamped to the signed 16-bit range:

> *x* ← max(−32768, min(*x*, 32767))

---

## 13. Constants Reference

| Constant                 | Value              | Description                                        |
| ------------------------ | ------------------ | -------------------------------------------------- |
| `SampleRate`             | 22050              | Audio sample rate in Hz                            |
| `AudioChannelCount`      | 1                  | Mono output                                        |
| `BitsPerSample`          | 8                  | Legacy audio bit depth                             |
| `MaxVoices`              | 10                 | Maximum voice slots per patch                      |
| `MaxOscillators`         | 10                 | Maximum partials per voice                         |
| `MaxFilterPairs`         | 15                 | Maximum poles per filter direction                 |
| `FilterUpdateRate`       | 256                | Value of `byte.MaxValue + 1`                       |
| `PhaseMask`              | 0x7FFF (32767)     | 15-bit phase accumulator mask                      |
| `PhaseScale`             | 32.768             | Phase increment scaling factor                     |
| `NoisePhaseDiv`          | 2607               | Noise waveform phase divisor                       |
| `MaxBufferSize`          | 1,048,576          | Maximum input file size (1 MiB)                    |
| `FixedPoint.Scale`       | 65536 (1 << 16)    | Q16 fixed-point multiplier                         |
| `FixedPoint.Offset`      | 32768 (1 << 15)    | Signed midpoint / table size                       |
| `FixedPoint.Quarter`     | 16384 (1 << 14)    | Quarter-scale for waveform amplitude               |
| `DecicentRatio`          | 1.0057929410678534 | Frequency ratio per decicent (2^(1/1200))          |
| `SinTableDivisor`        | 5215.1903          | Sine table generation divisor                      |
| `CircleSegments`         | 64                 | Unit circle table resolution                       |
| `SemitoneRange`          | 120                | Cached decicent range (±120)                       |
| `ChunkSize`              | 128                | Filter main-loop block size                        |
| `MaxCoefficients`        | 8                  | Maximum SOS coefficient array size                 |
| `MinVoiceSize`           | 30                 | Minimum bytes for a valid voice slot               |
| `EnvelopeStartThreshold` | 10,000,000         | Plausibility bound for filter/voice disambiguation |
| `MaxReasonableSegCount`  | 15                 | Maximum plausible segment count for disambiguation |
| `EnvelopeScaleFactor`    | 15                 | Envelope amplitude Q15 shift                       |
| `TwoPi`                  | 6.283185307179586  | 2π                                                 |

---

## 14. Waveform ID Reference

| ID  | Name   | Description                                 |
| --- | ------ | ------------------------------------------- |
| 0   | Off    | Silent output (no waveform generated)       |
| 1   | Square | Band-unlimited square wave                  |
| 2   | Sine   | Table-lookup sine wave (32768-entry LUT)    |
| 3   | Saw    | Band-unlimited sawtooth wave                |
| 4   | Noise  | Pseudo-random noise from pre-computed table |

Waveform IDs outside the range [0, 4] are treated as `Off`.
