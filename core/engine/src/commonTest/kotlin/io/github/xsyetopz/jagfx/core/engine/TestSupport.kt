package io.github.xsyetopz.jagfx.core.engine

internal fun hexToBytes(hex: String): ByteArray =
    ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

/** SHA-256 (FIPS 180-4), for the table hashes of spec §8.2. */
internal object Sha256 {
    private const val BLOCK = 64

    private val k =
        words(
            """
            428a2f98 71374491 b5c0fbcf e9b5dba5 3956c25b 59f111f1 923f82a4 ab1c5ed5
            d807aa98 12835b01 243185be 550c7dc3 72be5d74 80deb1fe 9bdc06a7 c19bf174
            e49b69c1 efbe4786 0fc19dc6 240ca1cc 2de92c6f 4a7484aa 5cb0a9dc 76f988da
            983e5152 a831c66d b00327c8 bf597fc7 c6e00bf3 d5a79147 06ca6351 14292967
            27b70a85 2e1b2138 4d2c6dfc 53380d13 650a7354 766a0abb 81c2c92e 92722c85
            a2bfe8a1 a81a664b c24b8b70 c76c51a3 d192e819 d6990624 f40e3585 106aa070
            19a4c116 1e376c08 2748774c 34b0bcb5 391c0cb3 4ed8aa4a 5b9cca4f 682e6ff3
            748f82ee 78a5636f 84c87814 8cc70208 90befffa a4506ceb bef9a3f7 c67178f2
            """,
        )

    private val initial =
        words("6a09e667 bb67ae85 3c6ef372 a54ff53a 510e527f 9b05688c 1f83d9ab 5be0cd19")

    fun hex(message: ByteArray): String {
        val padded = ByteArray((message.size + 9 + BLOCK - 1) / BLOCK * BLOCK)
        message.copyInto(padded)
        padded[message.size] = 0x80.toByte()
        val bits = message.size.toLong() * 8
        for (i in 0 until 8) padded[padded.size - 1 - i] = (bits ushr (8 * i)).toByte()
        val h = initial.copyOf()
        val w = IntArray(BLOCK)
        for (block in 0 until padded.size / BLOCK) compress(h, w, padded, block * BLOCK)
        return h.joinToString("") { it.toUInt().toString(16).padStart(8, '0') }
    }

    private fun compress(
        h: IntArray,
        w: IntArray,
        data: ByteArray,
        offset: Int,
    ) {
        for (t in 0 until 16) {
            w[t] =
                (0 until 4).fold(0) { acc, i ->
                    (acc shl 8) or
                        (data[offset + 4 * t + i].toInt() and 0xFF)
                }
        }
        for (t in 16 until BLOCK) {
            val w15 = w[t - 15]
            val w2 = w[t - 2]
            val s0 = w15.rotateRight(7) xor w15.rotateRight(18) xor (w15 ushr 3)
            val s1 = w2.rotateRight(17) xor w2.rotateRight(19) xor (w2 ushr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }
        val v = h.copyOf()
        for (t in 0 until BLOCK) {
            val a = v[0]
            val b = v[1]
            val c = v[2]
            val d = v[3]
            val e = v[4]
            val sigma1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val choose = (e and v[5]) xor (e.inv() and v[6])
            val t1 = v[7] + sigma1 + choose + k[t] + w[t]
            val sigma0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val majority = (a and b) xor (a and c) xor (b and c)
            v.copyInto(v, destinationOffset = 1, startIndex = 0, endIndex = 7)
            v[4] = d + t1
            v[0] = t1 + sigma0 + majority
        }
        for (i in h.indices) h[i] += v[i]
    }

    private fun words(hex: String): IntArray =
        hex
            .trim()
            .split(Regex("\\s+"))
            .map { it.toLong(16).toInt() }
            .toIntArray()
}
