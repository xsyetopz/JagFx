var __defProp = Object.defineProperty;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __hasOwnProp = Object.prototype.hasOwnProperty;
function __accessProp(key) {
  return this[key];
}
var __toCommonJS = (from) => {
  var entry = (__moduleCache ??= new WeakMap).get(from), desc;
  if (entry)
    return entry;
  entry = __defProp({}, "__esModule", { value: true });
  if (from && typeof from === "object" || typeof from === "function") {
    for (var key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(entry, key))
        __defProp(entry, key, {
          get: __accessProp.bind(from, key),
          enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable
        });
  }
  __moduleCache.set(from, entry);
  return entry;
};
var __moduleCache;
var __returnValue = (v) => v;
function __exportSetter(name, newValue) {
  this[name] = __returnValue.bind(null, newValue);
}
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, {
      get: all[name],
      enumerable: true,
      configurable: true,
      set: __exportSetter.bind(all, name)
    });
};

// src/index.ts
var exports_src = {};
__export(exports_src, {
  createJagFxWasmBackend: () => createJagFxWasmBackend
});
module.exports = __toCommonJS(exports_src);
var emptyOptions = Object.freeze({});
async function createJagFxWasmBackend(dotnetFactory) {
  if (typeof dotnetFactory !== "function") {
    throw new TypeError("dotnetFactory must be the dotnet export from _framework/dotnet.js.");
  }
  const runtime = await dotnetFactory().create();
  const config = runtime.getConfig();
  const exports2 = await runtime.getAssemblyExports(config.mainAssemblyName);
  const render = findRenderExport(exports2);
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
function findRenderExport(exports2) {
  const jagFx = getRecord(exports2, "JagFx");
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
