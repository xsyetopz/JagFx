import { Buffer } from "node:buffer";
import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export type Waveform = "off" | "square" | "sine" | "saw" | "noise";

export interface JagFxCommand {
	command: string;
	args?: readonly string[];
}

export interface CommandOptions {
	command?: string | JagFxCommand;
	cwd?: string;
}

export interface RenderSynthToWavOptions extends CommandOptions {
	loops?: number;
}

export interface SynthToJsonOptions extends CommandOptions {
	outputPath?: string;
}

export interface SegmentJson {
	duration: number;
	targetLevel: number;
}

export interface EnvelopeJson {
	waveform: Waveform;
	startValue: number;
	endValue: number;
	segments: SegmentJson[];
}

export interface LfoJson {
	rateEnvelope: EnvelopeJson;
	modulationDepth: EnvelopeJson;
}

export interface PartialJson {
	amplitude: number;
	pitchOffsetSemitones: number;
	delay?: number;
}

export interface PartialInputJson {
	amplitude: number;
	pitchOffsetSemitones?: number;
	delay?: number;
}

export interface EchoJson {
	delayMilliseconds: number;
	feedbackPercent: number;
}

export interface FilterPhasesJson {
	baseline: number[];
	modulated: number[];
}

export interface FilterCoefficientsJson {
	feedforward: FilterPhasesJson;
	feedback: FilterPhasesJson;
}

export interface FilterJson {
	poleCounts: [number, number];
	unityGain: [number, number];
	polePhase: FilterCoefficientsJson;
	poleMagnitude: FilterCoefficientsJson;
	modulationEnvelope?: EnvelopeJson | null;
}

export interface VoiceJson {
	frequencyEnvelope: EnvelopeJson;
	amplitudeEnvelope: EnvelopeJson;
	pitchLfo?: LfoJson | null;
	amplitudeLfo?: LfoJson | null;
	gapOffEnvelope?: EnvelopeJson | null;
	gapOnEnvelope?: EnvelopeJson | null;
	partials?: PartialJson[] | null;
	echo?: EchoJson | null;
	durationMs: number;
	offsetMs?: number;
	filter?: FilterJson | null;
}

export interface LoopSegmentJson {
	beginMs: number;
	endMs: number;
}

export interface PatchJson {
	voices: Array<VoiceJson | null>;
	loop?: LoopSegmentJson | null;
}

interface CommandResolution {
	command: string;
	args: string[];
}

interface CommandResult {
	stdout: string;
	stderr: string;
}

const gitLfsPrefix = "version https://git-lfs.github.com/spec/";
const gitLfsPrefixBytes = Buffer.from(gitLfsPrefix, "ascii");
const emptyArray = Object.freeze([]) as readonly never[];
const emptyOptions = Object.freeze({}) as Readonly<Record<string, never>>;

export const waveforms = Object.freeze([
	"off",
	"square",
	"sine",
	"saw",
	"noise",
] as const);

export class JagFxError extends Error {
	readonly code: string;

	constructor(message: string, code = "JAGFX_ERROR") {
		super(message);
		this.name = "JagFxError";
		this.code = code;
	}
}

export function isGitLfsPointer(data: Uint8Array): boolean {
	if (
		!(data instanceof Uint8Array) ||
		data.byteLength < gitLfsPrefixBytes.length
	) {
		return false;
	}

	for (let i = 0; i < gitLfsPrefixBytes.length; i++) {
		if (data[i] !== gitLfsPrefixBytes[i]) {
			return false;
		}
	}

	return true;
}

export function assertRealSynthData(data: Uint8Array): void {
	if (isGitLfsPointer(data)) {
		throw new JagFxError(
			"File is a Git LFS pointer, not synth data. Run `git lfs pull` in the archive repository to download the real .synth contents.",
			"JAGFX_GIT_LFS_POINTER",
		);
	}
}

export function createSegment(
	duration: number,
	targetLevel: number,
): SegmentJson {
	return { duration, targetLevel };
}

