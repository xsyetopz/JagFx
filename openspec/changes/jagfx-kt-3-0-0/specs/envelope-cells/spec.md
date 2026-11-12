## ADDED Requirements

### Requirement: Cell header buttons

Every plot SHALL show the header buttons `1x`, `2x`, `4x`, and `S` in that order. Every time-domain cell (the eight envelope cells, Transition Curve, and Final Output) SHALL also show `M…` and `▼` after them. Graph of poles and zeros and Frequency Response SHALL NOT show `M…` or `▼`. Each button SHALL have a tooltip that names its action.

#### Scenario: Header of a time-domain cell

- **WHEN** a patch is open
- **THEN** the Freq Base header shows `1x`, `2x`, `4x`, `S`, `M…`, and `▼` from left to right

#### Scenario: Header of the pole and zero graph

- **WHEN** the filter column is visible
- **THEN** the Graph of poles and zeros header shows `1x`, `2x`, `4x`, and `S` only

### Requirement: Zoom and pan

`1x`, `2x`, and `4x` SHALL set the plot's zoom to that level. `S` SHALL fit the plot to the whole voice or the whole plane. Ctrl or Cmd with the scroll wheel SHALL zoom around the pointer, and a drag with the middle button SHALL pan.

#### Scenario: Zoom preset

- **WHEN** the user presses `4x` on Amp Base
- **THEN** Amp Base shows a quarter of the voice's length

#### Scenario: Wheel zoom keeps the point under the pointer

- **WHEN** the pointer is over 300 ms and the user zooms in with Ctrl and the wheel
- **THEN** the time under the pointer stays 300 ms

#### Scenario: Fit

- **WHEN** a cell is zoomed and scrolled and the user presses `S`
- **THEN** the cell shows the whole voice at zoom 1

### Requirement: Linked time view

Time-domain cells SHALL share one time view by default, so a zoom or pan in one applies to all linked cells. `M…` SHALL switch a cell between "Linked" and "This cell".

#### Scenario: Linked zoom

- **WHEN** every time-domain cell is linked and the user presses `2x` on Freq Base
- **THEN** Amp Base, Final Output, and every other linked cell show the same half of the voice

#### Scenario: Unlinked cell

- **WHEN** the user sets Gap off to "This cell" with `M…` and zooms Freq Base
- **THEN** Gap off keeps its own view

### Requirement: Axes with units

Each time-domain cell SHALL label its time axis in ms of the voice. Each envelope cell SHALL label its value axis in the slot's unit: Hz for the pitch and rate envelopes, % for the depth and volume envelopes, and ms for the gate envelopes.

#### Scenario: Time axis

- **WHEN** the voice is 1200 ms long and the cell is at zoom 1
- **THEN** the time axis runs from 0 ms to 1200 ms with "ms" on its labels

#### Scenario: Value axis

- **WHEN** Freq Base is visible and Min Hz is 30 and Max Hz is 250
- **THEN** the value axis shows labels in Hz between 30 and 250

### Requirement: Point readout

While a point is hovered, dragged, or selected, its cell SHALL show the point's time in ms, its value in the slot's unit, and its raw position (out of 65536) and raw level.

#### Scenario: Hover readout

- **WHEN** the user hovers a point at raw position 32768 in a 1000 ms voice
- **THEN** the readout shows 500 ms, the value in its unit, and `32768/65536` with the raw level

#### Scenario: Drag readout follows the point

- **WHEN** the user drags a point
- **THEN** the readout updates on each move and stays next to the point

### Requirement: Point editing with the mouse

In an envelope cell, double-click SHALL add a point, a drag SHALL move a point, and a right-click SHALL open a menu to delete the point, add a point there, or add a point at the end. A point SHALL NOT move past its neighbors in time.

#### Scenario: Add by double-click

- **WHEN** the user double-clicks an empty spot in Amp Base
- **THEN** a point appears there and becomes selected

#### Scenario: Drag stops at a neighbor

- **WHEN** the user drags point 2 to the right past point 3
- **THEN** point 2 stops at point 3's time

### Requirement: Exact point table

The controls panel SHALL list every point of the cell with editable fields for time in ms, value in the unit, raw position, and raw level. A change in one field SHALL update the others and the plot.

#### Scenario: Editing a raw level

- **WHEN** the user types a raw level into a row of the table
- **THEN** the plot moves that point to the level
- **AND** the value field of the row shows the level in the unit

#### Scenario: Out-of-range entry

- **WHEN** the user types a raw position above 65535
- **THEN** the field shows an error with the allowed range and the point does not move
