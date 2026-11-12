/** Options for `renderSynthToPcm`. */
export interface RenderSynthToPcmOptions {
	loops?: number;
	voiceFilter?: number;
}

/** Header fields of a `.jagvorbis` sample. */
export interface SampleInfo {
	sampleRate: number;
	sampleCount: number;
	loopBegin: number;
	/** Sample after the loop region. */
	loopEnd: number;
	pingPong: boolean;
	packetCount: number;
}

/** Renderer and sample decoder over the Kotlin/Wasm engine. */
export interface JagFxWasmBackend {
	renderSynthToPcm: (
		data: Uint8Array,
		options?: RenderSynthToPcmOptions,
	) => Uint8Array;
	/** Decodes `.jagvorbis` sample bytes to mono signed 16-bit little-endian PCM, without looping. */
	decodeSampleToPcm: (data: Uint8Array) => Uint8Array;
	/** Reads the header fields of `.jagvorbis` sample bytes. */
	readSampleInfo: (data: Uint8Array) => SampleInfo;
}

/** The exports of the Kotlin/Wasm module `dist/kotlin/jagfx-bindings.mjs`. */
export interface JagFxWasmModule {
	renderPcm16Le: (
		synth: Int8Array,
		loops: number,
		voiceFilter: number,
	) => Int8Array;
	samplePcm16Le: (sample: Int8Array) => Int8Array;
	sampleInfo: (sample: Int8Array) => string;
}

/** Imports the Kotlin/Wasm module. */
export type JagFxWasmLoader = () => Promise<JagFxWasmModule>;

const emptyOptions: RenderSynthToPcmOptions = Object.freeze({});

const toInt8Array = (data: Uint8Array): Int8Array => {
	if (!(data instanceof Uint8Array)) {
		throw new TypeError("data must be a Uint8Array.");
	}
	return new Int8Array(data.buffer, data.byteOffset, data.byteLength);
};

const moduleExportNames = [
	"renderPcm16Le",
	"samplePcm16Le",
	"sampleInfo",
] as const;

const sampleInfoKeys = [
	"sampleRate",
	"sampleCount",
	"loopBegin",
	"loopEnd",
	"pingPong",
	"packetCount",
] as const;

const isJagFxWasmModule = (value: unknown): value is JagFxWasmModule =>
	typeof value === "object" &&
	value !== null &&
	moduleExportNames.every((name) => typeof Reflect.get(value, name) === "function");

const isSampleInfo = (value: unknown): value is SampleInfo =>
	typeof value === "object" &&
	value !== null &&
	sampleInfoKeys.every((key) => key in value);

const loadBundledModule: JagFxWasmLoader = async () => {
	// A runtime URL keeps the bundler from inlining the module and its top-level await.
	const url = new URL("kotlin/jagfx-bindings.mjs", import.meta.url).href;
	const loaded: unknown = await import(url);
	if (!isJagFxWasmModule(loaded)) {
		throw new TypeError("The JagFx Wasm module has unexpected exports.");
	}
	return loaded;
};

const assertModuleExports = (module: JagFxWasmModule): void => {
	for (const name of moduleExportNames) {
		if (typeof module?.[name] !== "function") {
			throw new TypeError(`The JagFx Wasm module does not export ${name}.`);
		}
	}
};

/**
 * Loads the Kotlin/Wasm engine and returns a renderer over it.
 *
 * The default loader imports `kotlin/jagfx-bindings.mjs` next to this module, which fetches
 * `jagfx-bindings.wasm` from the same directory. Pass a loader when a bundler or server moves
 * those files. The runtime needs WebAssembly GC.
 */
export const createJagFxWasmBackend = async (
	loader: JagFxWasmLoader = loadBundledModule,
): Promise<JagFxWasmBackend> => {
	if (typeof loader !== "function") {
		throw new TypeError(
			"loader must be a function that imports jagfx-bindings.mjs.",
		);
	}

	const module = await loader();
	assertModuleExports(module);

	return {
		renderSynthToPcm: (
			data: Uint8Array,
			options: RenderSynthToPcmOptions = emptyOptions,
		): Uint8Array => {
			if (!(data instanceof Uint8Array)) {
				throw new TypeError("data must be a Uint8Array.");
			}

			const optionLoops = options.loops;
			const optionVoiceFilter = options.voiceFilter;
			const loops =
				typeof optionLoops === "number" && Number.isInteger(optionLoops)
					? optionLoops
					: 1;
			const voiceFilter =
				typeof optionVoiceFilter === "number" &&
				Number.isInteger(optionVoiceFilter)
					? optionVoiceFilter
					: -1;
			const pcm = module.renderPcm16Le(toInt8Array(data), loops, voiceFilter);
			return new Uint8Array(pcm.buffer, pcm.byteOffset, pcm.byteLength);
		},
		decodeSampleToPcm: (data: Uint8Array): Uint8Array => {
			const pcm = module.samplePcm16Le(toInt8Array(data));
			return new Uint8Array(pcm.buffer, pcm.byteOffset, pcm.byteLength);
		},
		readSampleInfo: (data: Uint8Array): SampleInfo => {
			const info: unknown = JSON.parse(module.sampleInfo(toInt8Array(data)));
			if (!isSampleInfo(info)) {
				throw new TypeError("The JagFx Wasm module returned malformed sample info.");
			}
			return info;
		},
	};
};
