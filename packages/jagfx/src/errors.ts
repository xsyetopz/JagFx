import { Buffer } from "node:buffer";

const gitLfsPrefix = "version https://git-lfs.github.com/spec/";
const gitLfsPrefixBytes = Buffer.from(gitLfsPrefix, "ascii");

/** The built-in waveform names. */
export const waveforms = Object.freeze([
	"off",
	"square",
	"sine",
	"saw",
	"noise",
] as const);

/** Error with a stable `code` for every failure this package raises. */
export class JagFxError extends Error {
	readonly code: string;

	constructor(message: string, code = "JAGFX_ERROR") {
		super(message);
		this.name = "JagFxError";
		this.code = code;
	}
}

/** Returns whether the bytes start like a Git LFS pointer file. */
export const isGitLfsPointer = (data: Uint8Array): boolean => {
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
};

/** Throws a {@link JagFxError} when the bytes are a Git LFS pointer instead of synth data. */
export const assertRealSynthData = (data: Uint8Array): void => {
	if (isGitLfsPointer(data)) {
		throw new JagFxError(
			"File is a Git LFS pointer, not synth data. Run `git lfs pull` in the archive repository to download the real .synth contents.",
			"JAGFX_GIT_LFS_POINTER",
		);
	}
};
