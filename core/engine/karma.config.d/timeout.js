/// <reference path="../karma-config.d.ts" />
// Mocha's 2 s default is shorter than the golden render tests on a CI runner; matches the
// 30 s Node timeout in KmpLibraryConventionPlugin.
config.set({ client: { mocha: { timeout: 30_000 } } });
