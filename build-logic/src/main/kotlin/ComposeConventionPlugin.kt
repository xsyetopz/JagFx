import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Compose compiler and runtime setup shared by every module that declares composables. */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.compose")

            extensions.configure<ComposeCompilerGradlePluginExtension> {
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("compose-stability.conf"),
                )
                if (providers.gradleProperty("jagfx.composeReports").isPresent) {
                    val reports = layout.buildDirectory.dir("compose-reports")
                    reportsDestination.set(reports)
                    metricsDestination.set(reports)
                }
            }
        }
}
