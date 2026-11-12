/** `undefined<id>` names a waveform id from 5 to 255, which the game client renders as silence. */
export type Waveform =
	"off" | "square" | "sine" | "saw" | "noise" | `undefined${number}`;

/** A CLI executable with leading arguments. */
export interface JagFxCommand {
	command: string;
	args?: readonly string[];
}

/** Options that pick the CLI backend and its working directory. */
export interface CommandOptions {
	command?: string | JagFxCommand;
	cwd?: string;
}

/** Options for rendering to WAV. */
export interface RenderSynthToWavOptions extends CommandOptions {
	loops?: number;
}

/** Options for converting a synth to JSON. */
export interface SynthToJsonOptions extends CommandOptions {
	outputPath?: string;
}

/** One envelope segment. */
export interface SegmentJson {
	duration: number;
	targetLevel: number;
}

/** JSON form of an envelope. */
export interface EnvelopeJson {
	waveform: Waveform;
	startValue: number;
	endValue: number;
	segments: SegmentJson[];
}

/** JSON form of a low frequency oscillator. */
export interface LfoJson {
	rateEnvelope: EnvelopeJson;
	modulationDepth: EnvelopeJson;
}

/** JSON form of a voice partial. */
export interface PartialJson {
	amplitude: number;
	pitchOffsetSemitones: number;
	delay?: number;
}

/** Input accepted by `createPartial`. */
export interface PartialInputJson {
	amplitude: number;
	pitchOffsetSemitones?: number;
	delay?: number;
}

/** JSON form of an echo. */
export interface EchoJson {
	delayMilliseconds: number;
	feedbackPercent: number;
}

/** Baseline and modulated filter phases. */
export interface FilterPhasesJson {
	baseline: number[];
	modulated: number[];
}

/** Feedforward and feedback filter phases. */
export interface FilterCoefficientsJson {
	feedforward: FilterPhasesJson;
	feedback: FilterPhasesJson;
}

/** JSON form of a filter. */
export interface FilterJson {
	poleCounts: [number, number];
	unityGain: [number, number];
	polePhase: FilterCoefficientsJson;
	poleMagnitude: FilterCoefficientsJson;
	modulationEnvelope?: EnvelopeJson | null;
}

/** JSON form of a voice. */
export interface VoiceJson {
	frequencyEnvelope: EnvelopeJson;
	amplitudeEnvelope: EnvelopeJson;
	pitchLfo?: LfoJson | null;
	amplitudeLfo?: LfoJson | null;
	gapOffEnvelope?: EnvelopeJson | null;
	gapOnEnvelope?: EnvelopeJson | null;
	partials?: PartialJson[] | null;
	echo?: EchoJson | null;
	durationMs: number;
	offsetMs?: number;
	filter?: FilterJson | null;
}

/** JSON form of a patch loop region. */
export interface LoopSegmentJson {
	beginMs: number;
	endMs: number;
}

/** JSON form of a patch. */
export interface PatchJson {
	voices: (VoiceJson | null)[];
	loop?: LoopSegmentJson | null;
}

/** Header fields of a `.jagvorbis` sample. */
export interface SampleInfo {
	sampleRate: number;
	sampleCount: number;
	loopBegin: number;
	/** Sample after the loop region. */
	loopEnd: number;
	pingPong: boolean;
	packetCount: number;
}
