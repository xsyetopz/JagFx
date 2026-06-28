import { describe, expect, test } from "bun:test";
import { createJagFxWasmBackend } from "../dist/index.mjs";

describe("createJagFxWasmBackend", () => {
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
											expect([...data]).toEqual([9, 8, 7]);
											expect(loops).toBe(2);
											expect(voiceFilter).toBe(-1);
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

		const pcm = backend.renderSynthToPcm(new Uint8Array([9, 8, 7]), {
			loops: 2,
		});
		expect([...pcm]).toEqual([1, 2, 3, 4]);
	});
});
