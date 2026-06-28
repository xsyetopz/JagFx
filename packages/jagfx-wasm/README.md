# @xsyetopz/jagfx-wasm

Browser WebAssembly bindings for JagFx.

This package loads the `JagFx.Wasm` .NET WebAssembly runtime and calls its `[JSExport]` sound-engine entrypoint.

Build the browser artifact from the repository root:

```sh
dotnet publish src/JagFx.Wasm -c Release
```

Then serve the publish output and pass the generated `_framework/dotnet.js` `dotnet` export into `createJagFxWasmBackend`.

## Playground

Use the local playground to exercise the JavaScript wrapper against the real
`JagFx.Wasm` browser runtime:

```sh
bun --cwd=packages/jagfx-wasm run build:wasm
bun --cwd=packages/jagfx-wasm run playground
```

Open <http://127.0.0.1:8787/>. The page can render an uploaded `.synth` file or
`references/synths/cow_death.synth`, play the PCM output, and export a WAV.
