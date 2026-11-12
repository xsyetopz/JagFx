## Context

JagFx 3.0.0 is a Kotlin Multiplatform rewrite. The codec, engine, CLI, and bindings are done. The editor (`:sharedUI`, shown by `:desktopApp` and `:webApp`) works, but its UI and UX need rework.

### The original, from the references

Nobody on this project can run the in-house JagFx. Its look is known only from `references/`:

- `references/videos/youtube_Alanwalkermusic_UToQEpFqHKo.mp4` (2025, Windows 11, dark theme). This is the latest version and the primary reference. The user named it as the best reference.
- `references/images/twitter_JagexIanT_920674521304260609.jpeg` (2017, light grey Windows 7 theme), the sharpest image of the layout.
- Four more videos from 2019 to 2022 show the same layout in Main and Both modes.

Every source shows the same grid. The 2025 frame shows it in Both mode:

| Row | Base | module strip | Modulation rate | Modulation range | filter column |
| --- | --- | --- | --- | --- | --- |
| Freq | Freq Base | waveform, Min Hz, Max Hz, Av Hz | Freq Modulation rate | Freq Modulation range | Graph of poles and zeros |
| Amp | Amp Base | waveform, Min, Max, Av | Amp Modulation rate | Amp Modulation range | Transition Curve |
| Output | Final Output | Gap Off/On, Min Gap, Max Gap, Av Gap | Gap off | Gap on | Frequency Response |

- The left strip holds the oscillator waveform (Off, Sqr, Sin, Saw, Noise), Min Hz, Max Hz, Av Hz, Len ms, Pos ms, L1 ms, L2 ms, Play, Play All, Loop, Loop All, Loops, Stop, Load, Save, and a green True Wave toggle.
- The bottom row holds the tone selector 1 to 10 ("Editing Tone" in 2017, "Tones" in 2025) with Copy and Reset Active, five Harmonic boxes (Semi, Vol, Del), a Reverb box (vol, del), and the Mode switch (Main, Filter, Both).
- The filter column has a filter Off/On toggle and Import Filter and Export Filter buttons, left of the pole and zero graph (2025 frame).

### Tone and filter files

The Import Filter and Export Filter buttons show that the original saved `.filter` files. A Mod Surma video shows `.tone` files in a file explorer, as the user reports. Nobody outside Jagex has the bytes of either kind. The only files anyone can get are `.synth` files from the OSRS cache.

3.0.0 already reads and writes both kinds: `.tone` holds one voice slot as it appears in a `.synth` file, and `.filter` holds a voice's filter field (`EditorServices.kt:78-85`, `ToneFilterTransfer.kt`). `docs/synth-format-spec.md` §5.1 states that layout as fact. It is JagFx's own layout, a plausible guess, and nothing confirms it matches the original's.
- The 2025 theme: dark grey chrome, black plots, pale yellow-green envelope lines with point markers, a pale green output wave, and green active toggles. The pole and zero graph, the transition curve, and the frequency response draw a grid; the main envelope cells draw only a midline. The colors come from a camera recording, so they are approximate.

### The cell header buttons

The user reported this grouping, and the 2017 image and the 2025 frame confirm it:

- `[1x][2x][4x][S][M…][▼]`: Freq Base, Freq Modulation rate, Freq Modulation range, Amp Base, Amp Modulation rate, Amp Modulation range, Transition Curve, Final Output, Gap off, and Gap on.
- `[1x][2x][4x][S]`: Graph of poles and zeros, and Frequency Response.

What follows from that, as inference:

- `1x 2x 4x S` are view controls that every plot has. `1x`, `2x`, and `4x` are zoom levels. `S` is narrower and unlabeled beyond one letter. On the pole and zero graph it could be Snap; on the other plots it could be Scroll or a fit-to-view. The references never show one of them pressed.
- `M…` and `▼` appear only on plots with a time axis, including the read-only Final Output. So they act on time or on the cell's own controls, not on point editing.
- In the 2017 image, `M…` has the dark text of a selector and `▼` sits right after it. Together they look like a Swing combo box whose selected text is cut to `M…`. Another reading, from the user, is that `▼` shows or hides that envelope's controls. No frame shows either one open, so both readings stay hypotheses.

### The current 3.0.0 screen

`EditorScreen.kt` stacks a header bar, the rack with a 240 dp inspector on the right, and a status bar.

