import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import {
	decodeSampleToPcm,
	readSampleInfo,
	renderSynthToPcm,
} from "../dist/index.mjs";

const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = resolve(packageDir, "../..");
const cowDeathPath = resolve(repoRoot, "references/synths/cow_death.synth");
const samplePath = resolve(repoRoot, "references/vorbis/11102.jagvorbis");

test("renders cow_death through Kotlin/JS", async () => {
	const synth = await readFile(cowDeathPath);
	const pcm = renderSynthToPcm(synth);

	assert.ok(Buffer.isBuffer(pcm));
	assert.ok(pcm.length > 0);
	assert.equal(pcm.length % 2, 0);
});

test("the CommonJS build renders the same PCM", async () => {
	const synth = await readFile(cowDeathPath);
	const require = createRequire(import.meta.url);
	const cjs = require("../dist/index.cjs");

	assert.deepEqual(cjs.renderSynthToPcm(synth), renderSynthToPcm(synth));
});

test("renders a single voice", async () => {
	const synth = await readFile(cowDeathPath);
	const pcm = renderSynthToPcm(synth, { loops: 2, voiceFilter: 0 });

	assert.ok(pcm.length > 0);
	assert.equal(pcm.length % 2, 0);
});

test("rejects invalid arguments", async () => {
	const synth = await readFile(cowDeathPath);

	assert.throws(() => renderSynthToPcm(new Uint8Array([1])), TypeError);
	assert.throws(() => renderSynthToPcm(synth, { loops: 0 }), {
		code: "JAGFX_INVALID_ARGUMENT",
	});
	assert.throws(() => renderSynthToPcm(synth, { voiceFilter: 10 }), {
		code: "JAGFX_INVALID_ARGUMENT",
	});
});

test("decodes a sample to PCM", async () => {
	const sample = await readFile(samplePath);
	const info = readSampleInfo(sample);
	const pcm = decodeSampleToPcm(sample);

	assert.ok(info.sampleRate > 0);
	assert.ok(info.loopEnd <= info.sampleCount);
	assert.equal(typeof info.pingPong, "boolean");
	assert.equal(pcm.length, info.sampleCount * 2);
	assert.throws(() => decodeSampleToPcm(new Uint8Array([1])), TypeError);
	assert.throws(() => readSampleInfo(sample.subarray(0, 3)), {
		code: "JAGFX_DECODE_FAILED",
	});
});
