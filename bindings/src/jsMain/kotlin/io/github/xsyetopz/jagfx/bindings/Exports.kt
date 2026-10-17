@file:OptIn(ExperimentalJsExport::class)

package io.github.xsyetopz.jagfx.bindings

// Kotlin/JS represents ByteArray as Int8Array, so these functions take and return Int8Array.

/** See [Bindings.renderPcm16Le]. */
@JsExport
public fun renderPcm16Le(
    synth: ByteArray,
    loops: Int,
    voiceFilter: Int,
): ByteArray = Bindings.renderPcm16Le(synth, loops, voiceFilter)

/** See [Bindings.renderWav]. */
@JsExport
public fun renderWav(
    synth: ByteArray,
    loops: Int,
): ByteArray = Bindings.renderWav(synth, loops)

/** See [Bindings.synthToJson]. */
@JsExport
public fun synthToJson(synth: ByteArray): String = Bindings.synthToJson(synth)

/** See [Bindings.jsonToSynth]. */
@JsExport
public fun jsonToSynth(json: String): ByteArray = Bindings.jsonToSynth(json)

/** See [Bindings.toneToJson]. */
@JsExport
public fun toneToJson(tone: ByteArray): String = Bindings.toneToJson(tone)

/** See [Bindings.jsonToTone]. */
@JsExport
public fun jsonToTone(json: String): ByteArray = Bindings.jsonToTone(json)

/** See [Bindings.filterToJson]. */
@JsExport
public fun filterToJson(filter: ByteArray): String = Bindings.filterToJson(filter)

/** See [Bindings.jsonToFilter]. */
@JsExport
public fun jsonToFilter(json: String): ByteArray = Bindings.jsonToFilter(json)
