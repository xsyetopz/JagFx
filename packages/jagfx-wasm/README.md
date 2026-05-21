# @xsyetopz/jagfx-wasm

Browser WebAssembly bindings for JagFx.

This package loads the `JagFx.Wasm` .NET WebAssembly runtime and calls its `[JSExport]` sound-engine entrypoint.

Build the browser artifact from the repository root:

```sh
dotnet publish src/JagFx.Wasm -c Release
```

Then serve the publish output and pass the generated `_framework/dotnet.js` `dotnet` export into `createJagFxWasmBackend`.
