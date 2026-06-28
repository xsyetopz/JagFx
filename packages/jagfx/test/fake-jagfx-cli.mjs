import { readFile, writeFile } from "node:fs/promises";

const [, , ...args] = process.argv;
const minimalPatch = {
	voices: [
		{
			frequencyEnvelope: {
				waveform: "sine",
				startValue: 32768,
				endValue: 32768,
				segments: [],
			},
			amplitudeEnvelope: {
				waveform: "off",
				startValue: 0,
				endValue: 65535,
				segments: [{ duration: 65535, targetLevel: 0 }],
			},
			durationMs: 500,
		},
	],
};

if (args[0] === "to-json") {
	const outputPath = args[2];
	const json = `${JSON.stringify(minimalPatch, null, 2)}\n`;
	if (outputPath) {
		await writeFile(outputPath, json);
	} else {
		process.stdout.write(json);
	}
} else if (args[0] === "from-json") {
	const [, inputPath, outputPath] = args;
	if (!inputPath || !outputPath) {
		process.exitCode = 1;
	} else {
		await readFile(inputPath, "utf8");
		await writeFile(outputPath, Buffer.from([1, 2, 3, 4]));
	}
} else {
	const [, outputPath] = args;
	if (!outputPath) {
		process.exitCode = 1;
	} else {
		await writeFile(outputPath, Buffer.from("RIFF\x24\x00\x00\x00WAVEfmt "));
	}
}
