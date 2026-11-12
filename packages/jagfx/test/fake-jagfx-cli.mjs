import { readFile, writeFile } from "node:fs/promises";

const args = process.argv.slice(2);
const minimalPatch = {
	voices: [
		{
			frequencyEnvelope: {
				waveform: "sine",
				startValue: 32_768,
				endValue: 32_768,
				segments: [],
			},
			amplitudeEnvelope: {
				waveform: "off",
				startValue: 0,
				endValue: 65_535,
				segments: [{ duration: 65_535, targetLevel: 0 }],
			},
			durationMs: 500,
		},
	],
};

/**
 * @param {string | undefined} value
 * @returns {value is string}
 */
const isPresent = (value) => value !== undefined && value !== "";

if (args[0] === "to-json") {
	const outputPath = args[2];
	const json = `${JSON.stringify(minimalPatch, null, 2)}\n`;
	if (isPresent(outputPath)) {
		await writeFile(outputPath, json);
	} else {
		process.stdout.write(json);
	}
} else if (args[0] === "from-json") {
	const [, inputPath, outputPath] = args;
	if (isPresent(inputPath) && isPresent(outputPath)) {
		await readFile(inputPath, "utf8");
		await writeFile(outputPath, Buffer.from([1, 2, 3, 4]));
	} else {
		process.exitCode = 1;
	}
} else {
	const outputPath = args[1];
	if (isPresent(outputPath)) {
		await writeFile(
			outputPath,
			Buffer.from("RIFF$\u0000\u0000\u0000WAVEfmt "),
		);
	} else {
		process.exitCode = 1;
	}
}
