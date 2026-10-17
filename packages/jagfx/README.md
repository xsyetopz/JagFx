# @xsyetopz/jagfx

JavaScript and TypeScript helpers for JagFx synth patches.

The package converts and renders in-process through the JagFx Kotlin/JS engine
and exposes small builders for the JSON patch format. Use it from Node or Bun scripts when you want to generate a patch, convert
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

The package also converts the other sound-effect files.
`toneToJson` and `jsonToTone` convert a `.tone` file (one voice) to and from the
`voice` JSON form, and `filterToJson` and `jsonToFilter` convert a `.filter` file
to and from the `filter` JSON form, where `null` means no filter.
`readSampleInfo` reads the header of a `.jagvorbis` sample, and `sampleToWav`
decodes the sample to a 16-bit WAV file at its own rate, without looping.

```js
import { readSampleInfo, sampleToWav, toneToJson } from "@xsyetopz/jagfx";

const voice = await toneToJson("voice.tone");
const { sampleRate, loopBegin, loopEnd, pingPong } = await readSampleInfo("11102.jagvorbis");
await sampleToWav("11102.jagvorbis", "11102.wav");
```

These functions always use the in-process engine and ignore `JAGFX_CLI`.

To run an installed JagFx CLI instead, set `JAGFX_CLI` or pass the `command`
option.

```sh
JAGFX_CLI=/path/to/jagfx bun app.mjs
```

Failures carry a `code`: `JAGFX_INVALID_ARGUMENT` for bad arguments,
`JAGFX_CONVERSION_FAILED` when the in-process engine rejects the input, and
`JAGFX_COMMAND_FAILED` when the CLI fails.

If a `.synth` path points at a Git LFS pointer, the package throws before calling
JagFx. Run `git lfs pull` in the repository that owns the synth files.

## Building

`dist/` bundles the Kotlin/JS library. After changing the Kotlin code, run:

```sh
bun run build:kotlin && bun run build
```
