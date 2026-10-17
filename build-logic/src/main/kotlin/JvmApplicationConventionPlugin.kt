import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Plain JVM application (CLI, desktop shell). */
class JvmApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<KotlinJvmProjectExtension> {
                configureKotlinCommon(this)
                compilerOptions {
                    allWarningsAsErrors.set(true)
                    configureJvmTarget(this)
                }
            }
            tasks.withType<JavaCompile>().configureEach {
                options.release.set(libs.version("jvm-target").toInt())
            }
        }
}
