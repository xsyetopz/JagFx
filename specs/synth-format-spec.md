# Jagex Synthesizer File Format and Engine Specification

- **Version**: 2.1
- **Origin**: OldSchool RuneScape client audio subsystem
- **Sample Rate**: 22,050 Hz (fixed)
- **Channels**: 1 (mono)
- **Byte Order**: Big-endian throughout

---

## 1. Data Types

### 1.1 Primitive Types

| Type  | Size    | Description                                            |
| ----- | ------- | ------------------------------------------------------ |
| `u8`  | 1 byte  | Unsigned 8-bit integer, range [0, 255]                 |
| `u16` | 2 bytes | Unsigned 16-bit integer, big-endian, range [0, 65535]  |
| `s32` | 4 bytes | Signed 32-bit integer, big-endian, range [-2³¹, 2³¹-1] |

### 1.2 USmart16 (Unsigned Smart Integer)

Variable-length unsigned integer, range [0, 32767].

$$
\text{DecodeUSmart16}(stream) = \begin{cases} b & \text{if } b < 128 \text{ (consume 1 byte)} \\ w - 32768 & \text{otherwise (consume 2 bytes as u16)} \end{cases}
$$

$$
\text{EncodeUSmart16}(v) = \begin{cases} \text{emit } v \text{ as 1 byte} & \text{if } v < 128 \\ \text{emit } (v + 32768) \text{ as u16} & \text{otherwise} \end{cases}
$$

| Byte Form | Byte 0 Range | Value Range |
| --------- | ------------ | ----------- |
| 1-byte    | 0x00–0x7F    | 0–127       |
| 2-byte    | 0x80–0xFF    | 0–32767     |

### 1.3 Smart16 (Signed Smart Integer)

Variable-length signed integer, range [-32768, 16383].

$$
\text{DecodeSmart16}(stream) = \begin{cases} b - 64 & \text{if } b < 128 \text{ (consume 1 byte)} \\ w - 49152 & \text{otherwise (consume 2 bytes as u16)} \end{cases}
$$

$$
\text{EncodeSmart16}(v) = \begin{cases} \text{emit } (v + 64) \text{ as 1 byte} & \text{if } -64 \le v < 64 \\ \text{emit } (v + 49152) \text{ as u16} & \text{otherwise} \end{cases}
$$

| Byte Form | Byte 0 Range | Value Range     |
| --------- | ------------ | --------------- |
| 1-byte    | 0x00–0x7F    | -64 to 63       |
| 2-byte    | 0x80–0xFF    | -32768 to 16383 |

### 1.4 Fixed-Point Arithmetic

Q16.16 fixed-point representation used throughout:

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

$\text{TargetLevel}$ maps linearly between $\text{StartValue}$ and $\text{EndValue}$: 0 → StartValue, 65535 → EndValue.

#### 3.1.1 Implicit Segment Synthesis

When an envelope has **zero segments** but $\text{startValue} \ne \text{endValue}$, the parser synthesizes a single implicit segment with $\text{duration} = \text{voiceDuration(ms)}$ and $\text{targetLevel} = \text{endValue}$.

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

**Termination**: When decoded $\text{Amplitude} = 0$, stop reading (USmart16 encodes 0 as byte `0x00`).

### 3.4 Echo Parameters

| Field    | Type       | Description                   |
| -------- | ---------- | ----------------------------- |
| Delay    | `usmart16` | Echo delay in milliseconds    |
| Feedback | `usmart16` | Feedback gain (0–100 percent) |

Echo is only active when both $\text{Delay} > 0$ and $\text{Feedback} > 0$.

---

## 4. Filter Structure

The IIR filter immediately follows the voice's duration and offset fields.

### 4.1 Filter Detection Heuristic

The first byte of the filter header can collide with valid voice markers (1–4). The parser resolves this by peeking ahead:

1. Let $b$ = peeked byte at current position.
2. If $b = 0$: consume 1 byte, **no filter**. Return.
3. If $b \in [1, 4]$ **AND** at least 10 bytes remain:
   a. Reconstruct bytes at offsets 1–4 as big-endian `s32`.
   b. If $|\text{s32}| \le 10{,}000{,}000$ **AND** byte at offset 9 is $\le 15$:
      → Treat $b$ as a **voice marker**, not a filter. Return null.
4. Otherwise: treat $b$ as the **filter header byte**. Proceed to read filter.

### 4.2 Filter Header

