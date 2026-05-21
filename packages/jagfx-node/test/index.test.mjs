import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { renderSynthToPcm } from "../dist/index.mjs";

test("renders cow_death through the native addon", async () => {
	const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
	const repoRoot = resolve(packageDir, "../..");
	const synth = await readFile(
		resolve(repoRoot, "references/synths/cow_death.synth"),
	);
	const pcm = renderSynthToPcm(synth);

	assert.ok(Buffer.isBuffer(pcm));
	assert.ok(pcm.length > 0);
	assert.equal(pcm.length % 2, 0);
});
