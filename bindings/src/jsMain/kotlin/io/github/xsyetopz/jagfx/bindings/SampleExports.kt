@file:OptIn(ExperimentalJsExport::class)

package io.github.xsyetopz.jagfx.bindings

/** See [SampleBindings.samplePcm16Le]. */
@JsExport
public fun samplePcm16Le(sample: ByteArray): ByteArray = SampleBindings.samplePcm16Le(sample)

/** See [SampleBindings.sampleToWav]. */
@JsExport
public fun sampleToWav(sample: ByteArray): ByteArray = SampleBindings.sampleToWav(sample)

/** See [SampleBindings.sampleInfo]. */
@JsExport
public fun sampleInfo(sample: ByteArray): String = SampleBindings.sampleInfo(sample)