| Field           | Type  | Description                                                  |
| --------------- | ----- | ------------------------------------------------------------ |
| Pole Config     | `u8`  | Packed: `poleCount0 = byte >> 4`, `poleCount1 = byte & 0x0F` |
| Unity Gain Ch0  | `u16` | Gain normalization for feedforward channel                   |
| Unity Gain Ch1  | `u16` | Gain normalization for feedback channel                      |
| Modulation Mask | `u8`  | Bit flags for per-pole phase-1 coefficient presence          |

The pole config byte packs two 4-bit pole counts (max 15 per direction, typical 1–4).

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

**Phase 0 (baseline):** For each direction with $\text{poleCount} > 0$, read $\text{poleCount}$ pairs of $(\text{frequency}, \text{magnitude})$ as `u16`.

**Phase 1 (modulated):** For each direction, for each pole index $p$: if bit $(d \cdot 4 + p)$ of the modulation mask is **set**, read a new pair; if **clear**, copy phase-0 values.

Read order (phases outer, directions inner):

$$
\textbf{for } \varphi \in \{0, 1\},\; d \in \{0, 1\},\; p \in [0, \text{poleCount}[d]):
$$

$$
\text{coeffs}[d, \varphi, p] = \begin{cases} \text{read}(\text{u16}, \text{u16}) & \text{if } \varphi = 0 \text{ or bit}(d \cdot 4 + p) \text{ set} \\ \text{coeffs}[d, 0, p] & \text{otherwise (copy baseline)} \end{cases}
$$

### 4.4 Filter Modulation Envelope

Read **only if** $\text{modulationMask} \ne 0$ **OR** $\text{unityGainCh0} \ne \text{unityGainCh1}$.

Minimal envelope containing only segments (no waveform, start, or end fields):

| Field            | Type      | Description                  |
| ---------------- | --------- | ---------------------------- |
| Segment Count    | `u8`      | Number of segments (N)       |
| Segments[0..N-1] | Segment[] | Duration + TargetLevel pairs |

Constructed with $\text{Waveform} = \text{Off}$, $\text{StartValue} = 0$, $\text{EndValue} = 0$.

### 4.5 Truncation Safety

If the buffer is truncated during filter reading, the **entire filter is discarded** (returns null). A partial filter is never returned.

---

## 5. Loop Parameters

The final 4 bytes of the file form the loop footer:

| Field      | Type  | Description                       |
| ---------- | ----- | --------------------------------- |
| Loop Start | `u16` | Sample position where loop begins |
| Loop End   | `u16` | Sample position where loop ends   |

Active when $\text{LoopStart} < \text{LoopEnd}$. If fewer than 4 bytes remain, loop defaults to $(0, 0)$.

---

## 6. Truncation Behavior

The `BinaryBuffer` implements a permanent truncation flag:

1. Any read exceeding the buffer length sets `_truncated` **permanently**.
2. Subsequent reads return `0` for all types; position still advances.
3. **Peek operations** never set the truncation flag; they return `0` for out-of-bounds positions.
4. Smart integer reads check the first byte to determine 1- or 2-byte form before checking truncation.

---

## 7. Waveform Table Generation

All tables are computed once at initialization.

### 7.1 Sine Table

- **Size**: 32,768 entries (indexed 0–32767)
- **Formula**: $\text{sineTable}[i] = \lfloor \sin(i / 5215.1903) \times 16384 \rfloor$
- **Range**: $[-16384, +16384]$ (Q14 fixed-point)

### 7.2 Noise Table

- **Size**: 32,768 entries
- **RNG**: Java-compatible LCG seeded with `0` (48-bit state, multiplier `0x5DEECE66D`, addend `0xB`)
- **Formula**: $\text{noiseTable}[i] = (\text{nextInt}() \land 2) - 1 \in \{-1, +1\}$

### 7.3 Semitone Cache

- **Size**: 241 entries (indices 0–240, covering decicents -120 to +120)
- **Ratio**: $r = 2^{1/1200} = 1.0057929410678534$
- **Formula**: $\text{cache}[i] = r^{(i - 120)}$
- **Fallback**: Values outside $[-120, +120]$ computed as $r^{\text{decicents}}$

### 7.4 Unit Circle Tables

- **Size**: 65 entries each (indices 0–64)
- $\text{xTable}[i] = \cos(i \cdot 2\pi / 64)$
- $\text{yTable}[i] = \sin(i \cdot 2\pi / 64)$

---

## 8. Waveform Generation

All waveforms use a 32-bit signed phase accumulator. Let $\varphi$ denote the raw accumulator and $A$ the amplitude. Define effective phase $\tilde{\varphi} = \varphi \land \text{0x7FFF}$.

