# JagFx WASM playground

A small local browser harness for exercising `@xsyetopz/jagfx-wasm` against the real
`JagFx.Wasm` .NET WebAssembly publish output.

From the repository root:

```sh
bun --cwd=packages/jagfx-wasm run build:wasm
bun --cwd=packages/jagfx-wasm run playground
```

Then open <http://127.0.0.1:8787/>. The page can load a local `.synth` file or the
bundled `references/synths/cow_death.synth`, render it through the JSExport bridge,
play the PCM in the browser, and download a WAV preview.

If you publish the WASM app somewhere else, point the server at that `wwwroot`:

```sh
JAGFX_WASM_WWWROOT=/path/to/wwwroot bun --cwd=packages/jagfx-wasm run playground
```
