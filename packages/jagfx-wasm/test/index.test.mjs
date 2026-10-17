import { describe, expect, test } from "bun:test";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createJagFxWasmBackend } from "../dist/index.mjs";

const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = resolve(packageDir, "../..");
const cowDeathPath = resolve(repoRoot, "references/synths/cow_death.synth");
const cowDeathWavPath = resolve(repoRoot, "references/wav/cow_death.wav");
const samplePath = resolve(repoRoot, "references/vorbis/11102.jagvorbis");
const wavHeaderBytes = 44;

describe("createJagFxWasmBackend", () => {
	test("passes bytes and options to the loaded module", async () => {
		const backend = await createJagFxWasmBackend(() =>
			Promise.resolve({
				renderPcm16Le(data, loops, voiceFilter) {
					expect(data).toBeInstanceOf(Int8Array);
					expect([...data]).toEqual([9, 8, 7]);
					expect(loops).toBe(2);
					expect(voiceFilter).toBe(-1);
					return new Int8Array([1, 2, 3, 4]);
				},
				samplePcm16Le() {
					throw new Error("unused");
				},
				sampleInfo() {
					throw new Error("unused");
				},
			}),
		);

		const pcm = backend.renderSynthToPcm(new Uint8Array([9, 8, 7]), {
			loops: 2,
		});
		expect(pcm).toBeInstanceOf(Uint8Array);
		expect([...pcm]).toEqual([1, 2, 3, 4]);
	});

	test("renders the reference PCM through Kotlin/Wasm", async () => {
		const backend = await createJagFxWasmBackend();
		const synth = new Uint8Array(await readFile(cowDeathPath));
		const wav = await readFile(cowDeathWavPath);

		const pcm = backend.renderSynthToPcm(synth);
		expect(Buffer.from(pcm)).toEqual(wav.subarray(wavHeaderBytes));
	});

	test("rejects a module without the sample exports", async () => {
		await expect(
			createJagFxWasmBackend(() =>
				Promise.resolve({ renderPcm16Le: () => new Int8Array() }),
			),
		).rejects.toThrow("does not export samplePcm16Le");
	});

	test("decodes a sample through Kotlin/Wasm", async () => {
		const backend = await createJagFxWasmBackend();
		const sample = new Uint8Array(await readFile(samplePath));

		const info = backend.readSampleInfo(sample);
		const pcm = backend.decodeSampleToPcm(sample);
		expect(info.sampleRate).toBeGreaterThan(0);
		expect(info.loopEnd).toBeLessThanOrEqual(info.sampleCount);
		expect(pcm.byteLength).toBe(info.sampleCount * 2);
		expect(() => backend.readSampleInfo(sample.subarray(0, 3))).toThrow();
	});

	test("rejects a zero loop count", async () => {
		const backend = await createJagFxWasmBackend();
		const synth = new Uint8Array(await readFile(cowDeathPath));

		expect(() => backend.renderSynthToPcm(synth, { loops: 0 })).toThrow(
			"loops must be a positive integer",
		);
	});
});
