import { JagFxError } from "./errors.js";
import type { FilterJson, PatchJson, SampleInfo, VoiceJson } from "./types.js";

export const emptyArray: readonly never[] = Object.freeze([]);
export const emptyOptions: Readonly<Record<string, never>> = Object.freeze({});

export const isBlank = (value: unknown): boolean =>
	value === undefined || value === null || value === "";

const isObject = (value: unknown): value is Record<string, unknown> =>
	typeof value === "object" && value !== null;

export const isPatchJson = (value: unknown): value is PatchJson =>
	isObject(value) && Array.isArray(value["voices"]);

export const isVoiceJson = (value: unknown): value is VoiceJson =>
	isObject(value);

export const isFilterJsonOrNull = (
	value: unknown,
): value is FilterJson | null => value === null || isObject(value);

export const isSampleInfo = (value: unknown): value is SampleInfo =>
	isObject(value) && typeof value["sampleRate"] === "number";

/** JSON.parse that narrows its result with `guard` instead of returning `any`. */
export const parseJson = <T>(
	text: string,
	guard: (value: unknown) => value is T,
	what: string,
): T => {
	const parsed: unknown = JSON.parse(text);
	if (!guard(parsed)) {
		throw new JagFxError(
			`unexpected JSON for ${what}.`,
			"JAGFX_CONVERSION_FAILED",
		);
	}
	return parsed;
};