$$
\text{GenerateSample}(A, \varphi, w) = \begin{cases}
0 & w = \text{Off} \\
+A \text{ if } \tilde{\varphi} < 16384,\; -A \text{ otherwise} & w = \text{Square} \\
\lfloor \text{sineTable}[\tilde{\varphi}] \cdot A / 2^{14} \rfloor & w = \text{Sine} \\
\lfloor \tilde{\varphi} \cdot A / 2^{14} \rfloor - A & w = \text{Saw} \\
\text{noiseTable}[\lfloor \varphi / 2607 \rfloor \land \text{0x7FFF}] \cdot A & w = \text{Noise}
\end{cases}
$$

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

Called once per sample with $P$ = total sample count:

$$
\text{Evaluate}(P) = \begin{cases}
\text{startValue} & \text{if } |\text{segments}| = 0 \\
\lfloor (\text{amplitude} - \delta) / 2^{15} \rfloor & \text{otherwise}
\end{cases}
$$

Before returning: if $\text{ticks} \ge \text{threshold}$ then AdvanceSegment($P$); then $\text{amplitude} \mathrel{+}= \delta$; $\text{ticks} \mathrel{+}= 1$.

The return value uses $(\text{amplitude} - \delta)$ to yield the **previous step's value** (pre-increment).

### 9.3 Advance Segment

$$
\text{amplitude} \leftarrow \text{segments}[\text{position}].\text{targetLevel} \cdot 2^{15}
$$

$$
\text{position} \leftarrow \min(\text{position} + 1,\; |\text{segments}| - 1)
$$

$$
\text{threshold} \leftarrow \lfloor \text{segments}[\text{position}].\text{duration} \cdot P / 65536 \rfloor
$$

$$
\delta \leftarrow \begin{cases}
(\text{segments}[\text{position}].\text{targetLevel} \cdot 2^{15} - \text{amplitude}) / (\text{threshold} - \text{ticks}) & \text{if threshold} > \text{ticks} \\
0 & \text{otherwise}
\end{cases}
$$

The duration normalization $\lfloor d / 65536 \cdot P \rfloor$ converts the segment's raw duration (in units of 1/65536 of the total voice duration) into an absolute sample count.

---

## 10. Synthesis Signal Flow

### 10.1 Voice Synthesis Pipeline

For each voice:

1. $N \leftarrow \lfloor \text{durationMs} \cdot (\text{sampleRate} / 1000) \rfloor$. If $N \le 0$ or $\text{durationMs} < 10$, return empty.
2. $\sigma \leftarrow N / \text{durationMs}$ (samples per millisecond)
3. Initialize envelope generators for frequency, amplitude, vibrato, tremolo, filter.
4. Pre-compute per-partial arrays (for each partial $p$):

$$
\text{delay}[p] = \lfloor \text{partial.delay} \cdot \sigma \rfloor \qquad
\text{volume}[p] = \lfloor \text{partial.amplitude} \cdot 2^{14} / 100 \rfloor
$$

$$
\text{semitones}[p] = \lfloor (f_\text{end} - f_\text{start}) \cdot 32.768 \cdot \text{pitchMultiplier}(\text{offset}) / \sigma \rfloor
$$

$$
\text{starts}[p] = \lfloor f_\text{start} \cdot 32.768 / \sigma \rfloor
$$

1. Per-sample loop ($n = 0$ to $N-1$): evaluate frequency/amplitude envelopes, apply vibrato (§10.2), tremolo (§10.3), render partials (§10.4).
2. Apply gating (§10.5), echo (§10.6), IIR filter (§11), clip to $[-32768, 32767]$.

### 10.2 Vibrato (Pitch Modulation)

Pre-computed: $S_v = \lfloor (r_\text{end} - r_\text{start}) \cdot 32.768 / \sigma \rfloor$, $B_v = \lfloor r_\text{start} \cdot 32.768 / \sigma \rfloor$

$$
m \leftarrow \lfloor \text{GenerateSample}(d, \varphi_v, w_\text{lfo}) / 2 \rfloor
$$

$$
\varphi_v \leftarrow \varphi_v + B_v + \lfloor r \cdot S_v / 2^{16} \rfloor
$$

$$
\text{freq} \leftarrow \text{freq} + m
$$

Where $r$ and $d$ are the vibrato rate/depth envelope evaluations for the current sample.

### 10.3 Tremolo (Amplitude Modulation)

Pre-computed: $S_t = \lfloor (r_\text{end} - r_\text{start}) \cdot 32.768 / \sigma \rfloor$, $B_t = \lfloor r_\text{start} \cdot 32.768 / \sigma \rfloor$

