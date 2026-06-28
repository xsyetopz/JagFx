export type Waveform = "off" | "square" | "sine" | "saw" | "noise";
export interface JagFxCommand {
    command: string;
    args?: readonly string[];
}
export interface CommandOptions {
    command?: string | JagFxCommand;
    cwd?: string;
}
export interface RenderSynthToWavOptions extends CommandOptions {
    loops?: number;
}
export interface SynthToJsonOptions extends CommandOptions {
    outputPath?: string;
}
export interface SegmentJson {
    duration: number;
    targetLevel: number;
}
export interface EnvelopeJson {
    waveform: Waveform;
    startValue: number;
    endValue: number;
    segments: SegmentJson[];
}
export interface LfoJson {
    rateEnvelope: EnvelopeJson;
    modulationDepth: EnvelopeJson;
}
export interface PartialJson {
    amplitude: number;
    pitchOffsetSemitones: number;
    delay?: number;
}
export interface PartialInputJson {
    amplitude: number;
    pitchOffsetSemitones?: number;
    delay?: number;
}
export interface EchoJson {
    delayMilliseconds: number;
    feedbackPercent: number;
}
export interface FilterPhasesJson {
    baseline: number[];
    modulated: number[];
}
export interface FilterCoefficientsJson {
    feedforward: FilterPhasesJson;
    feedback: FilterPhasesJson;
}
export interface FilterJson {
    poleCounts: [number, number];
    unityGain: [number, number];
    polePhase: FilterCoefficientsJson;
    poleMagnitude: FilterCoefficientsJson;
    modulationEnvelope?: EnvelopeJson | null;
}
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
export interface LoopSegmentJson {
    beginMs: number;
    endMs: number;
}
export interface PatchJson {
    voices: Array<VoiceJson | null>;
    loop?: LoopSegmentJson | null;
}
export declare const waveforms: readonly ["off", "square", "sine", "saw", "noise"];
export declare class JagFxError extends Error {
    readonly code: string;
    constructor(message: string, code?: string);
}
export declare function isGitLfsPointer(data: Uint8Array): boolean;
export declare function assertRealSynthData(data: Uint8Array): void;
export declare function createSegment(duration: number, targetLevel: number): SegmentJson;
export declare function createEnvelope(envelope?: Partial<EnvelopeJson>): EnvelopeJson;
export declare function createLfo(rateEnvelope: EnvelopeJson, modulationDepth: EnvelopeJson): LfoJson;
export declare function createPartial({ amplitude, pitchOffsetSemitones, delay, }: PartialInputJson): PartialJson;
export declare function createEcho(echo?: Partial<EchoJson>): EchoJson;
export declare function createLoop(beginMs?: number, endMs?: number): LoopSegmentJson;
export declare function createVoice(voiceInput: VoiceJson): VoiceJson;
export declare function createPatch(patchInput?: Partial<PatchJson>): PatchJson;
export declare function readPatchJson(path: string): Promise<PatchJson>;
export declare function writePatchJson(path: string, patch: PatchJson): Promise<void>;
export declare function synthToJson(inputPath: string, options?: SynthToJsonOptions): Promise<PatchJson>;
export declare function jsonToSynth(patchOrPath: PatchJson | string, outputPath: string, options?: CommandOptions): Promise<void>;
export declare function renderPatchToWav(patchOrPath: PatchJson | string, outputPath: string, options?: RenderSynthToWavOptions): Promise<void>;
export declare function renderSynthToWav(inputPath: string, outputPath: string, options?: RenderSynthToWavOptions): Promise<void>;
