export async function createJagFxWasmBackend(dotnetFactory) {
	if (typeof dotnetFactory !== "function") {
		throw new TypeError(
			"dotnetFactory must be the dotnet export from _framework/dotnet.js.",
		);
	}

	const runtime = await dotnetFactory().create();
	const config = runtime.getConfig();
	const exports = await runtime.getAssemblyExports(config.mainAssemblyName);
	const render = exports?.JagFx?.Wasm?.JagFxWasmExports?.RenderPcm16Le;
	if (typeof render !== "function") {
		throw new Error("JagFx.Wasm export RenderPcm16Le was not found.");
	}

	return {
		renderSynthToPcm(data, options = {}) {
			if (!(data instanceof Uint8Array)) {
				throw new TypeError("data must be a Uint8Array.");
			}

			const loops = Number.isInteger(options.loops) ? options.loops : 1;
			const voiceFilter = Number.isInteger(options.voiceFilter)
				? options.voiceFilter
				: -1;
			return render(data, loops, voiceFilter);
		},
	};
}
