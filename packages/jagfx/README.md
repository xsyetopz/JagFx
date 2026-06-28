# @xsyetopz/jagfx

JavaScript and TypeScript helpers for JagFx synth patches.

The package wraps the JagFx CLI and exposes small builders for the JSON patch
format. Use it from Node or Bun scripts when you want to generate a patch, convert
`.synth` files to JSON, or render WAV files.

```js
import {
  createEnvelope,
  createPatch,
  createVoice,
  renderPatchToWav,
  synthToJson,
} from "@xsyetopz/jagfx";

const cow = await synthToJson("references/synths/cow_death.synth");

const patch = createPatch({
  voices: [
    createVoice({
      frequencyEnvelope: createEnvelope({
        waveform: "sine",
        startValue: 32768,
        endValue: 32768,
      }),
      amplitudeEnvelope: createEnvelope({
        waveform: "off",
        startValue: 0,
        endValue: 65535,
        segments: [{ duration: 65535, targetLevel: 0 }],
      }),
      durationMs: 500,
    }),
  ],
});

await renderPatchToWav(patch, "preview.wav");
```

Set `JAGFX_CLI` to use an installed CLI binary. In a JagFx checkout, the package
falls back to `dotnet run --project src/JagFx.Cli --`.

```sh
JAGFX_CLI=/path/to/JagFx.Cli bun app.mjs
```

If a `.synth` path points at a Git LFS pointer, the package throws before calling
JagFx. Run `git lfs pull` in the repository that owns the synth files.
