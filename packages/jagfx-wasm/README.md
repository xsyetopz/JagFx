# @xsyetopz/jagfx-wasm

Browser WebAssembly bindings for JagFx.

This package loads the JagFx Kotlin/Wasm engine from `dist/kotlin/` and renders
`.synth` files to 16-bit little-endian mono PCM at 22050 Hz. The runtime needs
WebAssembly GC support, which current Chrome, Firefox, Safari, Node, and Bun have.

```js
import { createJagFxWasmBackend } from "@xsyetopz/jagfx-wasm";

const backend = await createJagFxWasmBackend();
const pcm = backend.renderSynthToPcm(synthBytes, { loops: 1, voiceFilter: -1 });
```

The backend also decodes `.jagvorbis` samples.
`readSampleInfo` returns the header fields (`sampleRate`, `sampleCount`,
`loopBegin`, `loopEnd`, `pingPong`, `packetCount`), and `decodeSampleToPcm`
returns the decoded 16-bit little-endian mono PCM at the sample's rate, without
looping.

```js
const { sampleRate } = backend.readSampleInfo(sampleBytes);
const samplePcm = backend.decodeSampleToPcm(sampleBytes);
```

`createJagFxWasmBackend` imports `dist/kotlin/jagfx-bindings.mjs`, which fetches
`jagfx-bindings.wasm` from the same directory. If a bundler or server moves those
files, pass a loader that imports the module from its new location:

```js
const backend = await createJagFxWasmBackend(
  () => import("/assets/jagfx/jagfx-bindings.mjs"),
);
```

The package is ESM only, because the Wasm module uses top-level `await`.

## Building

After changing the Kotlin code, rebuild `dist/` from this directory:

```sh
bun run build:kotlin && bun run build
```

## Playground

Use the local playground to exercise the package in a browser:

```sh
bun --cwd=packages/jagfx-wasm run build:kotlin
bun --cwd=packages/jagfx-wasm run playground
```

Open <http://127.0.0.1:8787/>. The page can render an uploaded `.synth` file or
`references/synths/cow_death.synth`, play the PCM output, and export a WAV.