export function createEnvelope(
	envelope: Partial<EnvelopeJson> = emptyOptions,
): EnvelopeJson {
	const waveform = envelope.waveform ?? "off";
	assertWaveform(waveform);
	return {
		waveform,
		startValue: envelope.startValue ?? 0,
		endValue: envelope.endValue ?? 0,
		segments: cloneSegments(envelope.segments ?? emptyArray),
	};
}

export function createLfo(
	rateEnvelope: EnvelopeJson,
	modulationDepth: EnvelopeJson,
): LfoJson {
	return {
		rateEnvelope: cloneEnvelope(rateEnvelope),
		modulationDepth: cloneEnvelope(modulationDepth),
	};
}

export function createPartial({
	amplitude,
	pitchOffsetSemitones = 0,
	delay = 0,
}: PartialInputJson): PartialJson {
	if (!Number.isInteger(amplitude) || amplitude < 1) {
		throw new JagFxError(
			"partial amplitude must be a positive integer.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	return { amplitude, pitchOffsetSemitones, delay };
}

export function createEcho(echo: Partial<EchoJson> = emptyOptions): EchoJson {
	return {
		delayMilliseconds: echo.delayMilliseconds ?? 0,
		feedbackPercent: echo.feedbackPercent ?? 0,
	};
}

export function createLoop(beginMs = 0, endMs = 0): LoopSegmentJson {
	return { beginMs, endMs };
}

export function createVoice(voiceInput: VoiceJson): VoiceJson {
	const {
		frequencyEnvelope,
		amplitudeEnvelope,
		durationMs,
		offsetMs = 0,
		pitchLfo,
		amplitudeLfo,
		gapOffEnvelope,
		gapOnEnvelope,
		partials = emptyArray,
		echo,
		filter,
	} = voiceInput;

	if (!frequencyEnvelope || !amplitudeEnvelope) {
		throw new JagFxError(
			"frequencyEnvelope and amplitudeEnvelope are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	if (!Number.isInteger(durationMs)) {
		throw new JagFxError("durationMs is required.", "JAGFX_INVALID_ARGUMENT");
	}

	const voice: VoiceJson = {
		frequencyEnvelope: cloneEnvelope(frequencyEnvelope),
		amplitudeEnvelope: cloneEnvelope(amplitudeEnvelope),
		durationMs,
	};

	if (offsetMs !== 0) voice.offsetMs = offsetMs;
	if (pitchLfo) voice.pitchLfo = cloneLfo(pitchLfo);
	if (amplitudeLfo) voice.amplitudeLfo = cloneLfo(amplitudeLfo);
	if (gapOffEnvelope) voice.gapOffEnvelope = cloneEnvelope(gapOffEnvelope);
	if (gapOnEnvelope) voice.gapOnEnvelope = cloneEnvelope(gapOnEnvelope);
	if (partials && partials.length > 0) voice.partials = clonePartials(partials);
	if (echo) voice.echo = cloneEcho(echo);
	if (filter) voice.filter = cloneFilter(filter);

	return voice;
}

export function createPatch(
	patchInput: Partial<PatchJson> = emptyOptions,
): PatchJson {
	const voices = patchInput.voices ?? emptyArray;
	if (voices.length > 10) {
		throw new JagFxError(
			"patches can contain at most 10 voice slots.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const voiceCount = voices.length;
	const clonedVoices = new Array<VoiceJson | null>(voiceCount);
	for (let i = 0; i < voiceCount; i++) {
		const voice = voices[i];
		clonedVoices[i] = voice == null ? null : cloneVoice(voice);
	}

	const patch: PatchJson = { voices: clonedVoices };
	const loop = patchInput.loop;
	if (loop && (loop.beginMs !== 0 || loop.endMs !== 0)) {
		patch.loop = cloneLoop(loop);
	}

	return patch;
}

export async function readPatchJson(path: string): Promise<PatchJson> {
	return JSON.parse(await readFile(path, "utf8")) as PatchJson;
}

export async function writePatchJson(
	path: string,
	patch: PatchJson,
): Promise<void> {
	await writeFile(path, `${JSON.stringify(patch, null, 2)}\n`);
}

export async function synthToJson(
	inputPath: string,
	options: SynthToJsonOptions = emptyOptions,
): Promise<PatchJson> {
	if (!inputPath) {
		throw new JagFxError("inputPath is required.", "JAGFX_INVALID_ARGUMENT");
	}

	const synthData = await readFile(inputPath);
	assertRealSynthData(synthData);

	const command = resolveCommand(options.command);
	if (options.outputPath) {
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
	return JSON.parse(stdout) as PatchJson;
}

export async function jsonToSynth(
	patchOrPath: PatchJson | string,
	outputPath: string,
	options: CommandOptions = emptyOptions,
): Promise<void> {
	if (!patchOrPath || !outputPath) {
		throw new JagFxError(
			"patchOrPath and outputPath are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const command = resolveCommand(options.command);
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
}

export async function renderPatchToWav(
	patchOrPath: PatchJson | string,
	outputPath: string,
	options: RenderSynthToWavOptions = emptyOptions,
): Promise<void> {
	const dir = await mkdtemp(join(tmpdir(), "jagfx-patch-"));
	const synthPath = join(dir, "patch.synth");
	try {
		await jsonToSynth(patchOrPath, synthPath, options);
		await renderSynthToWav(synthPath, outputPath, options);
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
}

export async function renderSynthToWav(
	inputPath: string,
	outputPath: string,
	options: RenderSynthToWavOptions = emptyOptions,
): Promise<void> {
	if (!inputPath || !outputPath) {
		throw new JagFxError(
			"inputPath and outputPath are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const synthData = await readFile(inputPath);
	assertRealSynthData(synthData);

	const optionLoops = options.loops;
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

	const command = resolveCommand(options.command);
	await runCommand(
		command.command,
		[...command.args, inputPath, outputPath, String(loops)],
		options.cwd,
	);
}

function assertWaveform(waveform: string): asserts waveform is Waveform {
	switch (waveform) {
		case "off":
		case "square":
		case "sine":
		case "saw":
		case "noise":
			return;
		default:
			throw new JagFxError(
				`unknown waveform: ${waveform}`,
				"JAGFX_INVALID_ARGUMENT",
			);
	}
}

function cloneSegments(segments: readonly SegmentJson[]): SegmentJson[] {
	const segmentCount = segments.length;
	const cloned = new Array<SegmentJson>(segmentCount);
	for (let i = 0; i < segmentCount; i++) {
		const segment = segments[i];
		cloned[i] = {
			duration: segment.duration,
			targetLevel: segment.targetLevel,
		};
	}
	return cloned;
}

function cloneEnvelope(envelope: EnvelopeJson): EnvelopeJson {
	return createEnvelope(envelope);
}

function cloneLfo(lfo: LfoJson): LfoJson {
	return createLfo(lfo.rateEnvelope, lfo.modulationDepth);
}

function cloneVoice(voice: VoiceJson): VoiceJson {
	return createVoice(voice);
}

function clonePartials(partials: readonly PartialJson[]): PartialJson[] {
	const partialCount = partials.length;
	const cloned = new Array<PartialJson>(partialCount);
	for (let i = 0; i < partialCount; i++) {
		cloned[i] = clonePartial(partials[i]);
	}
	return cloned;
}

function clonePartial(partial: PartialJson): PartialJson {
	const cloned: PartialJson = {
		amplitude: partial.amplitude,
		pitchOffsetSemitones: partial.pitchOffsetSemitones,
	};
	if (partial.delay !== undefined) {
		cloned.delay = partial.delay;
	}
	return cloned;
}

function cloneEcho(echo: EchoJson): EchoJson {
	return {
		delayMilliseconds: echo.delayMilliseconds,
		feedbackPercent: echo.feedbackPercent,
	};
}

function cloneLoop(loop: LoopSegmentJson): LoopSegmentJson {
	return {
		beginMs: loop.beginMs,
		endMs: loop.endMs,
	};
}

function cloneFilter(filter: FilterJson): FilterJson {
	const cloned: FilterJson = {
		poleCounts: cloneNumberPair(filter.poleCounts),
		unityGain: cloneNumberPair(filter.unityGain),
		polePhase: cloneFilterCoefficients(filter.polePhase),
		poleMagnitude: cloneFilterCoefficients(filter.poleMagnitude),
	};
	if ("modulationEnvelope" in filter) {
		cloned.modulationEnvelope =
			filter.modulationEnvelope == null
				? null
				: cloneEnvelope(filter.modulationEnvelope);
	}
	return cloned;
}

function cloneFilterCoefficients(
	coefficients: FilterCoefficientsJson,
): FilterCoefficientsJson {
	return {
		feedforward: cloneFilterPhases(coefficients.feedforward),
		feedback: cloneFilterPhases(coefficients.feedback),
	};
}

function cloneFilterPhases(phases: FilterPhasesJson): FilterPhasesJson {
	return {
		baseline: cloneNumberArray(phases.baseline),
		modulated: cloneNumberArray(phases.modulated),
	};
}

function cloneNumberPair(values: readonly [number, number]): [number, number] {
	return [values[0], values[1]];
}

function cloneNumberArray(values: readonly number[]): number[] {
	const valueCount = values.length;
	const cloned = new Array<number>(valueCount);
	for (let i = 0; i < valueCount; i++) {
		cloned[i] = values[i];
	}
	return cloned;
}

async function withTempJson<T>(
	patch: PatchJson,
	callback: (jsonPath: string) => Promise<T>,
): Promise<T> {
	const dir = await mkdtemp(join(tmpdir(), "jagfx-json-"));
	const jsonPath = join(dir, "patch.json");
	try {
		await writePatchJson(jsonPath, patch);
		return await callback(jsonPath);
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
}

function resolveCommand(command?: string | JagFxCommand): CommandResolution {
	if (typeof command === "string" && command.length > 0) {
		return { command, args: [] };
	}

	if (
		typeof command === "object" &&
		command !== null &&
		typeof command.command === "string"
	) {
		return { command: command.command, args: cloneCommandArgs(command.args) };
	}

	const envCommand = process.env["JAGFX_CLI"];
	if (envCommand) {
		return { command: envCommand, args: [] };
	}

	const packageDir = dirname(fileURLToPath(import.meta.url));
	const repoRoot = resolve(packageDir, "../../..");
	return {
		command: "dotnet",
		args: [
			"run",
			"--no-restore",
			"--project",
			resolve(repoRoot, "src/JagFx.Cli"),
			"--",
		],
	};
}

function cloneCommandArgs(args?: readonly string[]): string[] {
	if (!args || args.length === 0) {
		return [];
	}

	const cloned = new Array<string>(args.length);
	for (let i = 0; i < args.length; i++) {
		cloned[i] = args[i];
	}
	return cloned;
}

function runCommand(
	command: string,
	args: readonly string[],
	cwd?: string,
): Promise<CommandResult> {
	return new Promise((resolvePromise, reject) => {
		const child = spawn(command, args, {
			cwd,
			stdio: ["ignore", "pipe", "pipe"],
		});

		const stdoutChunks: Buffer[] = [];
		const stderrChunks: Buffer[] = [];
		let stdoutLength = 0;
		let stderrLength = 0;

		child.stdout.on("data", (chunk: Buffer) => {
			stdoutChunks.push(chunk);
			stdoutLength += chunk.byteLength;
		});
		child.stderr.on("data", (chunk: Buffer) => {
			stderrChunks.push(chunk);
			stderrLength += chunk.byteLength;
		});

		child.on("error", (error) => {
			reject(new JagFxError(error.message, "JAGFX_COMMAND_FAILED"));
		});

		child.on("close", (code) => {
			const stdout = decodeChunks(stdoutChunks, stdoutLength);
			const stderr = decodeChunks(stderrChunks, stderrLength);
			if (code === 0) {
				resolvePromise({ stdout, stderr });
				return;
			}

			reject(
				new JagFxError(
					stderr.trim() || `JagFx command exited with status ${code}.`,
					"JAGFX_COMMAND_FAILED",
				),
			);
		});
	});
}

function decodeChunks(chunks: readonly Buffer[], byteLength: number): string {
	return byteLength === 0
		? ""
		: Buffer.concat(chunks, byteLength).toString("utf8");
}
