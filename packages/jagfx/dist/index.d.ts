export interface JagFxCommand {
	command: string;
	args?: readonly string[];
}

export interface RenderSynthToWavOptions {
	loops?: number;
	command?: string | JagFxCommand;
	cwd?: string;
}

export declare class JagFxError extends Error {
	readonly code: string;
	constructor(message: string, code?: string);
}

export declare function isGitLfsPointer(data: Uint8Array | Buffer): boolean;

export declare function assertRealSynthData(data: Uint8Array | Buffer): void;

export declare function renderSynthToWav(
	inputPath: string,
	outputPath: string,
	options?: RenderSynthToWavOptions,
): Promise<void>;
