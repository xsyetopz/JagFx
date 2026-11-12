import type { Buffer } from "node:buffer";
import { readFile } from "node:fs/promises";

import * as kotlin from "../kotlin/jagfx-bindings.mjs";
import { assertRealSynthData, JagFxError } from "./errors.js";
import type { PatchJson } from "./types.js";
import { isBlank } from "./util.js";

/** Runs a Kotlin/JS conversion and rethrows its failure as a {@link JagFxError}. */
export const convert = <T>(conversion: () => T): T => {
	try {
		return conversion();
	} catch (error) {
		throw new JagFxError(
			error instanceof Error ? error.message : String(error),
			"JAGFX_CONVERSION_FAILED",
		);
	}
};

export const toInt8Array = (data: Uint8Array): Int8Array =>
	new Int8Array(data.buffer, data.byteOffset, data.byteLength);

export const loopCount = (optionLoops?: number): number => {
	const loops =
		typeof optionLoops === "number" && Number.isInteger(optionLoops)
			? optionLoops
			: 1;
	if (loops < 1) {
		throw new JagFxError(
			"loops must be a positive integer.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}
	return loops;
};

export const readInput = async (inputPath: string): Promise<Buffer> => {
	if (isBlank(inputPath)) {
		throw new JagFxError("inputPath is required.", "JAGFX_INVALID_ARGUMENT");
	}
	const data = await readFile(inputPath);
	assertRealSynthData(data);
	return data;
};

export const readJsonInput = (
	valueOrPath: object | string,
	outputPath: string,
): Promise<string> => {
	if (isBlank(valueOrPath) || isBlank(outputPath)) {
		throw new JagFxError(
			"an input and outputPath are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}
	return typeof valueOrPath === "string"
		? readFile(valueOrPath, "utf8")
		: Promise.resolve(JSON.stringify(valueOrPath));
};

export const encodeSynth = async (
	patchOrPath: PatchJson | string,
): Promise<Int8Array> => {
	const json =
		typeof patchOrPath === "string"
			? await readFile(patchOrPath, "utf8")
			: JSON.stringify(patchOrPath);
	return convert(() => kotlin.jsonToSynth(json));
};
