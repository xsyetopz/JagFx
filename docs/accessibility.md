# Accessibility Statement

This statement covers the JagFx 3.0.0 editor on desktop (`:desktopApp`) and in the browser (`:webApp`).

## Standard

The editor targets [WCAG 2.2](https://www.w3.org/TR/WCAG22/) level AA, applied to non-web software through chapter 11 of [EN 301 549](https://accessible-eu-centre.ec.europa.eu/content-corner/news/european-accessibility-standard-en-301-549-has-been-updated-2026-09-07_en).
That level covers EN 301 549 v3.2.1 (2021), the version the European Accessibility Act currently cites, and v4.1.1 (2026), which moves to WCAG 2.2.

The project has not been audited by a third party. This statement records the maintainer's own checks.

## What The Editor Does

- Contrast: text reaches at least 4.5:1, and plot lines, point markers, control borders, and focus rings reach at least 3:1 in the Classic, Light, and High Contrast themes. `ContrastTest` checks each pair.
- Text size: the Material 3 small body (12 sp), small label (11 sp), and small title (14 sp) styles carry the text. The File menu sets the text size from 100% to 200%.
- Targets: every pointer target is at least 24 by 24 dp. The editor sets the Material minimum interactive size to 24 dp instead of 48 dp, to keep the rack dense; the controls are stock Material 3 widgets with compact padding.
- Focus: every focusable control shows a focus indicator: the stock Material one on buttons, fields, and menus, and a 2 dp ring on the plots, which take focus with Tab or a click. Tab moves through the title bar, the voice strip, the rack row by row, and the bottom bar.
- Keyboard: every pointer action has a key path. Alt and the arrows move between cells, the arrows select and move envelope points, Insert and Delete add and remove points, and Ctrl or Cmd with `+` and `-` zooms. The [README](../README.md#keyboard) lists every shortcut.
- Screen readers: controls expose a name, a role, a value, and a state through the platform accessibility API. Each plot has a text summary, and status messages are announced.
- Color: active toggles also change their mark, and the selected cell has a border, so color never carries meaning alone.
- Platform settings: on desktop the editor follows the Windows high-contrast and Make text bigger settings (`TextScaleFactor` under `HKCU\Software\Microsoft\Accessibility`), the macOS Increase Contrast and Reduce Motion settings (`defaults read`), the GNOME high-contrast, animations, and text-scaling-factor settings (`gsettings get`), and the KDE Plasma animation speed and general font size (`kreadconfig6`, `AnimationDurationFactor` and `font` in `kdeglobals`; instant animations count as reduced motion, and the font size is read as a percent of Plasma's default 10 pt). Text size is clamped to 100% to 200%. Desktop values are read once at start-up, so a change shows after a restart. In the browser it follows `prefers-contrast`, `forced-colors`, and `prefers-reduced-motion`. The editor has no animated transitions; opening or closing a cell is instant.
- Playhead rate: the playhead in the Output waveform plot follows the audio clock, updating about every 10 ms on desktop and every 16 ms in the browser, or once a second when the platform asks for reduced motion. The final position is always sent before playback stops.
- Calm cells: a cell in the rack shows only its title, a faint grid, and the curve. Double-click the title to enlarge it; the enlarged cell shows axis values, grid lines, and an optional time and value label at every point, each with a toggle in its header.
- Plot grid lines are faint on purpose, to keep the screen calm, and are decorative under 1.4.11 Non-text Contrast: the curve, the enlarged cell's axis labels, and the point readout carry the values.
- Selection: a selected breakpoint or pole is marked by a ring in the `marker` color, which reaches 3:1 on both the scope and the selection fill in every theme (`ContrastTest`), so the fill itself carries no meaning.
- Tooltips: stock Material tooltips show on keyboard focus and when the pointer rests on a control. The pointer can move onto a tooltip and it stays open while hovered, and Escape closes it (1.4.13). `TipTest` checks both.
- Plots: Tab focuses each plot. On the pole and zero map, the arrows move the selection between poles and zeros, and Shift with the arrows nudges the selected one. The map announces the selected marker as a state (for example "Pole 2 of 3, end: 440 Hz, 30.5 dB").
- Pointer cancellation: a rack cell is selected when the pointer is released, and a press that ends outside the cell selects nothing (2.5.2).
- Space: in the browser, Space starts and stops playback; on desktop the menu bar already has the shortcut.
- Either hand: the File menu puts the voice strip on the left or the right edge.

## Known Gaps

| Gap | WCAG criterion | Workaround |
| --- | --- | --- |
| In the browser, the browser titles the file open and save dialogs. A file input takes only `accept`, `multiple`, and `required` ([HTML](https://html.spec.whatwg.org/multipage/input.html#file-upload-state-(type=file))), and the File System Access pickers take no title ([WICG](https://wicg.github.io/file-system-access/#api-filepickeroptions)), so a page cannot set one. | 2.4.2 Page Titled, 4.1.2 Name, Role, Value | Use the desktop app. |
| The desktop app does not read the macOS Text size setting (System Settings › Accessibility › Display), any KDE Plasma contrast setting, or settings on Linux desktops other than GNOME and Plasma. It reads settings once at start-up. | EN 301 549 11.7 User preferences | Pick High Contrast and the text size in the File menu, or restart the app. |

## Testing

- Unit tests cover contrast ratios, key handling, plot summaries, and status messages (`./gradlew :sharedUI:jvmTest`).
- Screenshots at 960 by 600, 1280 by 800, and 1920 by 1080, in the Envelopes and All views, each with and without an enlarged cell, check that nothing clips (`JAGFX_SCREENSHOT_DIR=<dir> ./gradlew :sharedUI:jvmTest --tests '*ScreenshotTest'`).
- A manual pass with VoiceOver on macOS and NVDA on Windows has not been recorded yet.

## Contact

Report an accessibility problem at [github.com/xsyetopz/JagFx/issues](https://github.com/xsyetopz/JagFx/issues). Name the platform, the assistive technology, and the control.
