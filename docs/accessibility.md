# Accessibility Statement

This statement covers the JagFx 3.0.0 editor on desktop (`:desktopApp`) and in the browser (`:webApp`).

## Standard

The editor targets [WCAG 2.2](https://www.w3.org/TR/WCAG22/) level AA, applied to non-web software through chapter 11 of [EN 301 549](https://accessible-eu-centre.ec.europa.eu/content-corner/news/european-accessibility-standard-en-301-549-has-been-updated-2026-09-07_en).
That level covers EN 301 549 v3.2.1 (2021), the version the European Accessibility Act currently cites, and v4.1.1 (2026), which moves to WCAG 2.2.

The project has not been audited by a third party. This statement records the maintainer's own checks.

## What The Editor Does

- **Contrast**: text reaches at least 4.5:1, and plot lines, point markers, control borders, and focus rings reach at least 3:1 in the Classic, Light, and High Contrast themes. `ContrastTest` checks each pair.
- **Text size**: body text is 12 sp, labels 11 sp, and headings 14 sp. The File menu sets the text size from 100% to 200%.
- **Targets**: every pointer target is at least 24 by 24 dp.
- **Focus**: every focusable control shows a 2 dp focus ring. Tab moves through the title bar, the voice strip, the rack row by row, and the bottom bar.
- **Keyboard**: every pointer action has a key path. Alt and the arrows move between cells, the arrows select and move envelope points, Insert and Delete add and remove points, and Ctrl or Cmd with `+` and `-` zooms. The [README](../README.md#keyboard) lists every shortcut.
- **Screen readers**: controls expose a name, a role, a value, and a state through the platform accessibility API. Each plot has a text summary, and status messages are announced.
- **Color**: active toggles also change their mark, and the selected cell has a border, so color never carries meaning alone.
- **Platform settings**: the editor follows the Windows high-contrast setting on desktop, and `prefers-contrast`, `forced-colors`, and `prefers-reduced-motion` in the browser. The editor has no animated transitions.
- **Tooltips**: tooltips also show on keyboard focus, and Escape closes them.
- **Either hand**: the File menu puts the voice strip on the left or the right edge.

## Known Gaps

| Gap | WCAG criterion | Workaround |
| --- | --- | --- |
| A rack cell is selected when the pointer goes down, not when it comes up. Selecting is not destructive, and a point drag starts from the same press. | 2.5.2 Pointer Cancellation | Selecting another cell undoes the selection. |
| The pointer cannot move into a tooltip to read it. | 1.4.13 Content on Hover or Focus | Focus the control with Tab to show its tooltip, and press Escape to close it. |
| The Graph of poles and zeros, the Transition Curve, the Frequency Response, and the Final Output have no keyboard focus of their own. | 2.1.1 Keyboard | Each plot's text summary is announced; edit the filter through the filter strip fields. |
| Plot grid lines are below 3:1. They are decorative; the axis labels carry the values. | 1.4.11 Non-text Contrast | Read values from the axis labels and the point readout. |
| The selected area fill in a plot is not held to 3:1. | 1.4.11 Non-text Contrast | The selected point also shows a marker and a readout. |
| The selected pole or zero has no state description for screen readers. | 4.1.2 Name, Role, Value | The filter strip fields show the selected pole's values. |
| In the browser, Space does not stop playback. | 2.1.1 Keyboard | Press Escape. |
| In the browser, the file dialogs have no titles, and screen reader support depends on the browser. | 2.4.2 Page Titled, 4.1.2 Name, Role, Value | Use the desktop app. |
| The desktop app does not read the platform text size, nor the high-contrast setting on macOS and Linux. | EN 301 549 11.7 User preferences | Pick High Contrast and the text size in the File menu. |

## Testing

- Unit tests cover contrast ratios, key handling, plot summaries, and status messages (`./gradlew :sharedUI:jvmTest`).
- Screenshots at 1280 by 800 and 1920 by 1080 in Main and Both modes check that nothing clips (`JAGFX_SCREENSHOT_DIR=<dir> ./gradlew :sharedUI:jvmTest --tests '*ScreenshotTest'`).
- A manual pass with VoiceOver on macOS and NVDA on Windows has not been recorded yet.

## Contact

Report an accessibility problem at [github.com/xsyetopz/JagFx/issues](https://github.com/xsyetopz/JagFx/issues). Name the platform, the assistive technology, and the control.