$$
m \leftarrow \lfloor \text{GenerateSample}(d, \varphi_t, w_\text{lfo}) / 2 \rfloor
$$

$$
\text{amp} \leftarrow \lfloor \text{amp} \cdot (m + 32768) / 2^{15} \rfloor
$$

$$
\varphi_t \leftarrow \varphi_t + B_t + \lfloor r \cdot S_t / 2^{16} \rfloor
$$

The factor $(m + 32768) / 2^{15}$ modulates around unity: when $m = 0$ the amplitude is unchanged.

### 10.4 Partial Rendering

For each partial $p$ with non-zero amplitude, where $\varphi_p$ is partial $p$'s phase accumulator:

$$
\text{pos} \leftarrow n + \text{delay}[p]
$$

If $0 \le \text{pos} < N$:

$$
a_p \leftarrow \lfloor \text{amp} \cdot \text{volume}[p] / 2^{15} \rfloor
$$

$$
\text{buffer}[\text{pos}] \mathrel{+}= \text{GenerateSample}(a_p, \varphi_p, w_\text{pitch})
$$

$$
\varphi_p \leftarrow \varphi_p + \lfloor \text{freq} \cdot \text{semitones}[p] / 2^{16} \rfloor + \text{starts}[p]
$$

Partials are mixed additively. The waveform is the voice's pitch envelope waveform ID.

### 10.5 Gating

When gate envelopes are present. Let $s_0, e_0$ denote gapOffEnvelope's start and end values.

Counter $c$ starts at 0, $\text{muted} = \text{true}$. Per sample:

$$
T \leftarrow \begin{cases}
s_0 + \lfloor (e_0 - s_0) \cdot v_\text{on} / 256 \rfloor & \text{if muted} \\
s_0 + \lfloor (e_0 - s_0) \cdot v_\text{off} / 256 \rfloor & \text{otherwise}
\end{cases}
$$

$c \mathrel{+}= 256$. If $c \ge T$: $c \leftarrow 0$, toggle muted. If muted: $\text{buffer}[n] \leftarrow 0$.

### 10.6 Echo (Feedback Delay)

Single-tap feedback delay line, applied in-place:

$$
D \leftarrow \lfloor \text{delayMs} \cdot \sigma \rfloor
$$

$$
\textbf{for } n = D \textbf{ to } N-1: \quad \text{buffer}[n] \mathrel{+}= \lfloor \text{buffer}[n - D] \cdot \text{feedbackPercent} / 100 \rfloor
$$

---

## 11. IIR Filter

The filter implements a pole-zero cascade using second-order sections (SOS) with envelope-modulated coefficients.

### 11.1 Coefficient Computation

For each filter evaluation, coefficients are computed from stored pole parameters and an interpolation factor $f \in [0, 1]$ from the modulation envelope.

#### 11.1.1 Envelope Factor

$$
f \leftarrow \text{filterEnvelope.Evaluate}(N) / 65536
$$

If no modulation envelope exists, $f = 1.0$.

#### 11.1.2 Pole Amplitude

For pole $p$ in direction $d$, let $m_0 = \text{poleMag}[d][0][p]$ and $m_1 = \text{poleMag}[d][1][p]$:

$$
\hat{m} \leftarrow m_0 + f \cdot (m_1 - m_0)
$$

$$
r \leftarrow 1 - 10^{-\hat{m} \cdot 0.0015258789 / 20}
$$

The constant $0.0015258789 \approx 1/655.36$ scales the raw magnitude into decibels; the $/20$ performs the standard dB-to-amplitude conversion ($10^{x/20}$).

#### 11.1.3 Pole Phase (Frequency)

For pole $p$ in direction $d$, let $\psi_0 = \text{polePhase}[d][0][p]$ and $\psi_1 = \text{polePhase}[d][1][p]$:

$$
\hat{\psi} \leftarrow \psi_0 + f \cdot (\psi_1 - \psi_0)
$$

$$
s \leftarrow \hat{\psi} \cdot 1.2207031 \times 10^{-4}
$$

$$
f_\text{Hz} \leftarrow 2^s \cdot 32.703197
$$

$$
\theta \leftarrow f_\text{Hz} \cdot 2\pi / 22050
$$

The constant $1.2207031 \times 10^{-4} \approx 1/8192$ maps the raw phase value to octaves above $32.703197$ Hz (MIDI note C1).

#### 11.1.4 Unity Gain (Inverse A₀)

Let $g_0 = \text{unityGain}[0]$ and $g_1 = \text{unityGain}[1]$:

