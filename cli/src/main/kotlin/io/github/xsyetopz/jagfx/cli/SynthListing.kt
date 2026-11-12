package io.github.xsyetopz.jagfx.cli

import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.format.SynthFormatException
import io.github.xsyetopz.jagfx.core.format.TracedField
import io.github.xsyetopz.jagfx.core.model.Waveform
import java.util.Locale

/**
 * Assembly-style listing of a `.synth` file: one line per field the decoder reads, in the
 * layout of the earlier editor's `inspect` command.
 *
 * @property complete `false` when the decoder rejected the input.
 */
internal class SynthListing private constructor(
    val lines: List<String>,
    val complete: Boolean,
) {
    private data class Label(
        val mnemonic: String,
        val comment: String,
    )

    companion object {
        private const val HEX_WIDTH = 18
        private const val HEX_KEPT = 15
        private const val MNEMONIC_WIDTH = 10
        private const val PERCENT = 100.0
        private const val SUMMARY = "; Parsed %d/%d bytes (%.1f%%)"

        private val lfoNames =
            mapOf("pitchLfo" to "vib", "amplitudeLfo" to "trem", "gate" to "gate")
        private val slotPath = Regex("""voices\[(\d+)](?:\.(.+))?""")
        private val segmentPath = Regex(""".*segments\[(\d+)]\.(duration|targetLevel)""")
        private val partialPath = Regex("""partials\[(\d+)]\.(amplitude|pitchOffset|delay)""")
        private val polePath =
            Regex("""filter\.(feedforward|feedback)\[(\d+)]\.(start|end)\.(phase|magnitude)""")
        private val lfoStartPath = Regex("""(pitchLfo|amplitudeLfo|gate)\.first\.waveform""")

        fun of(
            path: String,
            bytes: ByteArray,
        ): SynthListing {
            val lines = mutableListOf("; File: $path", "; Size: ${bytes.size} bytes", "")
            var end = 0

            fun print(field: TracedField) {
                headerOf(field)?.let { lines += line(field.offset, "", it) }
                val stop = (field.offset + field.size).coerceAtMost(bytes.size)
                val shown = bytes.copyOfRange(field.offset.coerceAtMost(bytes.size), stop)
                val hex = shown.joinToString(" ") { "%02X".format(it) }
                val label = labelOf(field)
                val value = field.value.toString()
                val comment = if (label.comment.isEmpty()) value else "${label.comment}=$value"
                lines += line(field.offset, hex, Label(label.mnemonic, comment))
                end = field.offset + field.size
            }

            val warnings =
                try {
                    SynthCodec.decode(bytes, ::print).warnings
                } catch (e: SynthFormatException) {
                    lines += "; ERROR at 0x%04X: %s".format(e.offset, e.message)
                    return SynthListing(lines, complete = false)
                }
            val parsed = end.coerceAtMost(bytes.size)
            val percent = parsed * PERCENT / bytes.size
            lines += String.format(Locale.ROOT, SUMMARY, parsed, bytes.size, percent)
            if (parsed < bytes.size) lines += "; Remaining: ${bytes.size - parsed} bytes unparsed"
            warnings.forEach { lines += "; Warning: $it" }
            return SynthListing(lines, complete = true)
        }

        private fun line(
            offset: Int,
            hex: String,
            label: Label,
        ): String {
            val shownHex = if (hex.length > HEX_WIDTH) hex.take(HEX_KEPT) + "..." else hex
            val comma = if (label.comment.isNotEmpty() && label.mnemonic.isNotEmpty()) ", " else ""
            return "%04X: %s %s%s%s".format(
                offset,
                shownHex.padEnd(HEX_WIDTH),
                label.mnemonic.padEnd(MNEMONIC_WIDTH),
                comma,
                label.comment,
            )
        }

        /** The line printed before [field] when it starts a voice or an LFO. */
        private fun headerOf(field: TracedField): Label? {
            val (slot, rest) = slotPath.matchEntire(field.path)?.destructured ?: return null
            val lfo = lfoStartPath.matchEntire(rest)?.groupValues?.get(1)
            return when {
                rest == "frequencyEnvelope.waveform" -> {
                    val name = Waveform(field.value).toString().lowercase()
                    Label("voice $slot", "active, wf=$name")
                }

                lfo != null -> {
                    Label(lfoNames.getValue(lfo), "present")
                }

                else -> {
                    null
                }
            }
        }

        private fun labelOf(field: TracedField): Label {
            val slotMatch = slotPath.matchEntire(field.path)
            val slot = slotMatch?.groupValues?.get(1)
            val rest = slotMatch?.groupValues?.get(2).orEmpty()
            return when {
                slot == null -> fixedLabels[field.path] ?: Label("", field.path)
                rest.isEmpty() -> Label("empty", "voice $slot")
                rest in lfoNames -> Label("", "${lfoNames.getValue(rest)}=none")
                else -> fixedLabels[rest] ?: Label("", patternComment(rest, field.value))
            }
        }

        private val fixedLabels =
            mapOf(
                "loop.begin" to Label("loop", "start"),
                "loop.end" to Label("", "end"),
                "filter" to Label("", "filt=none"),
                "echo.delay" to Label("echo", "delay"),
                "echo.feedback" to Label("", "feedback"),
                "duration" to Label("time", "dur"),
                "offset" to Label("", "start"),
                "filter.unityGain0" to Label("", "unity0"),
                "filter.unityGain1" to Label("", "unity1"),
                "filter.modulationMask" to Label("", "modmask"),
                "filter.modulationEnvelope.segmentCount" to Label("", "env_segs"),
            )

        private val envelopeSuffixes =
            listOf(
                ".waveform" to "wf",
                ".startValue" to "start",
                ".endValue" to "end",
                ".segmentCount" to "segs",
            )

        private fun patternComment(
            rest: String,
            value: Int,
        ): String =
            segmentComment(rest)
                ?: partialComment(rest, value)
                ?: poleComment(rest)
                ?: if (rest == "filter.poleCounts") {
                    "filt: ch0=${value shr NIBBLE_BITS}, ch1=${value and NIBBLE_MASK}"
                } else {
                    envelopeSuffixes.firstOrNull { rest.endsWith(it.first) }?.second ?: rest
                }

        private fun segmentComment(rest: String): String? =
            segmentPath.matchEntire(rest)?.groupValues?.let { (_, i, name) ->
                if (name == "duration") "seg$i.dur" else "seg$i.peak"
            }

        private fun partialComment(
            rest: String,
            value: Int,
        ): String? =
            partialPath.matchEntire(rest)?.groupValues?.let { (_, k, name) ->
                when {
                    name == "pitchOffset" -> "pitch"
                    name == "delay" -> "delay"
                    value == 0 -> "osc=end"
                    else -> "osc$k"
                }
            }

        private fun poleComment(rest: String): String? =
            polePath.matchEntire(rest)?.groupValues?.let { groups ->
                val channel = if (groups[1] == "feedforward") 0 else 1
                val suffix = if (groups[POINT_GROUP] == "end") "_mod" else ""
                if (groups[PART_GROUP] == "phase") {
                    "ch$channel.pole${groups[2]}.freq$suffix"
                } else {
                    "mag$suffix"
                }
            }

        private const val POINT_GROUP = 3
        private const val PART_GROUP = 4
        private const val NIBBLE_BITS = 4
        private const val NIBBLE_MASK = 0x0F
    }
}
