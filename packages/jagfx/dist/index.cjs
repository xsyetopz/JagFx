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
  writePatchJson: () => writePatchJson,
  waveforms: () => waveforms,
  synthToJson: () => synthToJson,
  renderSynthToWav: () => renderSynthToWav,
  renderPatchToWav: () => renderPatchToWav,
  readPatchJson: () => readPatchJson,
  jsonToSynth: () => jsonToSynth,
  isGitLfsPointer: () => isGitLfsPointer,
  createVoice: () => createVoice,
  createSegment: () => createSegment,
  createPatch: () => createPatch,
  createPartial: () => createPartial,
  createLoop: () => createLoop,
  createLfo: () => createLfo,
  createEnvelope: () => createEnvelope,
  createEcho: () => createEcho,
  assertRealSynthData: () => assertRealSynthData,
  JagFxError: () => JagFxError
});
module.exports = __toCommonJS(exports_src);
var import_node_buffer = require("node:buffer");
var import_node_child_process = require("node:child_process");
var import_promises = require("node:fs/promises");
var import_node_os = require("node:os");
var import_node_path = require("node:path");
var import_node_url = require("node:url");
var gitLfsPrefix = "version https://git-lfs.github.com/spec/";
var gitLfsPrefixBytes = import_node_buffer.Buffer.from(gitLfsPrefix, "ascii");
var emptyArray = Object.freeze([]);
var emptyOptions = Object.freeze({});
var waveforms = Object.freeze([
  "off",
  "square",
  "sine",
  "saw",
  "noise"
]);