$$
\hat{g} \leftarrow g_0 + f \cdot (g_1 - g_0)
$$

$$
a_0^{-1} \leftarrow 10^{-\hat{g} \cdot 0.0030517578 / 20}
$$

$$
\text{inverseA0}\_\text{Q16} \leftarrow \lfloor a_0^{-1} \cdot 65536 \rfloor
$$

The constant $0.0030517578 \approx 1/327.68$ maps the raw gain value to decibels.

### 11.2 SOS Cascade Construction

The filter constructs its transfer function by cascading second-order sections. For direction $d$ with $N$ poles:

**First section (pole 0):**

$$
\text{sos}[0] \leftarrow -2 \cdot r_0 \cdot \cos(\theta_0) \qquad \text{sos}[1] \leftarrow r_0^2
$$

**Cascade multiplication (poles 1 through $N-1$):**

For each $k$, let $t_1 = -2 r_k \cos(\theta_k)$ and $t_2 = r_k^2$:

$$
\text{sos}[2k+1] \leftarrow \text{sos}[2k-1] \cdot t_2
$$

$$
\text{sos}[2k] \leftarrow \text{sos}[2k-1] \cdot t_1 + \text{sos}[2k-2] \cdot t_2
$$

$$
\textbf{for } j = 2k-1 \textbf{ downto } 2: \quad \text{sos}[j] \mathrel{+}= \text{sos}[j-1] \cdot t_1 + \text{sos}[j-2] \cdot t_2
$$

$$
\text{sos}[1] \mathrel{+}= \text{sos}[0] \cdot t_1 + t_2 \qquad \text{sos}[0] \mathrel{+}= t_1
$$

### 11.3 Coefficient Quantization

After SOS cascade construction:

- **Feedforward** (direction 0): scaled by floating-point inverse A₀ before quantization.
- **Feedback** (direction 1): used directly.

All coefficients quantized to Q16: $\text{coeff}\_\text{Q16}[i] = \lfloor \text{sos}[d, i] \cdot 65536 \rfloor$. Total per direction: $2 \cdot \text{poleCount}$.

### 11.4 Sample Processing

The filter processes the buffer in three phases:

- **Phase 1** (samples $0$ to $K_\text{fb}-1$): limited feedback range due to insufficient history.
- **Phase 2** (samples $K_\text{fb}$ to $N - K_\text{ff} - 1$): full access, processed in 128-sample chunks.
- **Phase 3** (samples $N - K_\text{ff}$ to $N - 1$): truncated feedforward window.

For each sample $n$:

$$
\text{acc} \leftarrow \lfloor \text{input}[n + K_\text{ff}] \cdot \text{inverseA0}\_\text{Q16} / 2^{16} \rfloor + \sum_{k=0}^{K_\text{ff}-1} \lfloor \text{input}[n + K_\text{ff} - 1 - k] \cdot \text{ff}[k] / 2^{16} \rfloor
$$

$$
\text{acc} \mathrel{-}= \sum_{k=0}^{\min(n, K_\text{fb})-1} \lfloor \text{output}[n - 1 - k] \cdot \text{fb}[k] / 2^{16} \rfloor
$$

$$
\text{output}[n] \leftarrow (\text{int})\,\text{acc}
$$

After each sample, the envelope is re-evaluated and coefficients recomputed, making the filter time-varying when a modulation envelope is present.

---

## 12. Patch Composition

### 12.1 Voice Mixing

Each voice is synthesized independently, then mixed additively:

$$
\text{output}[i + o] \mathrel{+}= \text{buf}_v[i] \quad \text{where } o = \lfloor \text{offsetMs} \cdot (\text{sampleRate} / 1000) \rfloor
$$

### 12.2 Loop Expansion

When $\text{loopStart} < \text{loopEnd}$ and $\text{loopCount} > 1$:

1. Convert loop parameters from milliseconds to samples.
2. $N_\text{total} = N + (L_\text{end} - L_\text{start}) \cdot (\text{loopCount} - 1)$
3. Shift tail (samples after $L_\text{end}$) to end of expanded buffer.
4. Copy loop region $\text{loopCount} - 1$ additional times:

$$
\textbf{for } j = 1 \textbf{ to } \text{loopCount}-1,\; n \in [L_\text{start}, L_\text{end}): \quad \text{buffer}[n + (L_\text{end} - L_\text{start}) \cdot j] \leftarrow \text{buffer}[n]
$$

### 12.3 Final Clipping

$$
x \leftarrow \max(-32768, \min(x, 32767))
$$

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
