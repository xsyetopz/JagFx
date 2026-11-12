## Why

The Kotlin 3.0.0 editor works, but its screen does not read like the in-house JagFx that Jagex sound designers use, and it does not explain itself to someone who is not a sound engineer.
The reference screenshot and six videos in `references/images/` and `references/videos/` (2017 to 2025) show one stable layout of the original tool. The latest is the 2025 Alan Walker clip, which shows a dark theme on Windows 11. The 3.0.0 rack transposes it, moves the per-module controls into a side inspector, and crowds every voice and loop control into one header row.
Its text is 9 to 10 sp, its controls are 18 dp high, and its dimmest text has about 2.2:1 contrast. Two features exist in the state but have no control: True Wave and keyboard moves between cells.

## What Changes

- Lay out the rack as the original does: rows Freq, Amp, and Output with the gate, and columns Base, Modulation rate, and Modulation range. Each row puts its module strip (waveform, Min, Max, and Average) between the Base column and the modulation columns.
- Add the original Mode switch: Main, Filter, and Both. Main shows the 3 by 3 rack. Filter and Both show the filter column: the pole and zero graph, the transition curve (the filter envelope), and the frequency response.
- Move the voice controls into a left strip, as in the original: the oscillator waveform, Min Hz and Max Hz, Len ms, Pos ms, L1 ms, L2 ms, the transport, the loop count, and True Wave.
- Move the voice selector ("Editing Tone" 1 to 10 with Copy and Reset) and the five harmonics with reverb into a bottom bar, as in the original.
- **BREAKING (UI only)**: remove the right-hand inspector panel. Its exact point table moves to a per-cell controls panel that the `▼` header button shows and hides. The filter fields, with Import Filter and Export Filter, move to a strip by the pole and zero graph.
- Keep the original's cell header buttons in their order and give each a stated job: `1x 2x 4x` zoom presets and `S` (Fit) on every plot; `M…` (time view, linked or per cell) and `▼` (controls panel) on time-domain cells only. Add wheel zoom and drag pan, a time axis in ms, and a value axis in the cell's unit. While a point is hovered or dragged, a readout shows its time in ms, its value in the cell's unit, and the raw `u16` position and level stored in the file.
- Make the tool readable for newcomers. Each cell keeps its classic title and gains a plain-language description. An empty module shows what turning it on does and offers the waveform buttons in place. The screen with no file open offers Open and New.
- Restyle with a Classic dark palette modeled on the 2025 theme: dark grey chrome, black plots, pale yellow-green envelope lines, a pale green output wave, and green active toggles. Raise text, target size, and contrast to the WCAG 2.2 AA minimums.
- Target EN 301 549 chapter 11 at WCAG 2.2 AA, the EU's harmonised accessibility standard: screen reader names and states, plot summaries, announced status messages, the platform's contrast and text size settings, a 200% text size, and an accessibility statement.
- Let the user put the side panel on the left or the right, so left-handed users keep it on their mouse side. The choice survives a restart.
- Remove the unused `Knob` widget.

## Capabilities

### New Capabilities

- `editor-layout`: the classic rack, module strips, Mode switch, left voice strip, bottom tone and harmonics bar, filter strip, and per-cell controls panel.
- `envelope-cells`: time and value axes, the shared zoom and pan, the hover and drag readout with raw `u16` values, and editing points with mouse and keyboard.
- `editor-guidance`: classic titles with plain-language descriptions, empty-module prompts, the no-file screen, and status messages.
- `editor-accessibility`: EN 301 549 / WCAG 2.2 AA conformance, the sidebar side, platform preferences, and the text size, contrast, target size, focus, and keyboard reach of every control.

### Modified Capabilities

None. `openspec/specs/` holds no specs yet.

## Impact

- Code: `sharedUI/src/commonMain/.../ui/screen/` (`Rack.kt`, `GridModel.kt`, `GridCell.kt`, `CellHeader.kt`, `Toolbar.kt`, `VoicePanel.kt`, `Inspector.kt`, `EditorScreen.kt`, `ScreenModel.kt`), `ui/canvas/` (`EnvelopeDrawing.kt`, `PlotLabels.kt`, `PanZoom.kt`, `EnvelopeInteraction.kt`, `EnvelopeCanvas.kt`), `ui/theme/` (`JagFxTheme.kt`, `SynthPalette.kt`), `ui/widgets/` (`Knob.kt` deleted), `ui/i18n/` (every `Strings*.kt`), and `jvmMain/.../desktop/EditorMenuBar.kt`.
- Tests: `sharedUI/src/commonTest/.../screen/GridModelTest.kt`, `ScreenModelTest.kt`, `LaneModelTest.kt`, `i18n/StringsTest.kt`, and new tests for the axis, readout, and shared viewport math.
- Unchanged: `:core:*`, `:cli`, `:bindings`, the npm packages, the binary and JSON formats, and the reducers' patch semantics. Binary round-trip fidelity is untouched because no codec code changes.
- Docs: `README.md` screenshots and the editor section, `CHANGELOG.md` 3.0.0 entry, `llms.txt` if it describes the editor layout, and a new `docs/accessibility.md`.
- Saved state: a new preferences file (desktop) or `localStorage` entry (web) for the panel side, text size, and theme, through a `Preferences` service in `ui/editor/EditorServices.kt`.
