import assert from "node:assert/strict";
import test from "node:test";
import { createJagFxWasmBackend } from "../dist/index.mjs";

test("renders PCM through the JagFx.Wasm JSExport runtime", async () => {
	const backend = await createJagFxWasmBackend(() => ({
		async create() {
			return {
				getConfig() {
					return { mainAssemblyName: "JagFx.Wasm" };
				},
				async getAssemblyExports() {
					return {
						JagFx: {
							Wasm: {
								JagFxWasmExports: {
									RenderPcm16Le(data, loops, voiceFilter) {
										assert.deepEqual([...data], [9, 8, 7]);
										assert.equal(loops, 2);
										assert.equal(voiceFilter, -1);
										return new Uint8Array([1, 2, 3, 4]);
									},
								},
							},
						},
					};
				},
			};
		},
	}));

	const pcm = backend.renderSynthToPcm(new Uint8Array([9, 8, 7]), { loops: 2 });
	assert.deepEqual([...pcm], [1, 2, 3, 4]);
});
