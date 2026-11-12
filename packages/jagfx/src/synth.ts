import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

import * as kotlin from "../kotlin/jagfx-bindings.mjs";
import { resolveCommand, runCommand } from "./command.js";
import { convert, encodeSynth, loopCount, toInt8Array } from "./convert.js";
import { assertRealSynthData, JagFxError } from "./errors.js";
import type {
	CommandOptions,
	PatchJson,
	RenderSynthToWavOptions,
	SynthToJsonOptions,
} from "./types.js";
import { emptyOptions, isBlank, isPatchJson, parseJson } from "./util.js";

/** Reads a patch from a JSON file. */
export const readPatchJson = async (path: string): Promise<PatchJson> =>
	parseJson(await readFile(path, "utf8"), isPatchJson, "a patch");

/** Writes a patch as a JSON file. */
export const writePatchJson = async (
	path: string,
	patch: PatchJson,
): Promise<void> => {
	await writeFile(path, `${JSON.stringify(patch, null, 2)}\n`);
};

const withTempJson = async <T>(
	patch: PatchJson,
	callback: (jsonPath: string) => Promise<T>,
): Promise<T> => {
	const dir = await mkdtemp(join(tmpdir(), "jagfx-json-"));
	const jsonPath = join(dir, "patch.json");
	try {
		await writePatchJson(jsonPath, patch);
		return await callback(jsonPath);
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
};

/** Reads a `.synth` file as the JSON form of a patch. */
export const synthToJson = async (
	inputPath: string,
	options: SynthToJsonOptions = emptyOptions,
): Promise<PatchJson> => {
	if (isBlank(inputPath)) {
		throw new JagFxError("inputPath is required.", "JAGFX_INVALID_ARGUMENT");
	}

	const synthData = await readFile(inputPath);
	assertRealSynthData(synthData);

	const command = resolveCommand(options.command);
	if (!command) {
		const json = convert(() => kotlin.synthToJson(toInt8Array(synthData)));
		if (options.outputPath !== undefined && options.outputPath !== "") {
			await writeFile(options.outputPath, `${json}\n`);
		}
		return parseJson(json, isPatchJson, "a patch");
	}

	if (options.outputPath !== undefined && options.outputPath !== "") {
		await runCommand(
			command.command,
			[...command.args, "to-json", inputPath, options.outputPath],
			options.cwd,
		);
		return readPatchJson(options.outputPath);
	}

	const { stdout } = await runCommand(
		command.command,
		[...command.args, "to-json", inputPath],
		options.cwd,
	);
	return parseJson(stdout, isPatchJson, "a patch");
};

/** Writes a patch, or the JSON file at a path, as a `.synth` file. */
export const jsonToSynth = async (
	patchOrPath: PatchJson | string,
	outputPath: string,
	options: CommandOptions = emptyOptions,
): Promise<void> => {
	if (isBlank(patchOrPath) || isBlank(outputPath)) {
		throw new JagFxError(
			"patchOrPath and outputPath are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const command = resolveCommand(options.command);
	if (!command) {
		await writeFile(outputPath, await encodeSynth(patchOrPath));
		return;
	}

	if (typeof patchOrPath === "string") {
		await runCommand(
			command.command,
			[...command.args, "from-json", patchOrPath, outputPath],
			options.cwd,
		);
		return;
	}

	await withTempJson(patchOrPath, async (jsonPath) => {
		await runCommand(
			command.command,
			[...command.args, "from-json", jsonPath, outputPath],
			options.cwd,
		);
	});
};

/** Renders a `.synth` file to a WAV file. */
export const renderSynthToWav = async (
	inputPath: string,
	outputPath: string,
	options: RenderSynthToWavOptions = emptyOptions,
): Promise<void> => {
	if (isBlank(inputPath) || isBlank(outputPath)) {
		throw new JagFxError(
			"inputPath and outputPath are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const synthData = await readFile(inputPath);
	assertRealSynthData(synthData);

	const loops = loopCount(options.loops);
	const command = resolveCommand(options.command);
	if (!command) {
		await writeFile(
			outputPath,
			convert(() => kotlin.renderWav(toInt8Array(synthData), loops)),
		);
		return;
	}

	await runCommand(
		command.command,
		[...command.args, inputPath, outputPath, String(loops)],
		options.cwd,
	);
};

/** Renders a patch, or the JSON file at a path, to a WAV file. */
export const renderPatchToWav = async (
	patchOrPath: PatchJson | string,
	outputPath: string,
	options: RenderSynthToWavOptions = emptyOptions,
): Promise<void> => {
	if (!resolveCommand(options.command)) {
		if (isBlank(patchOrPath) || isBlank(outputPath)) {
			throw new JagFxError(
				"patchOrPath and outputPath are required.",
				"JAGFX_INVALID_ARGUMENT",
			);
		}
		const loops = loopCount(options.loops);
		const synth = await encodeSynth(patchOrPath);
		await writeFile(
			outputPath,
			convert(() => kotlin.renderWav(synth, loops)),
		);
		return;
	}

	const dir = await mkdtemp(join(tmpdir(), "jagfx-patch-"));
	const synthPath = join(dir, "patch.synth");
	try {
		await jsonToSynth(patchOrPath, synthPath, options);
		await renderSynthToWav(synthPath, outputPath, options);
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
};
