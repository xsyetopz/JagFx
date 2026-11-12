import { readFile, writeFile } from "node:fs/promises";

import { JagFxError } from "./errors.js";
import * as kotlin from "../kotlin/jagfx-bindings.mjs";
import { convert, readInput, readJsonInput, toInt8Array } from "./convert.js";
import type { FilterJson, SampleInfo, VoiceJson } from "./types.js";
import {
	isBlank,
	isFilterJsonOrNull,
	isSampleInfo,
	isVoiceJson,
	parseJson,
} from "./util.js";

/** Reads a `.tone` file as the JSON form of one voice. */
export const toneToJson = async (inputPath: string): Promise<VoiceJson> => {
	const data = await readInput(inputPath);
	return parseJson(
		convert(() => kotlin.toneToJson(toInt8Array(data))),
		isVoiceJson,
		"a voice",
	);
};

/** Writes a voice, or the JSON file at a path, as a `.tone` file. */
export const jsonToTone = async (
	voiceOrPath: VoiceJson | string,
	outputPath: string,
): Promise<void> => {
	const json = await readJsonInput(voiceOrPath, outputPath);
	await writeFile(
		outputPath,
		convert(() => kotlin.jsonToTone(json)),
	);
};

/** Reads a `.filter` file as the JSON form of a filter, or `null` for no filter. */
export const filterToJson = async (
	inputPath: string,
): Promise<FilterJson | null> => {
	const data = await readInput(inputPath);
	return parseJson(
		convert(() => kotlin.filterToJson(toInt8Array(data))),
		isFilterJsonOrNull,
		"a filter",
	);
};

/** Writes a filter, `null` for no filter, or the JSON file at a path, as a `.filter` file. */
export const jsonToFilter = async (
	filterOrPath: FilterJson | null | string,
	outputPath: string,
): Promise<void> => {
	if (isBlank(outputPath)) {
		throw new JagFxError("outputPath is required.", "JAGFX_INVALID_ARGUMENT");
	}
	const json =
		typeof filterOrPath === "string"
			? await readFile(filterOrPath, "utf8")
			: JSON.stringify(filterOrPath);
	await writeFile(
		outputPath,
		convert(() => kotlin.jsonToFilter(json)),
	);
};

/** Reads the header fields of a `.jagvorbis` sample. */
export const readSampleInfo = async (inputPath: string): Promise<SampleInfo> => {
	const data = await readInput(inputPath);
	return parseJson(
		convert(() => kotlin.sampleInfo(toInt8Array(data))),
		isSampleInfo,
		"sample info",
	);
};

/** Decodes a `.jagvorbis` sample to a 16-bit WAV file at the sample's rate, without looping. */
export const sampleToWav = async (
	inputPath: string,
	outputPath: string,
): Promise<void> => {
	if (isBlank(outputPath)) {
		throw new JagFxError("outputPath is required.", "JAGFX_INVALID_ARGUMENT");
	}
	const data = await readInput(inputPath);
	await writeFile(
		outputPath,
		convert(() => kotlin.sampleToWav(toInt8Array(data))),
	);
};
