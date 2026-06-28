import { Buffer } from "node:buffer";
export interface RenderSynthToPcmOptions {
    loops?: number;
    voiceFilter?: number;
}
export declare function renderSynthToPcm(synthData: Buffer, options?: RenderSynthToPcmOptions): Buffer;
