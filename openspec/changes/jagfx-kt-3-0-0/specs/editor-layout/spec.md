## ADDED Requirements

### Requirement: Classic rack grid

The editor SHALL lay out the selected voice's cells in three rows and three columns. The rows are Freq, Amp, and Output. The columns are Base, Modulation rate, and Modulation range. The cells are Freq Base (pitch envelope), Freq Modulation rate (vibrato rate), Freq Modulation range (vibrato depth), Amp Base (volume envelope), Amp Modulation rate (tremolo rate), Amp Modulation range (tremolo depth), Final Output (output wave), Gap off, and Gap on.

#### Scenario: Main mode shows the classic grid

- **WHEN** a patch is open and the Mode switch is on Main
- **THEN** the first row shows Freq Base, Freq Modulation rate, and Freq Modulation range from left to right
- **AND** the second row shows Amp Base, Amp Modulation rate, and Amp Modulation range
- **AND** the third row shows Final Output, Gap off, and Gap on

#### Scenario: The filter envelope is not in the main grid

- **WHEN** the Mode switch is on Main
- **THEN** no cell of the 3 by 3 grid shows the filter envelope

### Requirement: Module strips beside the Base column

Each rack row SHALL show a module strip between its Base cell and its Modulation rate cell. The Freq and Amp strips SHALL hold the LFO waveform buttons (Off, Sqr, Sin, Saw, Noise), a Min field, a Max field, and a read-only average, each labelled with its unit. The Output row strip SHALL hold the gate Off and On buttons, Min Gap, Max Gap, and Av Gap in ms.

#### Scenario: Turning vibrato on from the strip

- **WHEN** the vibrato is off and the user presses Sin in the Freq strip
- **THEN** the voice gains a pitch LFO whose rate envelope uses the Sine waveform
- **AND** the Freq Modulation rate and Freq Modulation range cells show their envelopes

#### Scenario: Editing Max Hz

- **WHEN** the user sets Max Hz in the Freq strip to 150
- **THEN** the vibrato rate envelope's `endValue` holds the raw value for 150 Hz
- **AND** the average field shows the mean of Min and Max in Hz

### Requirement: Left voice strip

The editor SHALL show a left strip holding, from top to bottom, the oscillator waveform buttons, Min Hz, Max Hz, Av Hz, Len ms, Pos ms, L1 ms, L2 ms, Play, Play All, Loop, Loop All, Loops, Stop, Load, Save, and True Wave.

#### Scenario: True Wave is reachable

- **WHEN** the user presses True Wave
- **THEN** the editor dispatches `ViewIntent.SetTrueWave` with the opposite of the current value
- **AND** the button shows the active state while True Wave is on

#### Scenario: Play plays one tone and Play All plays the patch

- **WHEN** the user presses Play
- **THEN** the editor plays only the selected tone
- **WHEN** the user presses Play All
- **THEN** the editor plays every enabled tone of the patch

### Requirement: Bottom tone and harmonics bar

The editor SHALL show a bottom bar with the Tones selector 1 to 10, Copy, Reset Active, five harmonic boxes with Semi, Vol, and Del fields, a Reverb box with vol and del fields, and the Mode switch.

#### Scenario: Selecting a tone

- **WHEN** the user presses tone 3 in the bottom bar
- **THEN** every cell, strip, and field shows tone 3

#### Scenario: Copying a tone

- **WHEN** the user presses Copy while tone 1 is selected, then clicks tone 4
- **THEN** tone 4 becomes a copy of tone 1
- **AND** Copy shows the active state until the paste

### Requirement: Mode switch with a filter column

The editor SHALL offer a Mode switch with Main, Filter, and Both. Main SHALL show the 3 by 3 grid. Filter SHALL show only the filter column. Both SHALL show the grid and the filter column side by side. The filter column SHALL hold, from top to bottom, Graph of poles and zeros, Transition Curve (the filter envelope), and Frequency Response.

#### Scenario: Both mode

- **WHEN** the user selects Both
- **THEN** the filter column appears right of Freq Modulation range, Amp Modulation range, and Gap on
- **AND** Transition Curve sits in the Amp row

#### Scenario: Mode survives a tone change

- **WHEN** the Mode switch is on Filter and the user selects another tone
- **THEN** the Mode switch stays on Filter

### Requirement: Filter strip

When the filter column is visible, the editor SHALL show a filter strip left of it with the filter Off and On buttons, Import Filter, Export Filter, the pole counts, the unity gains, and the selected pole's frequency in Hz and resonance in dB.

#### Scenario: Turning the filter on

- **WHEN** the voice has no filter and the user presses On in the filter strip
- **THEN** the voice gains a filter with one pole
- **AND** the three filter plots show it

#### Scenario: Exporting a filter

- **WHEN** the user presses Export Filter
- **THEN** the editor saves the voice's filter as a `.filter` file through `ToneFilterTransfer`

### Requirement: Per-cell controls panel

Each time-domain cell SHALL have a `▼` header button that shows or hides that cell's controls panel. The panel SHALL hold the cell's point table, with time in ms, value in the cell's unit, and the raw position and level, and the cell's time-view option. The editor SHALL NOT show a permanent inspector panel.

#### Scenario: Opening the controls panel

- **WHEN** the user presses `▼` on Amp Base
- **THEN** the Amp Base controls panel opens with one row per point
- **WHEN** the user presses `▼` again
- **THEN** the panel closes and the cell regains its size

### Requirement: Layout fits common window sizes

The editor SHALL show every control of Both mode without clipping or horizontal scrolling at a window of 1280 by 800 dp.

#### Scenario: Small window

- **WHEN** the window is 1280 by 800 dp and the Mode switch is on Both
- **THEN** every strip, cell header button, and bottom-bar field is visible and operable
