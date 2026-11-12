## ADDED Requirements

### Requirement: Readable text

Body text SHALL be at least 12 sp, and label text at least 11 sp. Text SHALL have a contrast ratio of at least 4.5:1 against its background in every theme.

#### Scenario: Dim text contrast

- **WHEN** the Classic dark theme is active
- **THEN** the dimmest text color has at least 4.5:1 contrast against the panel color

### Requirement: Target size

Every pointer target SHALL have a hit area of at least 24 by 24 dp, even when the visible button is smaller.

#### Scenario: Header buttons

- **WHEN** the user clicks within 24 dp of the center of the `2x` button
- **THEN** the click reaches `2x` and no neighboring button

### Requirement: Color is not the only signal

An active toggle SHALL differ from an inactive one by more than color. A selected cell SHALL show a border.

#### Scenario: Active waveform

- **WHEN** Sin is the active waveform
- **THEN** the Sin button shows a mark or a heavier label in addition to the green fill

### Requirement: Visible focus

Every focusable control SHALL show a focus ring of at least 2 dp with at least 3:1 contrast against its surroundings.

#### Scenario: Tab to a field

- **WHEN** the user presses Tab until Max Hz has focus
- **THEN** Max Hz shows a focus ring

### Requirement: Keyboard reach

Tab SHALL move through the title bar, the left strip, the rack in reading order, the filter strip and column when visible, and the bottom bar. With a cell focused, the arrow keys with Alt SHALL move the focus to the neighboring cell through `SlotIntent.SelectTargetByOffset`.

#### Scenario: Move between cells

- **WHEN** Freq Base has focus and the user presses Alt and Right
- **THEN** the focus moves to Freq Modulation rate

### Requirement: Keyboard point editing

In a focused envelope cell, Left and Right SHALL select the previous and next point. Shift with an arrow SHALL move the selected point by one step, and Ctrl or Cmd with Shift and an arrow by a fine step. Insert or `+` SHALL add a point after the selected one, and Delete or Backspace SHALL remove it. Each move SHALL be undoable as one step.

#### Scenario: Move a point by keyboard

- **WHEN** point 2 is selected and the user presses Shift and Up
- **THEN** point 2's level rises by one step
- **AND** the readout shows the new value

#### Scenario: Add a point by keyboard

- **WHEN** point 2 is selected and the user presses Insert
- **THEN** a point appears halfway between points 2 and 3 and becomes selected

### Requirement: Screen reader names

Every icon-only or symbol-only control, including `1x`, `2x`, `4x`, `S`, `M…`, and `▼`, SHALL expose a descriptive accessible name.

#### Scenario: Symbol button name

- **WHEN** a screen reader reads the `▼` button of Amp Base
- **THEN** it announces that the button shows the Amp Base controls

### Requirement: EN 301 549 conformance target

The desktop and web editors SHALL meet the EN 301 549 chapter 11 requirements for non-web software at WCAG 2.2 level AA. That level covers EN 301 549 v3.2.1, the version the European Accessibility Act currently cites, and v4.1.1. Each release SHALL publish an accessibility statement that lists the criteria met, the known gaps, and a contact for reports.

#### Scenario: Statement ships with a release

- **WHEN** a release is built
- **THEN** `docs/accessibility.md` names the standard versions, each unmet criterion with its workaround, and the contact address

### Requirement: Sidebar side for either hand

The user SHALL be able to put the side panel on the left or the right edge of the window. In the 3.0.0 layout this panel is the voice strip; before that it is the properties panel. The rack SHALL keep its column order on both sides. The choice SHALL be reachable from the title bar's file menu, from the desktop View menu, and by keyboard. It SHALL survive a restart, and SHALL never change the patch or the undo history.

#### Scenario: Left-handed layout

- **WHEN** the user picks "Panel on Left" in the View menu
- **THEN** the side panel moves to the left edge, and the rack keeps its column order