- `GridModel.kt` puts pitch, volume, and filter in the columns, which transposes the original. The filter envelope sits in the main grid instead of the filter column.
- `Inspector.kt` holds each module's waveform, Start and End, the point table, the filter fields, the harmonics, and the echo. The user selects a cell, then looks away to the side panel.
- `Toolbar.kt` puts the transport, ten voice chips, Length, Start, the loop group, the file name, Undo and Redo, the file menu, and the panel toggle on one 36 dp row.
- `EnvelopeDrawing.kt` labels the time axis with bare numbers and leaves the value axis unlabeled. The readout in `EnvelopeCanvas.kt` shows only the raw position and level.
- Each cell keeps its own `CanvasView` zoom, although every time-domain cell shows the same voice timeline.
- `JagFxTheme.kt` sets body text at 10 sp, labels at 9 sp, controls at 18 dp, and `textSubtle` #555555 on #1E1E1E, about 2.2:1.
- The reducer handles `ViewIntent.SetTrueWave` and `SlotIntent.SelectTargetByOffset`, but no control dispatches them. `widgets/Knob.kt` has no caller.

## Goals / Non-Goals

**Goals:**

- A Jagex sound designer who used the 2025 JagFx finds every control where they remember it, under the name they remember.
- A newcomer can tell what each cell does, what unit each number is in, and how to turn a module on, without a manual.
- Every number on screen shows its unit, and the raw file value stays reachable for people who work from the format.
- The editor meets EN 301 549 chapter 11 at WCAG 2.2 AA (D12), and left- and right-handed users can each put the side panel on their mouse side (D13).
- The editor fits 1280 by 800 without clipping and uses the extra room at 1920 by 1080.

**Non-Goals:**

- No change to `:core:*`, the codec, the JSON schema, the renderer, the CLI, or the npm packages.
- No change to `docs/synth-format-spec.md`.
- No new Gradle or npm dependencies.
- No pixel copy of the original. Its fonts, bevels, and exact sizes are not targets.
- No mobile or touch layout. The web app keeps the desktop layout.

## Decisions

### D1. Use the original grid orientation

`RackRows` becomes the Freq, Amp, and Output rows across the Base, Modulation rate, and Modulation range columns. The Output row holds the output wave under Base and the gate pair in the two modulation columns.

Why: every reference from 2017 to 2025 uses this grid, so its users know it by heart. It also reads as a signal flow, with one module and its modulators per row. The transposed 3.0.0 grid offers nothing in return.

### D2. Put each module's controls in a strip beside its Base cell

Each row gets a strip between Base and the modulation columns. The Freq and Amp strips hold the waveform buttons (Off, Sqr, Sin, Saw, Noise), Min and Max with units, and a read-only average. The gate strip holds Off/On, Min Gap, Max Gap, and Av Gap. Min and Max edit the envelope's `startValue` and `endValue`, and the strip shows them in Hz, %, or ms through the slot's `ValueRange`.

Why: the control sits next to what it changes. The inspector made the user select a cell and then look away.

### D3. Replace the inspector with a per-cell controls panel

The right inspector goes. The `▼` button at the end of each time-domain cell header shows or hides that cell's controls panel, which follows the user's reading of `▼`. The panel holds the exact point table: time in ms, value in the unit, and raw `u16` position and level, all editable. It also holds the cell's view options (D6). The filter's pole counts, unity gains, and selected pole fields move to a filter strip left of the filter column, next to its Off/On, Import Filter, and Export Filter buttons, which sit by the pole and zero graph as in the 2025 frame. Import Tone and Export Tone stay in the file menu. Their tooltips call the files "JagFx tone file" and "JagFx filter file", and do not claim the original tool can read them.

Why: the exact table is for the few edits that need it, so one click away is close enough, and the scopes get back the inspector's 240 dp. Putting the panel behind `▼` keeps the original's header shape.

### D4. Add the Mode switch

`ScreenViewState` gains `mode: RackMode` with `Main`, `Filter`, and `Both`, default `Main`. Main shows the 3 by 3 rack. Filter shows the filter column at full size. Both shows the rack and the filter column side by side. The filter column holds the pole and zero graph, the transition curve (the filter envelope, `SignalChainSlot.Filter`), and the frequency response, in that order.

Why: it matches the original, it keeps the main screen calm for people who never touch the filter, and the filter envelope then sits next to the poles it moves.

### D5. Move voice and file controls to a left strip and a bottom bar

