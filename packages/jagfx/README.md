# @xsyetopz/jagfx

JavaScript and TypeScript bindings for JagFx.

This package currently provides a dependency-free Node backend that calls the JagFx CLI. It ships both ESM and CommonJS entrypoints plus TypeScript declarations.

```js
import { renderSynthToWav } from "@xsyetopz/jagfx";

await renderSynthToWav("cow_death.synth", "cow_death.wav");
```

By default, the package uses `JAGFX_CLI` when set. In a JagFx source checkout, it falls back to `dotnet run --project src/JagFx.Cli --`.

```sh
JAGFX_CLI=/path/to/JagFx.Cli node app.mjs
```

If a `.synth` file is a Git LFS pointer instead of real synth bytes, the package throws a clear error before invoking JagFx.
