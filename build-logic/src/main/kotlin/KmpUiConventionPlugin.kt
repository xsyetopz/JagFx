import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Shared Compose UI consumed by the desktop and web entry points. */
class KmpUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply(ComposeConventionPlugin::class.java)

            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinCommon(this)

                configureJvmTarget(jvm())
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser()
                    // Compose UI tests need the webpack bundle to load Skiko (CMP-4906).
                    binaries.executable()
                }

                compilerOptions {
                    allWarningsAsErrors.set(true)
                }
            }
        }
}
