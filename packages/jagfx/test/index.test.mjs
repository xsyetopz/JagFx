import assert from "node:assert/strict";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import {
	assertRealSynthData,
	isGitLfsPointer,
	JagFxError,
	renderSynthToWav,
} from "../dist/index.mjs";

const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = resolve(packageDir, "../..");
const cowDeathPath = resolve(repoRoot, "references/synths/cow_death.synth");
const fakeCliPath = resolve(packageDir, "test/fake-jagfx-cli.mjs");
const lfsPointer = Buffer.from(`version https://git-lfs.github.com/spec/v1
oid sha256:ba0494ff046823e2e11143beff9c0c705189afb8c810742f2e798b68f0f7baed
size 130
`);

test("detects Git LFS pointer files", () => {
	assert.equal(isGitLfsPointer(lfsPointer), true);
	assert.throws(() => assertRealSynthData(lfsPointer), {
		name: "JagFxError",
		code: "JAGFX_GIT_LFS_POINTER",
	});
});

test("does not classify real synth bytes as an LFS pointer", async () => {
	const synthData = await readFile(cowDeathPath);

	assert.equal(isGitLfsPointer(synthData), false);
	assert.doesNotThrow(() => assertRealSynthData(synthData));
});

test("renders synth to wav through a command backend", async () => {
	const dir = await mkdtemp(join(tmpdir(), "jagfx-js-"));
	const outputPath = join(dir, "cow_death.wav");

	try {
		await renderSynthToWav(cowDeathPath, outputPath, {
			command: {
				command: process.execPath,
				args: [fakeCliPath],
			},
		});
		const wav = await readFile(outputPath);
		assert.equal(wav.subarray(0, 4).toString("ascii"), "RIFF");
		assert.equal(wav.subarray(8, 12).toString("ascii"), "WAVE");
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
});

test("rejects invalid loop counts", async () => {
	await assert.rejects(
		() => renderSynthToWav(cowDeathPath, "unused.wav", { loops: 0 }),
		(error) =>
			error instanceof JagFxError && error.code === "JAGFX_INVALID_ARGUMENT",
	);
});
