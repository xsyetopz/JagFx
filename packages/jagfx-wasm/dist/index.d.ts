export interface RenderSynthToPcmOptions {
	loops?: number;
	voiceFilter?: number;
}

export interface JagFxWasmBackend {
	renderSynthToPcm(
		data: Uint8Array,
		options?: RenderSynthToPcmOptions,
	): Uint8Array;
}

export type DotnetFactory = () => {
	create(): Promise<{
		getConfig(): { mainAssemblyName: string };
		getAssemblyExports(assemblyName: string): Promise<Record<string, unknown>>;
	}>;
};

export declare function createJagFxWasmBackend(
	dotnetFactory: DotnetFactory,
): Promise<JagFxWasmBackend>;
