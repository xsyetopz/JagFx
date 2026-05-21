import { writeFile } from "node:fs/promises";

const [, , , outputPath] = process.argv;

if (!outputPath) {
	process.exitCode = 1;
} else {
	await writeFile(outputPath, Buffer.from("RIFF\x24\x00\x00\x00WAVEfmt "));
}
