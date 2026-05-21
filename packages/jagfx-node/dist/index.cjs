"use strict";

const { existsSync } = require("node:fs");
const { dirname, resolve } = require("node:path");

configureNativeLibrary();
const binding = require("../build/Release/jagfx_node.node");

function renderSynthToPcm(synthData, options = {}) {
	if (!Buffer.isBuffer(synthData)) {
		throw new TypeError("synthData must be a Buffer.");
	}

	return binding.renderSynthToPcm(synthData, options);
}

module.exports = {
	renderSynthToPcm,
};

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
	const packageDir = resolve(dirname(__dirname));
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
