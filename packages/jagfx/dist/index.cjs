"use strict";

const { spawn } = require("node:child_process");
const { mkdtemp, readFile, rm, writeFile } = require("node:fs/promises");
const { tmpdir } = require("node:os");
const { dirname, join, resolve } = require("node:path");

const gitLfsPrefix = "version https://git-lfs.github.com/spec/";
const waveforms = Object.freeze(["off", "square", "sine", "saw", "noise"]);

class JagFxError extends Error {
	constructor(message, code = "JAGFX_ERROR") {
		super(message);
		this.name = "JagFxError";
		this.code = code;
	}
}

function isGitLfsPointer(data) {
	if (!data || data.length < gitLfsPrefix.length) {
		return false;
	}

	const prefix = Buffer.from(data)
		.subarray(0, gitLfsPrefix.length)
		.toString("ascii");
	return prefix === gitLfsPrefix;
}

function assertRealSynthData(data) {
	if (isGitLfsPointer(data)) {
		throw new JagFxError(
			"File is a Git LFS pointer, not synth data. Run `git lfs pull` in the archive repository to download the real .synth contents.",
			"JAGFX_GIT_LFS_POINTER",
		);
	}
}

function createSegment(duration, targetLevel) {
	return { duration, targetLevel };
}

function createEnvelope({
	waveform = "off",
	startValue = 0,
	endValue = 0,
	segments = [],
} = {}) {
	assertWaveform(waveform);
	return {
		waveform,
		startValue,
		endValue,
		segments: segments.map((segment) => ({ ...segment })),
	};
}

function createLfo(rateEnvelope, modulationDepth) {
	return {
		rateEnvelope: cloneEnvelope(rateEnvelope),
		modulationDepth: cloneEnvelope(modulationDepth),
	};
}

function createPartial({ amplitude, pitchOffsetSemitones = 0, delay = 0 }) {
	if (!Number.isInteger(amplitude) || amplitude < 1) {
		throw new JagFxError(
			"partial amplitude must be a positive integer.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	return { amplitude, pitchOffsetSemitones, delay };
}

function createEcho({ delayMilliseconds = 0, feedbackPercent = 0 } = {}) {
	return { delayMilliseconds, feedbackPercent };
}

function createLoop(beginMs = 0, endMs = 0) {
	return { beginMs, endMs };
}

function createVoice({
	frequencyEnvelope,
	amplitudeEnvelope,
	durationMs,
	offsetMs = 0,
	pitchLfo,
	amplitudeLfo,
	gapOffEnvelope,
	gapOnEnvelope,
	partials = [],
	echo,
	filter,
}) {
	if (!frequencyEnvelope || !amplitudeEnvelope) {
		throw new JagFxError(
			"frequencyEnvelope and amplitudeEnvelope are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	if (!Number.isInteger(durationMs)) {
		throw new JagFxError("durationMs is required.", "JAGFX_INVALID_ARGUMENT");
	}

	const voice = {
		frequencyEnvelope: cloneEnvelope(frequencyEnvelope),
		amplitudeEnvelope: cloneEnvelope(amplitudeEnvelope),
		durationMs,
	};

	if (offsetMs !== 0) voice.offsetMs = offsetMs;
	if (pitchLfo) voice.pitchLfo = cloneLfo(pitchLfo);
	if (amplitudeLfo) voice.amplitudeLfo = cloneLfo(amplitudeLfo);
	if (gapOffEnvelope) voice.gapOffEnvelope = cloneEnvelope(gapOffEnvelope);
	if (gapOnEnvelope) voice.gapOnEnvelope = cloneEnvelope(gapOnEnvelope);
	if (partials?.length > 0)
		voice.partials = partials.map((partial) => ({ ...partial }));
	if (echo) voice.echo = { ...echo };
	if (filter) voice.filter = structuredClone(filter);

	return voice;
}

function createPatch({ voices = [], loop } = {}) {
	if (voices.length > 10) {
		throw new JagFxError(
			"patches can contain at most 10 voice slots.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const patch = {
		voices: voices.map((voice) => (voice == null ? null : cloneVoice(voice))),
	};

	if (loop && (loop.beginMs !== 0 || loop.endMs !== 0)) {
		patch.loop = { ...loop };
	}

	return patch;
}

async function readPatchJson(path) {
	return JSON.parse(await readFile(path, "utf8"));
}

async function writePatchJson(path, patch) {
	await writeFile(path, `${JSON.stringify(patch, null, 2)}\n`);
}

async function synthToJson(inputPath, options = {}) {
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
	return JSON.parse(stdout);
}

async function jsonToSynth(patchOrPath, outputPath, options = {}) {
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

async function renderPatchToWav(patchOrPath, outputPath, options = {}) {
	const dir = await mkdtemp(join(tmpdir(), "jagfx-patch-"));
	const synthPath = join(dir, "patch.synth");
	try {
		await jsonToSynth(patchOrPath, synthPath, options);
		await renderSynthToWav(synthPath, outputPath, options);
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
}

async function renderSynthToWav(inputPath, outputPath, options = {}) {
	if (!inputPath || !outputPath) {
		throw new JagFxError(
			"inputPath and outputPath are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const synthData = await readFile(inputPath);
	assertRealSynthData(synthData);

	const loops = Number.isInteger(options.loops) ? options.loops : 1;
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

function assertWaveform(waveform) {
	if (!waveforms.includes(waveform)) {
		throw new JagFxError(
			`unknown waveform: ${waveform}`,
			"JAGFX_INVALID_ARGUMENT",
		);
	}
}

function cloneEnvelope(envelope) {
	return createEnvelope(envelope);
}

function cloneLfo(lfo) {
	return createLfo(lfo.rateEnvelope, lfo.modulationDepth);
}

function cloneVoice(voice) {
	return createVoice(voice);
}

async function withTempJson(patch, callback) {
	const dir = await mkdtemp(join(tmpdir(), "jagfx-json-"));
	const jsonPath = join(dir, "patch.json");
	try {
		await writePatchJson(jsonPath, patch);
		return await callback(jsonPath);
	} finally {
		await rm(dir, { recursive: true, force: true });
	}
}

function resolveCommand(command) {
	if (typeof command === "string" && command.length > 0) {
		return { command, args: [] };
	}

	if (command && typeof command.command === "string") {
		return { command: command.command, args: [...(command.args ?? [])] };
	}

	if (process.env.JAGFX_CLI) {
		return { command: process.env.JAGFX_CLI, args: [] };
	}

	const repoRoot = resolve(dirname(__dirname), "../..");
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

function runCommand(command, args, cwd) {
	return new Promise((resolvePromise, reject) => {
		const child = spawn(command, args, {
			cwd,
			stdio: ["ignore", "pipe", "pipe"],
		});

		let stdout = "";
		let stderr = "";
		child.stdout.on("data", (chunk) => {
			stdout += chunk.toString();
		});
		child.stderr.on("data", (chunk) => {
			stderr += chunk.toString();
		});

		child.on("error", (error) => {
			reject(new JagFxError(error.message, "JAGFX_COMMAND_FAILED"));
		});

		child.on("close", (code) => {
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

module.exports = {
	JagFxError,
	assertRealSynthData,
	createEcho,
	createEnvelope,
	createLfo,
	createLoop,
	createPartial,
	createPatch,
	createSegment,
	createVoice,
	isGitLfsPointer,
	jsonToSynth,
	readPatchJson,
	renderPatchToWav,
	renderSynthToWav,
	synthToJson,
	waveforms,
	writePatchJson,
};
