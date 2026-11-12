package io.github.xsyetopz.jagfx.core.model

import kotlin.jvm.JvmInline

/** A time span or time position in milliseconds. */
@JvmInline
public value class Milliseconds(
    public val value: Int,
) {
    override fun toString(): String = "${value}ms"
}

/** A gain in 1/100 of full scale. `Percent(100)` is unity gain. */
@JvmInline
public value class Percent(
    public val value: Int,
) {
    override fun toString(): String = "$value%"
}
