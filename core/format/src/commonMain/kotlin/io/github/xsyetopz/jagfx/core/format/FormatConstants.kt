package io.github.xsyetopz.jagfx.core.format

internal const val GIT_LFS_POINTER_PREFIX = "version https://git-lfs.github.com/spec/"

/** Spec §3.2 and §3.3. */
internal const val SMART_ONE_BYTE_LIMIT = 128
internal const val USMART16_BIAS = 32768
internal const val SMART16_BIAS = 49152
internal const val SMART16_ONE_BYTE_BIAS = 64
internal val smart16OneByteRange = -SMART16_ONE_BYTE_BIAS until SMART16_ONE_BYTE_BIAS

/** Spec §6.4. */
internal const val MIN_VOICE_BYTES = 30
internal const val LOOP_BYTES = 2 * Short.SIZE_BYTES

/** Spec §5 rule 3 and §4.3 `Mask(F)`: direction `d` uses nibble `d` of the packed bytes. */
internal const val NIBBLE_BITS = 4
internal const val NIBBLE_MASK = 0x0F
internal const val LOW_BYTE_MASK = 0xFF
