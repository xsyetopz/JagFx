package io.github.xsyetopz.jagfx.benchmark

import io.github.xsyetopz.jagfx.core.engine.Renderer
import io.github.xsyetopz.jagfx.core.format.SynthCodec
import io.github.xsyetopz.jagfx.core.model.Patch
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup

/** Render throughput over every reference synth (plan: DSP performance). */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
open class RenderBenchmark {
    private var patches: List<Patch> = emptyList()

    @Setup
    fun setUp() {
        patches =
            ReferenceSynths.hex.values.map { SynthCodec.decode(it.hexToByteArray()).patch }
    }

    /** Renders all reference synths once; returns the sample count so no work is eliminated. */
    @Benchmark
    fun renderReferenceSynths(): Int {
        var samples = 0
        for (patch in patches) samples += Renderer.render(patch).size
        return samples
    }
}
