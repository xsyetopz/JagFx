import { expect, test } from "bun:test";
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { resolve } from "node:path";

import {
	decodeSampleToPcm,
	readSampleInfo,
	renderSynthToPcm,
} from "@xsyetopz/jagfx-node";

const packageDir = resolve(import.meta.dirname, "..");
const repoRoot = resolve(packageDir, "../..");
const cowDeathPath = resolve(repoRoot, "references/synths/cow_death.synth");
const samplePath = resolve(repoRoot, "references/vorbis/11102.jagvorbis");

// Reflect.apply lets the test pass a plain Uint8Array, as a JavaScript caller can.
const notABuffer = [new Uint8Array([1])];

/**
 * @param {unknown} value
 * @returns {value is typeof import("@xsyetopz/jagfx-node")}
 */
const isBindings = (value) =>
	typeof value === "object" &&
	value !== null &&
	"renderSynthToPcm" in value &&
	typeof value.renderSynthToPcm === "function";

/** @type {unknown} */
const loaded = createRequire(import.meta.url)("../dist/index.cjs");
if (!isBindings(loaded)) {
	throw new TypeError("dist/index.cjs does not export the bindings");
}
const cjs = loaded;

test("renders cow_death through Kotlin/JS", async () => {
	const synth = await readFile(cowDeathPath);
	const pcm = renderSynthToPcm(synth);

	expect(Buffer.isBuffer(pcm)).toBe(true);
	expect(pcm.length).toBeGreaterThan(0);
	expect(pcm.length % 2).toBe(0);
});

test("the CommonJS build renders the same PCM", async () => {
	const synth = await readFile(cowDeathPath);

	expect(cjs.renderSynthToPcm(synth)).toEqual(renderSynthToPcm(synth));
});

test("renders a single voice", async () => {
	const synth = await readFile(cowDeathPath);
	const pcm = renderSynthToPcm(synth, { loops: 2, voiceFilter: 0 });

	expect(pcm.length).toBeGreaterThan(0);
	expect(pcm.length % 2).toBe(0);
});

test("rejects invalid arguments", async () => {
	const synth = await readFile(cowDeathPath);

	expect(() => {
		Reflect.apply(renderSynthToPcm, undefined, notABuffer);
	}).toThrow(TypeError);
	expect(() => renderSynthToPcm(synth, { loops: 0 })).toThrow(
		expect.objectContaining({ code: "JAGFX_INVALID_ARGUMENT" }),
	);
	expect(() => renderSynthToPcm(synth, { voiceFilter: 10 })).toThrow(
		expect.objectContaining({ code: "JAGFX_INVALID_ARGUMENT" }),
	);
});

test("decodes a sample to PCM", async () => {
	const sample = await readFile(samplePath);
	const info = readSampleInfo(sample);
	const pcm = decodeSampleToPcm(sample);

	expect(info.sampleRate).toBeGreaterThan(0);
	expect(info.loopEnd).toBeLessThanOrEqual(info.sampleCount);
	expect(typeof info.pingPong).toBe("boolean");
	expect(pcm.length).toBe(info.sampleCount * 2);
	expect(() => {
		Reflect.apply(decodeSampleToPcm, undefined, notABuffer);
	}).toThrow(TypeError);
	expect(() => readSampleInfo(sample.subarray(0, 3))).toThrow(
		expect.objectContaining({ code: "JAGFX_DECODE_FAILED" }),
	);
});