class JagFxError extends Error {
  code;
  constructor(message, code = "JAGFX_ERROR") {
    super(message);
    this.name = "JagFxError";
    this.code = code;
  }
}
function isGitLfsPointer(data) {
  if (!(data instanceof Uint8Array) || data.byteLength < gitLfsPrefixBytes.length) {
    return false;
  }
  for (let i = 0;i < gitLfsPrefixBytes.length; i++) {
    if (data[i] !== gitLfsPrefixBytes[i]) {
      return false;
    }
  }
  return true;
}
function assertRealSynthData(data) {
  if (isGitLfsPointer(data)) {
    throw new JagFxError("File is a Git LFS pointer, not synth data. Run `git lfs pull` in the archive repository to download the real .synth contents.", "JAGFX_GIT_LFS_POINTER");
  }
}
function createSegment(duration, targetLevel) {
  return { duration, targetLevel };
}
function createEnvelope(envelope = emptyOptions) {
  const waveform = envelope.waveform ?? "off";
  assertWaveform(waveform);
  return {
    waveform,
    startValue: envelope.startValue ?? 0,
    endValue: envelope.endValue ?? 0,
    segments: cloneSegments(envelope.segments ?? emptyArray)
  };
}
function createLfo(rateEnvelope, modulationDepth) {
  return {
    rateEnvelope: cloneEnvelope(rateEnvelope),
    modulationDepth: cloneEnvelope(modulationDepth)
  };
}
function createPartial({
  amplitude,
  pitchOffsetSemitones = 0,
  delay = 0
}) {
  if (!Number.isInteger(amplitude) || amplitude < 1) {
    throw new JagFxError("partial amplitude must be a positive integer.", "JAGFX_INVALID_ARGUMENT");
  }
  return { amplitude, pitchOffsetSemitones, delay };
}
function createEcho(echo = emptyOptions) {
  return {
    delayMilliseconds: echo.delayMilliseconds ?? 0,
    feedbackPercent: echo.feedbackPercent ?? 0
  };
}
function createLoop(beginMs = 0, endMs = 0) {
  return { beginMs, endMs };
}
function createVoice(voiceInput) {
  const {
    frequencyEnvelope,
    amplitudeEnvelope,
    durationMs,
    offsetMs = 0,
    pitchLfo,
    amplitudeLfo,
    gapOffEnvelope,
    gapOnEnvelope,
    partials = emptyArray,
    echo,
    filter
  } = voiceInput;
  if (!frequencyEnvelope || !amplitudeEnvelope) {
    throw new JagFxError("frequencyEnvelope and amplitudeEnvelope are required.", "JAGFX_INVALID_ARGUMENT");
  }
  if (!Number.isInteger(durationMs)) {
    throw new JagFxError("durationMs is required.", "JAGFX_INVALID_ARGUMENT");
  }
  const voice = {
    frequencyEnvelope: cloneEnvelope(frequencyEnvelope),
    amplitudeEnvelope: cloneEnvelope(amplitudeEnvelope),
    durationMs
  };
  if (offsetMs !== 0)
    voice.offsetMs = offsetMs;
  if (pitchLfo)
    voice.pitchLfo = cloneLfo(pitchLfo);
  if (amplitudeLfo)
    voice.amplitudeLfo = cloneLfo(amplitudeLfo);
  if (gapOffEnvelope)
    voice.gapOffEnvelope = cloneEnvelope(gapOffEnvelope);
  if (gapOnEnvelope)
    voice.gapOnEnvelope = cloneEnvelope(gapOnEnvelope);
  if (partials && partials.length > 0)
    voice.partials = clonePartials(partials);
  if (echo)
    voice.echo = cloneEcho(echo);
  if (filter)
    voice.filter = cloneFilter(filter);
  return voice;
}
function createPatch(patchInput = emptyOptions) {
  const voices = patchInput.voices ?? emptyArray;
  if (voices.length > 10) {
    throw new JagFxError("patches can contain at most 10 voice slots.", "JAGFX_INVALID_ARGUMENT");
  }
  const voiceCount = voices.length;
  const clonedVoices = new Array(voiceCount);
  for (let i = 0;i < voiceCount; i++) {
    const voice = voices[i];
    clonedVoices[i] = voice == null ? null : cloneVoice(voice);
  }
  const patch = { voices: clonedVoices };
  const loop = patchInput.loop;
  if (loop && (loop.beginMs !== 0 || loop.endMs !== 0)) {
    patch.loop = cloneLoop(loop);
  }
  return patch;
}
async function readPatchJson(path) {
  return JSON.parse(await import_promises.readFile(path, "utf8"));
}
async function writePatchJson(path, patch) {
  await import_promises.writeFile(path, `${JSON.stringify(patch, null, 2)}
`);
}
async function synthToJson(inputPath, options = emptyOptions) {
  if (!inputPath) {
    throw new JagFxError("inputPath is required.", "JAGFX_INVALID_ARGUMENT");
  }
  const synthData = await import_promises.readFile(inputPath);
  assertRealSynthData(synthData);
  const command = resolveCommand(options.command);
  if (options.outputPath) {
    await runCommand(command.command, [...command.args, "to-json", inputPath, options.outputPath], options.cwd);
    return readPatchJson(options.outputPath);
  }
  const { stdout } = await runCommand(command.command, [...command.args, "to-json", inputPath], options.cwd);
  return JSON.parse(stdout);
}
async function jsonToSynth(patchOrPath, outputPath, options = emptyOptions) {
  if (!patchOrPath || !outputPath) {
    throw new JagFxError("patchOrPath and outputPath are required.", "JAGFX_INVALID_ARGUMENT");
  }
  const command = resolveCommand(options.command);
  if (typeof patchOrPath === "string") {
    await runCommand(command.command, [...command.args, "from-json", patchOrPath, outputPath], options.cwd);
    return;
  }
  await withTempJson(patchOrPath, async (jsonPath) => {
    await runCommand(command.command, [...command.args, "from-json", jsonPath, outputPath], options.cwd);
  });
}
async function renderPatchToWav(patchOrPath, outputPath, options = emptyOptions) {
  const dir = await import_promises.mkdtemp(import_node_path.join(import_node_os.tmpdir(), "jagfx-patch-"));
  const synthPath = import_node_path.join(dir, "patch.synth");
  try {
    await jsonToSynth(patchOrPath, synthPath, options);
    await renderSynthToWav(synthPath, outputPath, options);
  } finally {
    await import_promises.rm(dir, { recursive: true, force: true });
  }
}
async function renderSynthToWav(inputPath, outputPath, options = emptyOptions) {
  if (!inputPath || !outputPath) {
    throw new JagFxError("inputPath and outputPath are required.", "JAGFX_INVALID_ARGUMENT");
  }
  const synthData = await import_promises.readFile(inputPath);
  assertRealSynthData(synthData);
  const optionLoops = options.loops;
  const loops = typeof optionLoops === "number" && Number.isInteger(optionLoops) ? optionLoops : 1;
  if (loops < 1) {
    throw new JagFxError("loops must be a positive integer.", "JAGFX_INVALID_ARGUMENT");
  }
  const command = resolveCommand(options.command);
  await runCommand(command.command, [...command.args, inputPath, outputPath, String(loops)], options.cwd);
}
function assertWaveform(waveform) {
  switch (waveform) {
    case "off":
    case "square":
    case "sine":
    case "saw":
    case "noise":
      return;
    default:
      throw new JagFxError(`unknown waveform: ${waveform}`, "JAGFX_INVALID_ARGUMENT");
  }
}
function cloneSegments(segments) {
  const segmentCount = segments.length;
  const cloned = new Array(segmentCount);
  for (let i = 0;i < segmentCount; i++) {
    const segment = segments[i];
    cloned[i] = {
      duration: segment.duration,
      targetLevel: segment.targetLevel
    };
  }
  return cloned;
}
function cloneEnvelope(envelope) {
  return createEnvelope(envelope);
}
function cloneLfo(lfo) {
  return createLfo(lfo.rateEnvelope, lfo.modulationDepth);
}
function cloneVoice(voice) {
  return createVoice(voice);
}
function clonePartials(partials) {
  const partialCount = partials.length;
  const cloned = new Array(partialCount);
  for (let i = 0;i < partialCount; i++) {
    cloned[i] = clonePartial(partials[i]);
  }
  return cloned;
}
function clonePartial(partial) {
  const cloned = {
    amplitude: partial.amplitude,
    pitchOffsetSemitones: partial.pitchOffsetSemitones
  };
  if (partial.delay !== undefined) {
    cloned.delay = partial.delay;
  }
  return cloned;
}
function cloneEcho(echo) {
  return {
    delayMilliseconds: echo.delayMilliseconds,
    feedbackPercent: echo.feedbackPercent
  };
}
function cloneLoop(loop) {
  return {
    beginMs: loop.beginMs,
    endMs: loop.endMs
  };
}
function cloneFilter(filter) {
  const cloned = {
    poleCounts: cloneNumberPair(filter.poleCounts),
    unityGain: cloneNumberPair(filter.unityGain),
    polePhase: cloneFilterCoefficients(filter.polePhase),
    poleMagnitude: cloneFilterCoefficients(filter.poleMagnitude)
  };
  if ("modulationEnvelope" in filter) {
    cloned.modulationEnvelope = filter.modulationEnvelope == null ? null : cloneEnvelope(filter.modulationEnvelope);
  }
  return cloned;
}
function cloneFilterCoefficients(coefficients) {
  return {
    feedforward: cloneFilterPhases(coefficients.feedforward),
    feedback: cloneFilterPhases(coefficients.feedback)
  };
}
function cloneFilterPhases(phases) {
  return {
    baseline: cloneNumberArray(phases.baseline),
    modulated: cloneNumberArray(phases.modulated)
  };
}
function cloneNumberPair(values) {
  return [values[0], values[1]];
}
function cloneNumberArray(values) {
  const valueCount = values.length;
  const cloned = new Array(valueCount);
  for (let i = 0;i < valueCount; i++) {
    cloned[i] = values[i];
  }
  return cloned;
}
async function withTempJson(patch, callback) {
  const dir = await import_promises.mkdtemp(import_node_path.join(import_node_os.tmpdir(), "jagfx-json-"));
  const jsonPath = import_node_path.join(dir, "patch.json");
  try {
    await writePatchJson(jsonPath, patch);
    return await callback(jsonPath);
  } finally {
    await import_promises.rm(dir, { recursive: true, force: true });
  }
}
function resolveCommand(command) {
  if (typeof command === "string" && command.length > 0) {
    return { command, args: [] };
  }
  if (typeof command === "object" && command !== null && typeof command.command === "string") {
    return { command: command.command, args: cloneCommandArgs(command.args) };
  }
  const envCommand = process.env["JAGFX_CLI"];
  if (envCommand) {
    return { command: envCommand, args: [] };
  }
  const packageDir = import_node_path.dirname(import_node_url.fileURLToPath("file:///Users/krystian/CodeProjects/xsyetopz/JagFx/packages/jagfx/src/index.ts"));
  const repoRoot = import_node_path.resolve(packageDir, "../../..");
  return {
    command: "dotnet",
    args: [
      "run",
      "--no-restore",
      "--project",
      import_node_path.resolve(repoRoot, "src/JagFx.Cli"),
      "--"
    ]
  };
}
function cloneCommandArgs(args) {
  if (!args || args.length === 0) {
    return [];
  }
  const cloned = new Array(args.length);
  for (let i = 0;i < args.length; i++) {
    cloned[i] = args[i];
  }
  return cloned;
}
function runCommand(command, args, cwd) {
  return new Promise((resolvePromise, reject) => {
    const child = import_node_child_process.spawn(command, args, {
      cwd,
      stdio: ["ignore", "pipe", "pipe"]
    });
    const stdoutChunks = [];
    const stderrChunks = [];
    let stdoutLength = 0;
    let stderrLength = 0;
    child.stdout.on("data", (chunk) => {
      stdoutChunks.push(chunk);
      stdoutLength += chunk.byteLength;
    });
    child.stderr.on("data", (chunk) => {
      stderrChunks.push(chunk);
      stderrLength += chunk.byteLength;
    });
    child.on("error", (error) => {
      reject(new JagFxError(error.message, "JAGFX_COMMAND_FAILED"));
    });
    child.on("close", (code) => {
      const stdout = decodeChunks(stdoutChunks, stdoutLength);
      const stderr = decodeChunks(stderrChunks, stderrLength);
      if (code === 0) {
        resolvePromise({ stdout, stderr });
        return;
      }
      reject(new JagFxError(stderr.trim() || `JagFx command exited with status ${code}.`, "JAGFX_COMMAND_FAILED"));
    });
  });
}
function decodeChunks(chunks, byteLength) {
  return byteLength === 0 ? "" : import_node_buffer.Buffer.concat(chunks, byteLength).toString("utf8");
}
