@file:OptIn(ExperimentalJsExport::class, ExperimentalWasmJsInterop::class)

package io.github.xsyetopz.jagfx.bindings

import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array

/** See [SampleBindings.samplePcm16Le]. */
@JsExport
public fun samplePcm16Le(sample: Int8Array): Int8Array =
    SampleBindings.samplePcm16Le(sample.toByteArray()).toInt8Array()

/** See [SampleBindings.sampleToWav]. */
@JsExport
public fun sampleToWav(sample: Int8Array): Int8Array =
    SampleBindings.sampleToWav(sample.toByteArray()).toInt8Array()

/** See [SampleBindings.sampleInfo]. */
@JsExport
public fun sampleInfo(sample: Int8Array): String = SampleBindings.sampleInfo(sample.toByteArray())
