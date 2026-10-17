import { Buffer } from "node:buffer";
import {
	renderPcm16Le,
	sampleInfo,
	samplePcm16Le,
} from "../kotlin/jagfx-bindings.mjs";

export interface RenderSynthToPcmOptions {
	loops?: number;
	voiceFilter?: number;
}

/** Header fields of a `.jagvorbis` sample. */
export interface SampleInfo {
	sampleRate: number;
	sampleCount: number;
	loopBegin: number;
	/** Sample after the loop region. */
	loopEnd: number;
	pingPong: boolean;
	packetCount: number;
}

const voiceSlots = 10;

export function renderSynthToPcm(
	synthData: Buffer,
	options: RenderSynthToPcmOptions = {},
): Buffer {
	if (!Buffer.isBuffer(synthData)) {
		throw new TypeError("synthData must be a Buffer.");
	}

	const loops = integerOption(options.loops, 1);
	const voiceFilter = integerOption(options.voiceFilter, -1);
	if (loops < 1) {
		throw codedError(
			"loops must be a positive integer.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}
	if (voiceFilter < -1 || voiceFilter >= voiceSlots) {
		throw codedError(
			`voiceFilter must be -1 or a voice index from 0 to ${voiceSlots - 1}.`,
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const synth = toInt8Array(synthData);
	const pcm = run(
		() => renderPcm16Le(synth, loops, voiceFilter),
		"JAGFX_RENDER_FAILED",
	);
	return Buffer.from(pcm.buffer, pcm.byteOffset, pcm.byteLength);
}

/** Decodes `.jagvorbis` sample bytes to mono signed 16-bit little-endian PCM, without looping. */
export function decodeSampleToPcm(sampleData: Buffer): Buffer {
	const sample = sampleBytes(sampleData);
	const pcm = run(() => samplePcm16Le(sample), "JAGFX_DECODE_FAILED");
	return Buffer.from(pcm.buffer, pcm.byteOffset, pcm.byteLength);
}

/** Reads the header fields of `.jagvorbis` sample bytes. */
export function readSampleInfo(sampleData: Buffer): SampleInfo {
	const sample = sampleBytes(sampleData);
	return JSON.parse(
		run(() => sampleInfo(sample), "JAGFX_DECODE_FAILED"),
	) as SampleInfo;
}

function sampleBytes(sampleData: Buffer): Int8Array {
	if (!Buffer.isBuffer(sampleData)) {
		throw new TypeError("sampleData must be a Buffer.");
	}
	return toInt8Array(sampleData);
}

function toInt8Array(data: Buffer): Int8Array {
	return new Int8Array(data.buffer, data.byteOffset, data.byteLength);
}

/** Runs a Kotlin/JS call and rethrows its failure with [code]. */
function run<T>(call: () => T, code: string): T {
	try {
		return call();
	} catch (error) {
		throw codedError(
			error instanceof Error ? error.message : String(error),
			code,
		);
	}
}

function integerOption(value: number | undefined, fallback: number): number {
	return typeof value === "number" && Number.isInteger(value)
		? value
		: fallback;
}

function codedError(message: string, code: string): Error {
	return Object.assign(new Error(message), { code });
}
