import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

/** Compile with the toolchain JDK; emit bytecode and API usage for the minimum supported runtime. */
internal fun Project.configureKotlinCommon(kotlin: KotlinProjectExtension) {
    kotlin.jvmToolchain(libs.version("jvm-toolchain").toInt())
}

internal fun Project.configureJvmTarget(options: KotlinJvmCompilerOptions) {
    val target = libs.version("jvm-target")
    options.jvmTarget.set(JvmTarget.fromTarget(target))
    options.freeCompilerArgs.add("-Xjdk-release=$target")
}

internal fun Project.configureJvmTarget(
    target: HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>,
) {
    target.compilerOptions { configureJvmTarget(this) }
}

/**
 * Apple targets ship arm64 only: Compose Multiplatform 1.11 dropped x86_64 Apple targets.
 * The iOS minimum is pinned to `ios-minVersion` (15.0, the floor of Kotlin/Native's prebuilt
 * platform libraries), so a newer Kotlin default does not raise it silently.
 */
internal fun Project.configureIosTargets(kotlin: KotlinMultiplatformExtension) {
    val iosMinVersion = libs.version("ios-minVersion")
    kotlin.iosArm64()
    kotlin.iosSimulatorArm64()
    kotlin.targets.withType(KotlinNativeTarget::class.java).configureEach {
        if (konanTarget.family != Family.IOS) return@configureEach
        binaries.configureEach {
            freeCompilerArgs += "-Xoverride-konan-properties=minVersion.ios=$iosMinVersion"
        }
    }
}