- The left strip holds the oscillator (waveform, Min Hz, Max Hz, Av Hz), the voice timing (Len ms, Pos ms), the loop (L1 ms, L2 ms), the transport (Play, Play All, Loop, Loop All, Loops, Stop), Load, Save, and True Wave.
- The bottom bar holds "Tones" 1 to 10 with the existing timeline bars, Copy, Reset Active, the five harmonics (Semi, Vol, Del), Reverb (vol, del), and the Mode switch.
- A slim title bar keeps the file name, the file menu (Open sample, Save as, Import and Export tone), Undo, and Redo.

Why: this is where the original puts them, and it empties the overfull header row. "Play" and "Loop" play the selected tone; "Play All" and "Loop All" play the patch. The 3.0.0 Solo toggle maps onto that pair, and each tooltip says which one plays what.

### D6. Keep the header buttons and give each a clear job

- Every plot keeps `[1x][2x][4x]` as zoom presets, so Jagex users find them where they expect. The scroll wheel with Ctrl or Cmd zooms around the cursor between presets, and a drag with the middle button or on the axis pans.
- `S` becomes Fit: it returns the plot to the whole voice or the whole plane. Its tooltip says so. On the pole and zero graph, Snap moves into the `▼` panel's place in the filter strip, because that plot has no `▼`.
- `M…` becomes a time-view selector on time-domain cells, with "Linked" (default) and "This cell". All linked cells share one viewport, held once in `ScreenViewState` as `timeView`, so zooming the pitch envelope zooms the output with it. The pole and zero graph and the frequency response keep their own viewports.
- Every time-domain cell draws a time axis in ms (from `positionToMs`) and a value axis in the slot's unit.

Why: the presets cost nothing and keep the header familiar. The original's `M…` sits only on time-domain plots, so a time-axis control fits it under either reading. The references show no unit other than ms, so a unit choice adds nothing.

### D7. Show units and raw values in the readout

While a point is hovered, dragged, or selected, the cell shows the time in ms, the value in its unit, and the raw `position/65536 · level`. During a drag the readout follows the point. The raw columns in the controls panel are editable.

Why: newcomers think in ms and Hz. Format authors and people who compare against the game cache think in raw values. Both get what they need without a mode switch.

### D8. Classic titles with plain descriptions

Cell titles use the original names: "Freq Base", "Freq Modulation rate", "Freq Modulation range", "Amp Base", "Amp Modulation rate", "Amp Modulation range", "Final Output", "Gap off", "Gap on", "Graph of poles and zeros", "Transition Curve", and "Frequency Response". Each title has a one-line description in its tooltip and in the empty-module prompt, for example "Freq Modulation rate: how fast the pitch wobbles (vibrato speed)."

Why: Jagex staff know the classic names, and newcomers need the descriptions. All six languages get the descriptions. The titles stay in English in every language, because they name the original tool's cells.

### D9. A Classic dark palette modeled on the 2025 theme

`SynthPalette` gains a Classic set and makes it the default dark theme: chrome dark grey, plot background black, plot grid dark grey-green, midline grey, envelope lines pale yellow-green with point markers, output wave pale green, active toggles green, filter response pale yellow-green, and poles and zeros in a separate hue. The filter-column plots draw a grid, and the main envelope cells draw a light grid behind the midline so the axes have ticks to read. The exact values are tuned during the work against WCAG contrast, because the reference is a camera recording. The light scheme stays. Color never carries meaning alone: active toggles also change their label weight or show a check, and the selected cell gets a border.

Why: light lines on black exceed 7:1 contrast, so the classic look is also the high-contrast look.

### D10. Accessibility floor

- Body text 12 sp, labels 11 sp, headings 14 sp. All text reaches at least 4.5:1, so `textSubtle` becomes at least #8A8A8A on #1E1E1E.
- Every pointer target is at least 24 by 24 dp. Small-looking buttons get a larger hit area.
- Every focusable control shows a 2 dp focus ring in the accent color.
- Tab moves through the title bar, the left strip, the rack in reading order, and the bottom bar. Arrow keys move between cells through `SlotIntent.SelectTargetByOffset`.
- In a focused envelope cell, Left and Right select the previous and next point. An arrow with Shift moves the point one step, and with Ctrl a fine step. Insert or `+` adds a point after the selected one, and Delete removes it. Every drag gets a keyboard path this way.

### D11. Guidance for empty and disabled states

