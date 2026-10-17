import { afterAll, beforeAll, describe, expect, test } from "bun:test";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
	assertRealSynthData,
	createEnvelope,
	createPatch,
	createVoice,
	filterToJson,
	isGitLfsPointer,
	JagFxError,
	jsonToFilter,
	jsonToSynth,
	jsonToTone,
	readPatchJson,
	readSampleInfo,
	renderPatchToWav,
	renderSynthToWav,
	sampleToWav,
	synthToJson,
	toneToJson,
	writePatchJson,
} from "../dist/index.mjs";

const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = resolve(packageDir, "../..");
const cowDeathPath = resolve(repoRoot, "references/synths/cow_death.synth");
const cowDeathJsonPath = resolve(repoRoot, "references/json/cow_death.json");
const cowDeathWavPath = resolve(repoRoot, "references/wav/cow_death.wav");
const samplePath = resolve(repoRoot, "references/vorbis/11102.jagvorbis");
const fakeCliPath = resolve(packageDir, "test/fake-jagfx-cli.mjs");
const fakeCommand = { command: process.execPath, args: [fakeCliPath] };
const lfsPointer = Buffer.from(`version https://git-lfs.github.com/spec/v1
oid sha256:ba0494ff046823e2e11143beff9c0c705189afb8c810742f2e798b68f0f7baed
size 130
`);

describe("git lfs checks", () => {
	test("detects Git LFS pointer files", () => {
		expect(isGitLfsPointer(lfsPointer)).toBe(true);
		expect(() => assertRealSynthData(lfsPointer)).toThrow(JagFxError);
	});

	test("does not classify real synth bytes as an LFS pointer", async () => {
		const synthData = await readFile(cowDeathPath);

		expect(isGitLfsPointer(synthData)).toBe(false);
		expect(() => assertRealSynthData(synthData)).not.toThrow();
	});
});

describe("patch builders", () => {
	test("creates a minimal patch shape", () => {
		const patch = createPatch({
			voices: [
				createVoice({
					frequencyEnvelope: createEnvelope({
						waveform: "sine",
						startValue: 1,
						endValue: 2,
					}),
					amplitudeEnvelope: createEnvelope({
						waveform: "off",
						startValue: 0,
						endValue: 65535,
					}),
					durationMs: 250,
				}),
			],
		});

		expect(patch.voices).toHaveLength(1);
		expect(patch.voices[0].frequencyEnvelope.waveform).toBe("sine");
		expect(patch.voices[0].durationMs).toBe(250);
	});
});

describe("cli-backed conversions", () => {
	test("renders synth to wav through a command backend", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-js-"));
		const outputPath = join(dir, "cow_death.wav");

		try {
			await renderSynthToWav(cowDeathPath, outputPath, {
				command: fakeCommand,
			});
			const wav = await readFile(outputPath);
			expect(wav.subarray(0, 4).toString("ascii")).toBe("RIFF");
			expect(wav.subarray(8, 12).toString("ascii")).toBe("WAVE");
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("converts synth to json through the CLI", async () => {
		const patch = await synthToJson(cowDeathPath, { command: fakeCommand });

		expect(patch.voices).toHaveLength(1);
		expect(patch.voices[0].durationMs).toBe(500);
	});

	test("writes json and converts it to synth through the CLI", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-json-"));
		const jsonPath = join(dir, "patch.json");
		const synthPath = join(dir, "patch.synth");
		const patch = createPatch({ voices: [] });

		try {
			await writePatchJson(jsonPath, patch);
			expect(await readPatchJson(jsonPath)).toEqual(patch);
			await jsonToSynth(jsonPath, synthPath, { command: fakeCommand });
			expect([...(await readFile(synthPath))]).toEqual([1, 2, 3, 4]);
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("renders a patch object to wav", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-patch-"));
		const outputPath = join(dir, "patch.wav");
		const patch = createPatch({ voices: [] });

		try {
			await renderPatchToWav(patch, outputPath, { command: fakeCommand });
			const wav = await readFile(outputPath);
			expect(wav.subarray(0, 4).toString("ascii")).toBe("RIFF");
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});
});

describe("in-process conversions", () => {
	const savedCli = process.env.JAGFX_CLI;
	beforeAll(() => {
		delete process.env.JAGFX_CLI;
	});
	afterAll(() => {
		if (savedCli !== undefined) {
			process.env.JAGFX_CLI = savedCli;
		}
	});

	test("converts synth to the reference json", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-json-"));
		const jsonPath = join(dir, "cow_death.json");
		const reference = await readFile(cowDeathJsonPath, "utf8");

		try {
			const patch = await synthToJson(cowDeathPath, { outputPath: jsonPath });
			expect(patch).toEqual(JSON.parse(reference));
			expect(await readFile(jsonPath, "utf8")).toBe(reference);
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("converts json back to the original synth bytes", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-synth-"));
		const synthPath = join(dir, "cow_death.synth");
		const original = await readFile(cowDeathPath);

		try {
			await jsonToSynth(cowDeathJsonPath, synthPath);
			expect(await readFile(synthPath)).toEqual(original);
			await jsonToSynth(await readPatchJson(cowDeathJsonPath), synthPath);
			expect(await readFile(synthPath)).toEqual(original);
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("renders synth and patch to the reference wav", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-wav-"));
		const synthWavPath = join(dir, "synth.wav");
		const patchWavPath = join(dir, "patch.wav");
		const reference = await readFile(cowDeathWavPath);

		try {
			await renderSynthToWav(cowDeathPath, synthWavPath);
			expect(await readFile(synthWavPath)).toEqual(reference);
			await renderPatchToWav(cowDeathJsonPath, patchWavPath);
			expect(await readFile(patchWavPath)).toEqual(reference);
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("converts tones and filters through json", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-tone-"));
		const tonePath = join(dir, "voice.tone");
		const filterPath = join(dir, "voice.filter");
		const voice = (await readPatchJson(cowDeathJsonPath)).voices.find(Boolean);

		try {
			await jsonToTone(voice, tonePath);
			expect(await toneToJson(tonePath)).toEqual(voice);
			await jsonToFilter(voice.filter ?? null, filterPath);
			expect(await filterToJson(filterPath)).toEqual(voice.filter ?? null);
			await jsonToFilter(null, filterPath);
			expect([...(await readFile(filterPath))]).toEqual([0]);
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("decodes a sample to wav at its rate", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-sample-"));
		const wavPath = join(dir, "sample.wav");

		try {
			const info = await readSampleInfo(samplePath);
			await sampleToWav(samplePath, wavPath);
			const wav = await readFile(wavPath);
			expect(wav.readUInt32LE(24)).toBe(info.sampleRate);
			expect(wav.length).toBe(44 + info.sampleCount * 2);
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});

	test("reports malformed json as a conversion failure", async () => {
		const dir = await mkdtemp(join(tmpdir(), "jagfx-bad-"));
		const jsonPath = join(dir, "bad.json");

		try {
			await writeFile(jsonPath, "{");
			await expect(
				jsonToSynth(jsonPath, join(dir, "bad.synth")),
			).rejects.toMatchObject({ code: "JAGFX_CONVERSION_FAILED" });
		} finally {
			await rm(dir, { recursive: true, force: true });
		}
	});
});

test("rejects invalid loop counts", async () => {
	await expect(
		renderSynthToWav(cowDeathPath, "unused.wav", { loops: 0 }),
	).rejects.toMatchObject({ code: "JAGFX_INVALID_ARGUMENT" });
});
