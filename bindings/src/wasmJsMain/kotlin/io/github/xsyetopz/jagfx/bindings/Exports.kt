@file:OptIn(ExperimentalJsExport::class, ExperimentalWasmJsInterop::class)

package io.github.xsyetopz.jagfx.bindings

import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array

/** See [Bindings.renderPcm16Le]. */
@JsExport
public fun renderPcm16Le(
    synth: Int8Array,
    loops: Int,
    voiceFilter: Int,
): Int8Array = Bindings.renderPcm16Le(synth.toByteArray(), loops, voiceFilter).toInt8Array()

/** See [Bindings.renderWav]. */
@JsExport
public fun renderWav(
    synth: Int8Array,
    loops: Int,
): Int8Array = Bindings.renderWav(synth.toByteArray(), loops).toInt8Array()

/** See [Bindings.synthToJson]. */
@JsExport
public fun synthToJson(synth: Int8Array): String = Bindings.synthToJson(synth.toByteArray())

/** See [Bindings.jsonToSynth]. */
@JsExport
public fun jsonToSynth(json: String): Int8Array = Bindings.jsonToSynth(json).toInt8Array()

/** See [Bindings.toneToJson]. */
@JsExport
public fun toneToJson(tone: Int8Array): String = Bindings.toneToJson(tone.toByteArray())

/** See [Bindings.jsonToTone]. */
@JsExport
public fun jsonToTone(json: String): Int8Array = Bindings.jsonToTone(json).toInt8Array()

/** See [Bindings.filterToJson]. */
@JsExport
public fun filterToJson(filter: Int8Array): String = Bindings.filterToJson(filter.toByteArray())

/** See [Bindings.jsonToFilter]. */
@JsExport
public fun jsonToFilter(json: String): Int8Array = Bindings.jsonToFilter(json).toInt8Array()