- A pair cell whose module is off shows "Vibrato is off. Pick a waveform to make the pitch wobble." with the waveform buttons inline.
- A voice that is off dims the rack, shows "Tone 3 is off", and offers a Turn on button.
- The screen with no file open offers Open and New.
- The status bar reports what an action did and how to undo it.

### D12. Target EN 301 549 at WCAG 2.2 AA

The European Accessibility Act (Directive (EU) 2019/882) presumes conformance through the harmonised standard EN 301 549. The version the Official Journal cites is v3.2.1 (2021), which applies WCAG 2.1 AA to web content and, through chapter 11, to non-web software. v4.1.1, published in September 2026, moves to WCAG 2.2, drops 4.1.1 Parsing, and strengthens clause 11.7: software must follow the platform's color, contrast, font size, and focus cursor settings. Sources: [AccessibleEU](https://accessible-eu-centre.ec.europa.eu/content-corner/news/european-accessibility-standard-en-301-549-has-been-updated-2026-09-07_en), [EN 301 549](https://en.wikipedia.org/wiki/EN_301_549), [EAA](https://en.wikipedia.org/wiki/European_Accessibility_Act).

A free desktop editor from one maintainer is probably outside the EAA's listed products and services, and the Act exempts microenterprises that provide services. That reading is ours and is not legal advice. The editor targets the standard anyway, because it is the shared yardstick, and WCAG 2.2 AA satisfies both versions.

Beyond D10, this adds: 3:1 contrast for plot lines and control borders; names, roles, values, and states through the platform accessibility API; a text summary per plot; announced status messages; tooltips on focus that Escape closes; release-to-activate presses; keyboard alternatives to wheel zoom and pan; the platform's high-contrast, text scale, and reduced-motion settings; an in-app text size up to 200%; a high-contrast theme; and an accessibility statement in `docs/accessibility.md`.

Why: these are the WCAG 2.2 AA and chapter 11 items that a plot-heavy desktop editor most often fails, and each one has a concrete test.

### D13. Side panel on either edge

`EditorState` holds `panelSide: PanelSide` (Left or Right, default Right), set by `ViewIntent.SetPanelSide`. The reducer leaves the patch and the history alone. Until task 2.7 the setting moves the properties panel; after it, the voice strip. The title bar's file menu lists the other side, and the desktop View menu shows both as radio items. A `Preferences` service in `EditorServices.kt`, with one implementation per platform (a JSON file under the user's config folder on desktop, `localStorage` on the web), keeps the side, the text size, and the theme across restarts.

Why: a left-handed mouse user keeps the pointer on the left. A strip on the right makes every voice edit cross the whole rack. Keeping the choice in `EditorState` makes it one tested reducer case, and the menus only dispatch it.

## Risks / Trade-offs

- [`M…` and `▼` may mean something else in the original] → D3 and D6 give them jobs that fit their position. If a better source appears, the jobs can move without moving the buttons.
- [Linked zoom may surprise someone who zooms one cell] → `M…` switches that cell to "This cell", and Fit restores the view.
- [The classic layout needs width] → the left strip and the bottom bar replace the 240 dp inspector. Below 1280 dp the strips collapse to compact fields with tooltips. Screenshots at 1280 by 800 and 1920 by 1080 check this.
- [Larger text and targets cost density] → the scopes get the inspector's width back, and the strips use two-column grids.
- [Six languages make labels longer] → strips size to their content and wrap. `StringsTest` checks that every `UiString` has a value in every language.
- [The Wasm build may expose less to screen readers than the desktop build] → the accessibility statement lists what the web app lacks, and points to the desktop app.
- [Platform settings are not all visible to the JVM] → the editor reads what the platform exposes, such as the Windows high-contrast desktop property, and the in-app text size and theme cover the rest.
- [Moved controls break screen tests] → `GridModelTest`, `ScreenModelTest`, and `LaneModelTest` change in the same tasks as the code they test.

## Migration Plan

UI only. No file format changes. The only new saved state is the preferences file of D13. The changelog tells users of the 3.0.0 previews where each inspector control moved.

## Open Questions

- Should the cell titles be the classic names with plain descriptions (D8), or plain names with the classic name in the tooltip? D8 stands until the user picks.
- Should the time view be linked by default (D6)?
- Should the Classic palette replace the current dark theme, or sit beside it as a choice?
- Should `docs/synth-format-spec.md` §5.1 say that the `.tone` and `.filter` layouts are JagFx's own, unconfirmed against the original? This change does not edit the spec unless the user agrees.
