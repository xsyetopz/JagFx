## ADDED Requirements

### Requirement: Classic titles with descriptions

Each cell SHALL show the original tool's title: Freq Base, Freq Modulation rate, Freq Modulation range, Amp Base, Amp Modulation rate, Amp Modulation range, Final Output, Gap off, Gap on, Graph of poles and zeros, Transition Curve, and Frequency Response. Each title SHALL have a tooltip with a one-line plain-language description in the UI language.

#### Scenario: Description on hover

- **WHEN** the user hovers the Freq Modulation rate title
- **THEN** a tooltip says how fast the pitch wobbles (the vibrato speed)

#### Scenario: Titles in another language

- **WHEN** the UI language is German
- **THEN** the cell titles stay as in the original
- **AND** the descriptions are in German

### Requirement: Empty module prompt

When a module of the selected tone is off, its cells SHALL say what the module does and offer the control that turns it on.

#### Scenario: Vibrato is off

- **WHEN** the selected tone has no pitch LFO
- **THEN** Freq Modulation rate and Freq Modulation range say that vibrato is off and that a waveform turns it on
- **AND** the cells show the waveform buttons

#### Scenario: The filter is off

- **WHEN** the filter column is visible and the tone has no filter
- **THEN** the filter plots say that the filter is off and offer On

### Requirement: Disabled tone prompt

When the selected tone is off, the rack SHALL appear dimmed and SHALL show which tone is off and a Turn on button.

#### Scenario: Turning a tone on

- **WHEN** tone 3 is off and the user presses Turn on
- **THEN** tone 3 is on and the rack shows its cells at full strength

### Requirement: Untitled patch at start

When the editor starts without a file, it SHALL show an untitled patch with tone 1 on, the same patch that New makes.

#### Scenario: First start

- **WHEN** the editor starts without a file
- **THEN** the title says `untitled` and the rack shows tone 1

### Requirement: Status feedback

After each file action or edit, the status bar SHALL say what happened. After an undoable edit, it SHALL say how to undo it.

#### Scenario: Save

- **WHEN** the user saves `cow_death.synth`
- **THEN** the status bar says the file was saved, with its name

#### Scenario: Failed import

- **WHEN** the user imports a `.filter` file that does not decode
- **THEN** the status bar names the file and the reason

### Requirement: Honest file labels

The editor SHALL describe `.tone` and `.filter` files as JagFx tone and filter files, and SHALL NOT claim that the original tool reads them.

#### Scenario: Export tooltip

- **WHEN** the user hovers Export Filter
- **THEN** the tooltip says it saves a JagFx filter file