#### Scenario: Choice survives a restart

- **WHEN** the user picks "Panel on Left", quits, and opens the editor again
- **THEN** the side panel is on the left

#### Scenario: Not an edit

- **WHEN** the user switches the panel side
- **THEN** the patch is unchanged, the file is not marked dirty, and Undo does not switch it back

### Requirement: Platform preferences

The editor SHALL follow the platform's high-contrast, text scale, and reduced-motion settings where the platform reports them, as EN 301 549 clause 11.7 asks. It SHALL also offer its own text size from 100% to 200% and a high-contrast theme, for platforms that do not report them. At 200% text the editor SHALL lose no content or function (WCAG 1.4.4).

#### Scenario: Windows high contrast

- **WHEN** Windows high contrast is on as the editor starts
- **THEN** the editor uses its high-contrast theme

#### Scenario: Double text size

- **WHEN** the user sets the text size to 200% in a 1280 by 800 window
- **THEN** every control stays reachable, and the strips wrap or scroll instead of clipping

### Requirement: Contrast of plots and controls

Envelope lines, point markers, the output wave, the frequency response, poles and zeros, control borders, and focus rings SHALL each have at least 3:1 contrast against what lies next to them (WCAG 1.4.11).

#### Scenario: Envelope line contrast

- **WHEN** the Classic dark theme draws the Freq Base envelope
- **THEN** the line and its point markers have at least 3:1 contrast against the plot background

### Requirement: Assistive technology names, roles, and states

Every control SHALL expose its name, role, value, and state through the platform accessibility API (EN 301 549 clause 11.5, WCAG 4.1.2). Every visible label SHALL be part of the accessible name (WCAG 2.5.3). Each plot SHALL expose a text summary: the module, its waveform, the point count, and the value range in its unit (WCAG 1.1.1).

#### Scenario: Toggle state

- **WHEN** a screen reader reaches the Loop button while looping is on
- **THEN** it announces "Loop playback, toggle, on"

#### Scenario: Plot summary

- **WHEN** a screen reader reaches the Amp Base cell
- **THEN** it announces the module, the waveform, the number of points, and the lowest and highest level in percent

### Requirement: Status messages reach assistive technology

Status bar messages, including rejected edits and file errors, SHALL be announced without moving the focus (WCAG 4.1.3). A message SHALL stay until the next one replaces it (WCAG 2.2.1).

#### Scenario: Rejected edit

- **WHEN** an edit is rejected because the tone is off
- **THEN** a screen reader announces the status message while the focus stays on the edited control

### Requirement: Pointer alternatives

Every drag SHALL have a single-click or keyboard alternative (WCAG 2.5.7). Every press SHALL act on release and be cancelled by moving off the target before release (WCAG 2.5.2). Wheel zoom and pan SHALL each have a button or key alternative. No action SHALL need two keys pressed on separate parts of the keyboard at once without a menu alternative (EN 301 549 clause 5.9).

#### Scenario: Pan without a drag

- **WHEN** a keyboard user presses Shift with Page Down in a focused cell
- **THEN** the time view pans right by one screen

### Requirement: Tooltips the user controls

Tooltips SHALL appear on keyboard focus as well as hover. They SHALL stay while the pointer moves onto them, and SHALL close on Escape without moving the focus (WCAG 1.4.13).

#### Scenario: Dismiss a tooltip

- **WHEN** the tooltip of the `S` button is showing and the user presses Escape
- **THEN** the tooltip closes and the focus stays on `S`

### Requirement: Playback the user controls

Playback SHALL never start by itself on load. Any playback longer than three seconds, including loops, SHALL stop with Stop, Space, and Escape (WCAG 1.4.2). The playhead SHALL not flash more than three times per second (WCAG 2.3.1).

#### Scenario: Loaded file stays silent

- **WHEN** the user opens a `.synth` file
- **THEN** nothing plays until the user presses Play
