## 1. Screen state and models

- [x] 1.1 Add `RackMode` (Main, Filter, Both) and `mode` to `ScreenViewState`; add the shared `timeView` viewport and a per-cell "linked" flag to `CanvasView`
- [x] 1.2 Change `RackRows` in `GridModel.kt` to the Freq, Amp, and Output rows; make `FilterColumn` PoleZero, Filter (Transition Curve), and Bode; update `RackCells`
- [x] 1.3 Add the axis math: tick values in ms for a viewport and voice length, and value ticks in the slot unit from `SignalChainSlot.ValueRange`
- [x] 1.4 Add the readout model: time in ms, value in unit, raw position, and raw level of a point
- [x] 1.5 Update `GridModelTest`, `ScreenModelTest`, and `LaneModelTest`, and add tests for the axis, readout, and linked-viewport math; run `./gradlew :sharedUI:jvmTest`

## 2. Layout

- [x] 2.1 Rebuild `Rack.kt` as the 3 by 3 grid with a module strip after each Base cell, and the filter column and filter strip for Filter and Both modes
- [x] 2.2 Build the Freq and Amp module strips (waveform, Min, Max, Av with units) and the gate strip (Off, On, Min Gap, Max Gap, Av Gap)
- [x] 2.3 Build the left voice strip: oscillator, Len, Pos, L1, L2, Play, Play All, Loop, Loop All, Loops, Stop, Load, Save, and True Wave wired to `ViewIntent.SetTrueWave`
- [x] 2.4 Build the bottom bar: Tones 1 to 10 with timeline bars, Copy, Reset Active, five harmonic boxes, Reverb, and the Mode switch
- [x] 2.5 Build the filter strip: Off and On, Import Filter, Export Filter, pole counts, unity gains, selected pole Hz and dB, and Snap
- [x] 2.6 Slim `Toolbar.kt` to the title bar: file name, file menu, Undo, Redo
- [x] 2.7 Remove `Inspector.kt` and the panel toggle; move its remaining fields into the strips and the controls panel
- [x] 2.8 Delete `widgets/Knob.kt`
- [x] 2.9 Run `./gradlew :sharedUI:jvmTest`

## 3. Cells

- [x] 3.1 Rework `CellHeader.kt`: classic title with description tooltip, `1x 2x 4x S` on every plot, `M…` and `▼` on time-domain cells, each with a tooltip and an accessible name
- [x] 3.2 Wire `1x 2x 4x`, `S` (Fit), wheel zoom around the pointer, and middle-drag pan to the linked or per-cell viewport
- [x] 3.3 Draw the time axis in ms and the value axis in the unit in `EnvelopeDrawing.kt` and `PlotLabels.kt`, plus the light grid
- [x] 3.4 Show the readout on hover, drag, and selection in `EnvelopeCanvas.kt`
- [x] 3.5 Build the per-cell controls panel behind `▼`: the point table with ms, unit, raw position, and raw level fields, range errors, and the time-view option
- [x] 3.6 Extend `EnvelopeInteraction.kt` with keyboard select, move, fine move, add, and delete, each one undo step
- [x] 3.7 Run `./gradlew :sharedUI:jvmTest`

## 4. Guidance

- [ ] 4.1 Add the empty-module prompts with inline waveform buttons and the filter-off prompt
- [ ] 4.2 Add the disabled-tone prompt with Turn on
- [ ] 4.3 Add the no-file screen with Open and New
- [ ] 4.4 Make the status bar report each action and its undo, and label `.tone` and `.filter` files as JagFx files in tooltips and dialogs

## 5. Theme and accessibility

- [x] 5.1 Add the Classic dark palette to `SynthPalette` and `JagFxColorSchemes`, tuned to at least 4.5:1 text contrast and 3:1 focus contrast
- [x] 5.2 Raise `JagFxTheme` text to 12, 11, and 14 sp, and give every control a hit area of at least 24 dp
- [x] 5.3 Add a 2 dp focus ring to every focusable control and a non-color mark to active toggles
- [ ] 5.4 Set the Tab order and wire Alt and arrow keys to `SlotIntent.SelectTargetByOffset`
- [ ] 5.5 Add Play All, Loop All, and Mode shortcuts to `EditorMenuBar.kt`
- [x] 5.6 Add `PanelSide`, `EditorState.panelSide`, and `ViewIntent.SetPanelSide`; place the properties panel by it; add the file menu item and the desktop View menu; test in `EditorReducerTest`
- [x] 5.7 After task 2.7, place the voice strip by `panelSide` instead of the properties panel
- [x] 5.8 Add a `Preferences` service to `EditorServices.kt` with desktop and web implementations, and keep the panel side, text size, and theme across restarts
- [ ] 5.9 Read the platform's high-contrast, text scale, and reduced-motion settings where exposed; add the in-app text size (100% to 200%) and the high-contrast theme
- [ ] 5.10 Give plot lines, markers, borders, and focus rings at least 3:1 contrast, and add a text summary to each plot
- [ ] 5.11 Set accessible names, roles, values, and states on every control; announce status messages; show tooltips on focus and close them with Escape
- [ ] 5.12 Make presses act on release, add key alternatives to wheel zoom and pan, and let Escape stop playback
- [ ] 5.13 Check with VoiceOver on macOS and NVDA on Windows, and write `docs/accessibility.md` with the standard versions, the gaps, and the contact

## 6. Strings

- [ ] 6.1 Add every new `UiString` (descriptions, prompts, tooltips, accessible names, strip labels) to `StringsEn`, `StringsDe`, `StringsEs`, `StringsFr`, `StringsIt`, and `StringsPtBr`; remove unused keys
- [ ] 6.2 Run `StringsTest` through `./gradlew :sharedUI:jvmTest`

## 7. Docs and checks

- [ ] 7.1 Render screenshots of Main and Both at 1280 by 800 and 1920 by 1080 and check that nothing clips
- [ ] 7.2 Update the editor section and screenshots of `README.md`, the 3.0.0 entry of `CHANGELOG.md` (where each inspector control moved), and `llms.txt` if it describes the editor
- [ ] 7.3 Run `./gradlew spotlessApply` and `just lint`
- [ ] 7.4 Run `./gradlew check` and `just verify`
