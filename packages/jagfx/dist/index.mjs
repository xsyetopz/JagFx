import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const gitLfsPrefix = "version https://git-lfs.github.com/spec/";

export class JagFxError extends Error {
	constructor(message, code = "JAGFX_ERROR") {
		super(message);
		this.name = "JagFxError";
		this.code = code;
	}
}

export function isGitLfsPointer(data) {
	if (!data || data.length < gitLfsPrefix.length) {
		return false;
	}

	const prefix = Buffer.from(data)
		.subarray(0, gitLfsPrefix.length)
		.toString("ascii");
	return prefix === gitLfsPrefix;
}

export function assertRealSynthData(data) {
	if (isGitLfsPointer(data)) {
		throw new JagFxError(
			"File is a Git LFS pointer, not synth data. Run `git lfs pull` in the archive repository to download the real .synth contents.",
			"JAGFX_GIT_LFS_POINTER",
		);
	}
}

export async function renderSynthToWav(inputPath, outputPath, options = {}) {
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

function runCommand(command, args, cwd) {
	return new Promise((resolvePromise, reject) => {
		const child = spawn(command, args, {
			cwd,
			stdio: ["ignore", "pipe", "pipe"],
		});

		let stderr = "";
		child.stderr.on("data", (chunk) => {
			stderr += chunk.toString();
		});

		child.on("error", (error) => {
			reject(new JagFxError(error.message, "JAGFX_COMMAND_FAILED"));
		});

		child.on("close", (code) => {
			if (code === 0) {
				resolvePromise();
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
