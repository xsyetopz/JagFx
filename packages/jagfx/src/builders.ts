import { JagFxError, waveforms } from "./errors.js";
import type {
	EchoJson,
	EnvelopeJson,
	FilterCoefficientsJson,
	FilterJson,
	FilterPhasesJson,
	LfoJson,
	LoopSegmentJson,
	PartialInputJson,
	PartialJson,
	PatchJson,
	SegmentJson,
	VoiceJson,
	Waveform,
} from "./types.js";
import { emptyArray, emptyOptions } from "./util.js";

const undefinedWaveform =
	/^undefined(?:[5-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$/u;
const builtInWaveforms = new Set<string>(waveforms);

const assertWaveform: (waveform: string) => asserts waveform is Waveform = (
	waveform,
) => {
	if (!builtInWaveforms.has(waveform) && !undefinedWaveform.test(waveform)) {
		throw new JagFxError(
			`unknown waveform: ${waveform}`,
			"JAGFX_INVALID_ARGUMENT",
		);
	}
};

const cloneSegments = (segments: readonly SegmentJson[]): SegmentJson[] =>
	segments.map(({ duration, targetLevel }) => ({ duration, targetLevel }));

const clonePartial = (partial: PartialJson): PartialJson => {
	const cloned: PartialJson = {
		amplitude: partial.amplitude,
		pitchOffsetSemitones: partial.pitchOffsetSemitones,
	};
	if (partial.delay !== undefined) {
		cloned.delay = partial.delay;
	}
	return cloned;
};

const cloneEcho = (echo: EchoJson): EchoJson => ({
	delayMilliseconds: echo.delayMilliseconds,
	feedbackPercent: echo.feedbackPercent,
});

const cloneLoop = (loop: LoopSegmentJson): LoopSegmentJson => ({
	beginMs: loop.beginMs,
	endMs: loop.endMs,
});

const cloneNumberPair = (
	values: readonly [number, number],
): [number, number] => [values[0], values[1]];

const cloneFilterPhases = (phases: FilterPhasesJson): FilterPhasesJson => ({
	baseline: [...phases.baseline],
	modulated: [...phases.modulated],
});

const cloneFilterCoefficients = (
	coefficients: FilterCoefficientsJson,
): FilterCoefficientsJson => ({
	feedforward: cloneFilterPhases(coefficients.feedforward),
	feedback: cloneFilterPhases(coefficients.feedback),
});

/** Creates a segment. */
export const createSegment = (
	duration: number,
	targetLevel: number,
): SegmentJson => ({ duration, targetLevel });

/** Creates an envelope, defaulting to waveform `off` and no segments. */
export const createEnvelope = (
	envelope: Partial<EnvelopeJson> = emptyOptions,
): EnvelopeJson => {
	const waveform = envelope.waveform ?? "off";
	assertWaveform(waveform);
	return {
		waveform,
		startValue: envelope.startValue ?? 0,
		endValue: envelope.endValue ?? 0,
		segments: cloneSegments(envelope.segments ?? emptyArray),
	};
};

const cloneEnvelope = (envelope: EnvelopeJson): EnvelopeJson =>
	createEnvelope(envelope);

/** Creates a low frequency oscillator from deep copies of its envelopes. */
export const createLfo = (
	rateEnvelope: EnvelopeJson,
	modulationDepth: EnvelopeJson,
): LfoJson => ({
	rateEnvelope: cloneEnvelope(rateEnvelope),
	modulationDepth: cloneEnvelope(modulationDepth),
});

const cloneLfo = (lfo: LfoJson): LfoJson =>
	createLfo(lfo.rateEnvelope, lfo.modulationDepth);

const cloneFilter = (filter: FilterJson): FilterJson => {
	const cloned: FilterJson = {
		poleCounts: cloneNumberPair(filter.poleCounts),
		unityGain: cloneNumberPair(filter.unityGain),
		polePhase: cloneFilterCoefficients(filter.polePhase),
		poleMagnitude: cloneFilterCoefficients(filter.poleMagnitude),
	};
	if ("modulationEnvelope" in filter) {
		cloned.modulationEnvelope = filter.modulationEnvelope
			? cloneEnvelope(filter.modulationEnvelope)
			: null;
	}
	return cloned;
};

/** Creates a partial, which needs a positive integer amplitude. */
export const createPartial = ({
	amplitude,
	pitchOffsetSemitones = 0,
	delay = 0,
}: PartialInputJson): PartialJson => {
	if (!Number.isInteger(amplitude) || amplitude < 1) {
		throw new JagFxError(
			"partial amplitude must be a positive integer.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	return { amplitude, pitchOffsetSemitones, delay };
};

/** Creates an echo, defaulting both fields to 0. */
export const createEcho = (
	echo: Partial<EchoJson> = emptyOptions,
): EchoJson => ({
	delayMilliseconds: echo.delayMilliseconds ?? 0,
	feedbackPercent: echo.feedbackPercent ?? 0,
});

/** Creates a loop region. */
export const createLoop = (beginMs = 0, endMs = 0): LoopSegmentJson => ({
	beginMs,
	endMs,
});

const cloneLayerFields = (input: Partial<VoiceJson>): Partial<VoiceJson> => {
	const {
		offsetMs = 0,
		pitchLfo,
		amplitudeLfo,
		gapOffEnvelope,
		gapOnEnvelope,
	} = input;
	const fields: Partial<VoiceJson> = {};
	if (offsetMs !== 0) {
		fields.offsetMs = offsetMs;
	}
	if (pitchLfo) {
		fields.pitchLfo = cloneLfo(pitchLfo);
	}
	if (amplitudeLfo) {
		fields.amplitudeLfo = cloneLfo(amplitudeLfo);
	}
	if (gapOffEnvelope) {
		fields.gapOffEnvelope = cloneEnvelope(gapOffEnvelope);
	}
	if (gapOnEnvelope) {
		fields.gapOnEnvelope = cloneEnvelope(gapOnEnvelope);
	}
	return fields;
};

const cloneSoundFields = (input: Partial<VoiceJson>): Partial<VoiceJson> => {
	const { partials = emptyArray, echo, filter } = input;
	const fields: Partial<VoiceJson> = {};
	if (partials && partials.length > 0) {
		fields.partials = partials.map((partial) => clonePartial(partial));
	}
	if (echo) {
		fields.echo = cloneEcho(echo);
	}
	if (filter) {
		fields.filter = cloneFilter(filter);
	}
	return fields;
};

/** Creates a voice as a deep copy of its input, omitting empty optional fields. */
export const createVoice = (voiceInput: VoiceJson): VoiceJson => {
	const input: Partial<VoiceJson> = voiceInput;
	const { frequencyEnvelope, amplitudeEnvelope, durationMs } = input;

	if (frequencyEnvelope === undefined || amplitudeEnvelope === undefined) {
		throw new JagFxError(
			"frequencyEnvelope and amplitudeEnvelope are required.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	if (typeof durationMs !== "number" || !Number.isInteger(durationMs)) {
		throw new JagFxError("durationMs is required.", "JAGFX_INVALID_ARGUMENT");
	}

	return {
		frequencyEnvelope: cloneEnvelope(frequencyEnvelope),
		amplitudeEnvelope: cloneEnvelope(amplitudeEnvelope),
		durationMs,
		...cloneLayerFields(input),
		...cloneSoundFields(input),
	};
};

/** Creates a patch of at most 10 voice slots as a deep copy of its input. */
export const createPatch = (
	patchInput: Partial<PatchJson> = emptyOptions,
): PatchJson => {
	const voices = patchInput.voices ?? emptyArray;
	if (voices.length > 10) {
		throw new JagFxError(
			"patches can contain at most 10 voice slots.",
			"JAGFX_INVALID_ARGUMENT",
		);
	}

	const patch: PatchJson = {
		voices: voices.map((voice) => (voice ? createVoice(voice) : null)),
	};
	const { loop } = patchInput;
	if (loop && (loop.beginMs !== 0 || loop.endMs !== 0)) {
		patch.loop = cloneLoop(loop);
	}

	return patch;
};
