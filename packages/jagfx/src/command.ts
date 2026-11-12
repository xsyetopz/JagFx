import { JagFxError } from "./errors.js";
import type { JagFxCommand } from "./types.js";
import { isBlank } from "./util.js";

export interface CommandResolution {
	command: string;
	args: string[];
}

interface CommandResult {
	stdout: string;
	stderr: string;
}

/** The CLI to run, or `undefined` to convert in-process with Kotlin/JS. */
export const resolveCommand = (
	command?: string | JagFxCommand,
): CommandResolution | undefined => {
	if (typeof command === "string" && command.length > 0) {
		return { command, args: [] };
	}

	if (
		typeof command === "object" &&
		command !== null &&
		typeof command.command === "string"
	) {
		return { command: command.command, args: [...(command.args ?? [])] };
	}

	const envCommand = process.env["JAGFX_CLI"];
	return envCommand === undefined || isBlank(envCommand)
		? undefined
		: { command: envCommand, args: [] };
};

const spawnCli = (
	command: string,
	args: readonly string[],
	cwd?: string,
): Bun.Subprocess<"ignore", "pipe", "pipe"> => {
	try {
		return Bun.spawn([command, ...args], {
			...(cwd === undefined ? {} : { cwd }),
			stdin: "ignore",
			stdout: "pipe",
			stderr: "pipe",
		});
	} catch (error) {
		throw new JagFxError(
			error instanceof Error ? error.message : String(error),
			"JAGFX_COMMAND_FAILED",
		);
	}
};

/** Runs the CLI and resolves with its output, or rejects with its stderr when it exits non-zero. */
export const runCommand = async (
	command: string,
	args: readonly string[],
	cwd?: string,
): Promise<CommandResult> => {
	const child = spawnCli(command, args, cwd);
	const [stdout, stderr, code] = await Promise.all([
		new Response(child.stdout).text(),
		new Response(child.stderr).text(),
		child.exited,
	]);
	if (code !== 0) {
		throw new JagFxError(
			stderr.trim() || `JagFx command exited with status ${code}.`,
			"JAGFX_COMMAND_FAILED",
		);
	}
	return { stdout, stderr };
};
