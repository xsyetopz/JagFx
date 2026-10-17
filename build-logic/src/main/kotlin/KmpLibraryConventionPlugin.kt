import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/** Platform-independent library published to every JagFx target, including JS for npm. */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinCommon(this)
                explicitApi()

                @OptIn(ExperimentalAbiValidation::class)
                abiValidation()

                configureJvmTarget(jvm())
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    namespace = jagfxNamespace
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    configureJvmTarget(this)
                    withHostTest {}
                }
                configureIosTargets(this)
                // Host target for Native render benchmarks (`:benchmark`).
                macosArm64()
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser()
                    nodejs { testTask { useMocha { timeout = NODE_TEST_TIMEOUT } } }
                }
                js {
                    browser()
                    nodejs { testTask { useMocha { timeout = NODE_TEST_TIMEOUT } } }
                }

                compilerOptions {
                    allWarningsAsErrors.set(true)
                }
            }
        }

    private companion object {
        // Mocha's 2 s default is shorter than the golden render tests under a parallel `check`.
        const val NODE_TEST_TIMEOUT = "30s"
    }
}
