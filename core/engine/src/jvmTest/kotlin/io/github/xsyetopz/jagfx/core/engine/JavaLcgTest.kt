package io.github.xsyetopz.jagfx.core.engine

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaLcgTest {
    @Test
    fun matchesJavaUtilRandom() {
        for (seed in longArrayOf(
            0L,
            1L,
            -1L,
            0x5DEECE66DL,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            123456789L,
        )) {
            val expected = Random(seed)
            val actual = JavaLcg(seed)
            repeat(WaveTables.SIZE) {
                assertEquals(expected.nextInt(), actual.nextInt(), "seed $seed, call $it")
            }
        }
    }
}
