// src/index.ts
var emptyOptions = Object.freeze({});
async function createJagFxWasmBackend(dotnetFactory) {
  if (typeof dotnetFactory !== "function") {
    throw new TypeError("dotnetFactory must be the dotnet export from _framework/dotnet.js.");
  }
  const runtime = await dotnetFactory().create();
  const config = runtime.getConfig();
  const exports = await runtime.getAssemblyExports(config.mainAssemblyName);
  const render = findRenderExport(exports);
  return {
    renderSynthToPcm(data, options = emptyOptions) {
      if (!(data instanceof Uint8Array)) {
        throw new TypeError("data must be a Uint8Array.");
      }
      const optionLoops = options.loops;
      const optionVoiceFilter = options.voiceFilter;
      const loops = typeof optionLoops === "number" && Number.isInteger(optionLoops) ? optionLoops : 1;
      const voiceFilter = typeof optionVoiceFilter === "number" && Number.isInteger(optionVoiceFilter) ? optionVoiceFilter : -1;
      return render(data, loops, voiceFilter);
    }
  };
}
function findRenderExport(exports) {
  const jagFx = getRecord(exports, "JagFx");
  const wasm = getRecord(jagFx, "Wasm");
  const wasmExports = getRecord(wasm, "JagFxWasmExports");
  const render = wasmExports["RenderPcm16Le"];
  if (typeof render !== "function") {
    throw new Error("JagFx.Wasm export RenderPcm16Le was not found.");
  }
  return render;
}
function getRecord(source, key) {
  const value = source[key];
  return value && typeof value === "object" ? value : {};
}
export {
  createJagFxWasmBackend
};
