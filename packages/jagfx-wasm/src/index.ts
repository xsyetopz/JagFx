export interface RenderSynthToPcmOptions {
	loops?: number;
	voiceFilter?: number;
}

export interface JagFxWasmBackend {
	renderSynthToPcm(
		data: Uint8Array,
		options?: RenderSynthToPcmOptions,
	): Uint8Array;
}

export type DotnetFactory = () => {
	create(): Promise<{
		getConfig(): { mainAssemblyName: string };
		getAssemblyExports(assemblyName: string): Promise<Record<string, unknown>>;
	}>;
};

type RenderPcm16Le = (
	data: Uint8Array,
	loops: number,
	voiceFilter: number,
) => Uint8Array;

const emptyOptions = Object.freeze({}) as Readonly<Record<string, never>>;

export async function createJagFxWasmBackend(
	dotnetFactory: DotnetFactory,
): Promise<JagFxWasmBackend> {
	if (typeof dotnetFactory !== "function") {
		throw new TypeError(
			"dotnetFactory must be the dotnet export from _framework/dotnet.js.",
		);
	}

	const runtime = await dotnetFactory().create();
	const config = runtime.getConfig();
	const exports = await runtime.getAssemblyExports(config.mainAssemblyName);
	const render = findRenderExport(exports);

	return {
		renderSynthToPcm(
			data: Uint8Array,
			options: RenderSynthToPcmOptions = emptyOptions,
		): Uint8Array {
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
			return render(data, loops, voiceFilter);
		},
	};
}

function findRenderExport(exports: Record<string, unknown>): RenderPcm16Le {
	const jagFx = getRecord(exports, "JagFx");
	const wasm = getRecord(jagFx, "Wasm");
	const wasmExports = getRecord(wasm, "JagFxWasmExports");
	const render = wasmExports["RenderPcm16Le"];
	if (typeof render !== "function") {
		throw new Error("JagFx.Wasm export RenderPcm16Le was not found.");
	}
	return render as RenderPcm16Le;
}

function getRecord(
	source: Record<string, unknown>,
	key: string,
): Record<string, unknown> {
	const value = source[key];
	return value && typeof value === "object"
		? (value as Record<string, unknown>)
		: {};
}
