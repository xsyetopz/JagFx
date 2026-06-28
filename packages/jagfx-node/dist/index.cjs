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
  renderSynthToPcm: () => renderSynthToPcm
});
module.exports = __toCommonJS(exports_src);
var import_node_buffer = require("node:buffer");
var import_node_fs = require("node:fs");
var import_node_module = require("node:module");
var import_node_path = require("node:path");
var import_node_url = require("node:url");
var require2 = import_node_module.createRequire("file:///Users/krystian/CodeProjects/xsyetopz/JagFx/packages/jagfx-node/src/index.ts");
configureNativeLibrary();
var binding = require2("../build/Release/jagfx_node.node");
function renderSynthToPcm(synthData, options = {}) {
  if (!import_node_buffer.Buffer.isBuffer(synthData)) {
    throw new TypeError("synthData must be a Buffer.");
  }
  return binding.renderSynthToPcm(synthData, options);
}
function configureNativeLibrary() {
  if (process.env["JAGFX_NATIVE_LIB"]) {
    return;
  }
  const platform = process.platform === "darwin" ? "darwin" : process.platform;
  const extension = process.platform === "win32" ? "dll" : process.platform === "darwin" ? "dylib" : "so";
  const packageDir = import_node_path.resolve(import_node_path.dirname(import_node_url.fileURLToPath("file:///Users/krystian/CodeProjects/xsyetopz/JagFx/packages/jagfx-node/src/index.ts")), "..");
  const candidate = import_node_path.resolve(packageDir, "prebuilds", `${platform}-${process.arch}`, `jagfx.${extension}`);
  if (import_node_fs.existsSync(candidate)) {
    process.env["JAGFX_NATIVE_LIB"] = candidate;
  }
}
