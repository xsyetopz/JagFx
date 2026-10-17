import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.io.File

/**
 * Compares the newest kotlinx-benchmark report with the committed baseline, or replaces the
 * baseline with that report when [update] is set.
 *
 * A benchmark regresses when its score interval (`score ± scoreError`) lies entirely below the
 * baseline interval, so run-to-run noise does not fail the check.
 */
@UntrackedTask(because = "Benchmark results differ on every run")
abstract class BenchmarkBaselineTask : DefaultTask() {
    @get:Internal
    abstract val baselineDir: DirectoryProperty

    @get:Internal
    abstract val reportsDir: DirectoryProperty

    @get:Input
    abstract val update: Property<Boolean>

    @TaskAction
    fun run() {
        val latest =
            reportsDir
                .get()
                .asFile
                .listFiles(File::isDirectory)
                ?.maxByOrNull { it.name }
                ?: throw GradleException("No benchmark report; run `benchmark` first.")
        val reports = latest.listFiles { file -> file.extension == "json" }.orEmpty()
        val baseline = baselineDir.get().asFile
        if (update.get()) {
            baseline.mkdirs()
            reports.forEach { it.copyTo(baseline.resolve(it.name), overwrite = true) }
            logger.lifecycle("Baseline updated from ${latest.name}: ${reports.map { it.name }}")
            return
        }
        val failures =
            reports.flatMap { report ->
                val expected = baseline.resolve(report.name)
                if (!expected.isFile) {
                    listOf("${report.name}: no baseline")
                } else {
                    regressions(report.nameWithoutExtension, scores(expected), scores(report))
                }
            }
        if (failures.isNotEmpty()) {
            throw GradleException("Benchmark regression:\n" + failures.joinToString("\n"))
        }
        logger.lifecycle("No regression against the baseline in ${reports.map { it.name }}")
    }

    private fun regressions(
        target: String,
        baseline: Map<String, Score>,
        current: Map<String, Score>,
    ): List<String> =
        baseline.mapNotNull { (name, base) ->
            val now = current[name] ?: return@mapNotNull "$target $name: missing from report"
            if (now.high < base.low) "$target $name: $now < baseline $base" else null
        }

    private fun scores(file: File): Map<String, Score> {
        @Suppress("UNCHECKED_CAST")
        val runs = JsonSlurper().parse(file) as List<Map<String, Any?>>
        return runs.associate { run ->
            val metric = run["primaryMetric"] as Map<*, *>
            val error = (metric["scoreError"] as? Number)?.toDouble()?.takeUnless { it.isNaN() }
            run["benchmark"] as String to
                Score((metric["score"] as Number).toDouble(), error ?: 0.0, metric["scoreUnit"])
        }
    }

    private data class Score(
        val score: Double,
        val error: Double,
        val unit: Any?,
    ) {
        val low get() = score - error
        val high get() = score + error

        override fun toString(): String = "%.3f ± %.3f %s".format(score, error, unit)
    }
}
