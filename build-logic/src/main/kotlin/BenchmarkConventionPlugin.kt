import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Render benchmarks on the JVM and the Native host target, with tasks that compare the newest
 * report against the committed `baseline/` directory or replace it.
 */
class BenchmarkConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinCommon(this)
                configureJvmTarget(jvm())
                macosArm64()
                compilerOptions {
                    allWarningsAsErrors.set(true)
                }
            }

            val baseline = layout.projectDirectory.dir("baseline")
            val reports = layout.buildDirectory.dir("reports/benchmarks/main")
            tasks.register<BenchmarkBaselineTask>("checkBenchmarkBaseline") {
                description = "Fails when the newest benchmark report regresses from the baseline."
                baselineDir.set(baseline)
                reportsDir.set(reports)
                update.set(false)
            }
            tasks.register<BenchmarkBaselineTask>("updateBenchmarkBaseline") {
                description = "Replaces the benchmark baseline with the newest report."
                baselineDir.set(baseline)
                reportsDir.set(reports)
                update.set(true)
            }
        }
}
