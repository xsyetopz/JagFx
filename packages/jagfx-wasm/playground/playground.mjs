import { createJagFxWasmBackend } from "../dist/index.mjs";

const synthExtensionPattern = /\.synth$/i;
const sampleRate = 22050;
const form = document.querySelector("#render-form");
const fileInput = document.querySelector("#synth-file");
const loopsInput = document.querySelector("#loops");
const voiceFilterInput = document.querySelector("#voice-filter");
const status = document.querySelector("#status");
const exampleButton = document.querySelector("#load-example");
const playButton = document.querySelector("#play");
const downloadLink = document.querySelector("#download");

let backendPromise;
let lastAudioBuffer;
let lastWavUrl;

form.addEventListener("submit", async (event) => {
	event.preventDefault();

	if (!fileInput.files?.length) {
		setStatus("Choose a .synth file first, or render the bundled example.");
		return;
	}

	const data = new Uint8Array(await fileInput.files[0].arrayBuffer());
	await render(data, fileInput.files[0].name);
});

exampleButton.addEventListener("click", async () => {
	const response = await fetch("/references/synths/cow_death.synth");
	if (!response.ok) {
		throw new Error(`Could not load example synth: HTTP ${response.status}`);
	}

	await render(new Uint8Array(await response.arrayBuffer()), "cow_death.synth");
});

playButton.addEventListener("click", () => {
	if (!lastAudioBuffer) {
		return;
	}

	const context = new AudioContext({ sampleRate });
	const source = context.createBufferSource();
	source.buffer = lastAudioBuffer;
	source.connect(context.destination);
	source.start();
});

async function render(data, label) {
	setBusy(true);
	try {
		const backend = await getBackend();
		const loops = Number.parseInt(loopsInput.value, 10);
		const voiceFilter = Number.parseInt(voiceFilterInput.value, 10);
		const startedAt = performance.now();
		const pcm = backend.renderSynthToPcm(data, { loops, voiceFilter });
		const elapsedMs = performance.now() - startedAt;

		lastAudioBuffer = pcm16LeToAudioBuffer(pcm);
		setDownload(pcm, label.replace(synthExtensionPattern, ".wav"));
		playButton.disabled = false;

		setStatus(
			[
				`Rendered ${label}`,
				`input: ${data.byteLength.toLocaleString()} bytes`,
				`output: ${pcm.byteLength.toLocaleString()} PCM bytes`,
				`duration: ${lastAudioBuffer.duration.toFixed(3)}s @ ${sampleRate} Hz`,
				`render time: ${elapsedMs.toFixed(1)}ms`,
			].join("\n"),
		);
	} catch (error) {
		console.error(error);
		setStatus(
			error instanceof Error ? (error.stack ?? error.message) : String(error),
		);
	} finally {
		setBusy(false);
	}
}

function getBackend() {
	backendPromise ??= import("/_framework/dotnet.js").then(({ dotnet }) =>
		createJagFxWasmBackend(dotnet),
	);
	return backendPromise;
}

function pcm16LeToAudioBuffer(pcm) {
	const view = new DataView(pcm.buffer, pcm.byteOffset, pcm.byteLength);
	const frameCount = Math.floor(pcm.byteLength / Int16Array.BYTES_PER_ELEMENT);
	const audioBuffer = new AudioBuffer({ length: frameCount, sampleRate });
	const channel = audioBuffer.getChannelData(0);

	for (let i = 0; i < frameCount; i++) {
		channel[i] = view.getInt16(i * Int16Array.BYTES_PER_ELEMENT, true) / 32768;
	}

	return audioBuffer;
}

function setDownload(pcm, filename) {
	if (lastWavUrl) {
		URL.revokeObjectURL(lastWavUrl);
	}

	const wav = pcm16LeToWav(pcm);
	lastWavUrl = URL.createObjectURL(new Blob([wav], { type: "audio/wav" }));
	downloadLink.href = lastWavUrl;
	downloadLink.download = filename.endsWith(".wav")
		? filename
		: `${filename}.wav`;
	downloadLink.setAttribute("aria-disabled", "false");
}

function pcm16LeToWav(pcm) {
	const headerSize = 44;
	const wav = new Uint8Array(headerSize + pcm.byteLength);
	const view = new DataView(wav.buffer);
	writeAscii(wav, 0, "RIFF");
	view.setUint32(4, wav.byteLength - 8, true);
	writeAscii(wav, 8, "WAVE");
	writeAscii(wav, 12, "fmt ");
	view.setUint32(16, 16, true);
	view.setUint16(20, 1, true);
	view.setUint16(22, 1, true);
	view.setUint32(24, sampleRate, true);
	view.setUint32(28, sampleRate * Int16Array.BYTES_PER_ELEMENT, true);
	view.setUint16(32, Int16Array.BYTES_PER_ELEMENT, true);
	view.setUint16(34, 16, true);
	writeAscii(wav, 36, "data");
	view.setUint32(40, pcm.byteLength, true);
	wav.set(pcm, headerSize);
	return wav;
}

function writeAscii(bytes, offset, text) {
	for (let i = 0; i < text.length; i++) {
		bytes[offset + i] = text.charCodeAt(i);
	}
}

function setBusy(isBusy) {
	for (const element of form.elements) {
		if (
			element instanceof HTMLButtonElement ||
			element instanceof HTMLInputElement ||
			element instanceof HTMLSelectElement
		) {
			element.disabled = isBusy;
		}
	}

	playButton.disabled = isBusy || !lastAudioBuffer;
}

function setStatus(message) {
	status.textContent = message;
}
