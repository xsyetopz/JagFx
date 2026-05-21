import { existsSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
configureNativeLibrary();
const binding = require("../build/Release/jagfx_node.node");

export function renderSynthToPcm(synthData, options = {}) {
	if (!Buffer.isBuffer(synthData)) {
		throw new TypeError("synthData must be a Buffer.");
	}

	return binding.renderSynthToPcm(synthData, options);
}

function configureNativeLibrary() {
	if (process.env.JAGFX_NATIVE_LIB) {
		return;
	}

	const platform = process.platform === "darwin" ? "darwin" : process.platform;
	const extension =
		process.platform === "win32"
			? "dll"
			: process.platform === "darwin"
				? "dylib"
				: "so";
	const packageDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
	const candidate = resolve(
		packageDir,
		"prebuilds",
		`${platform}-${process.arch}`,
		`jagfx.${extension}`,
	);

	if (existsSync(candidate)) {
		process.env.JAGFX_NATIVE_LIB = candidate;
	}
}
