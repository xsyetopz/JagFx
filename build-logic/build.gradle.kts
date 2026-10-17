plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(
        libs.versions.jvm.toolchain
            .get()
            .toInt(),
    )
}

dependencies {
    compileOnly(libs.gradle.plugin.kotlin)
    compileOnly(libs.gradle.plugin.compose.compiler)
    compileOnly(libs.gradle.plugin.compose)
    compileOnly(libs.gradle.plugin.android)
    compileOnly(libs.gradle.plugin.kover)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "jagfx.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpUi") {
            id = "jagfx.kmp.ui"
            implementationClass = "KmpUiConventionPlugin"
        }
        register("compose") {
            id = "jagfx.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("benchmark") {
            id = "jagfx.benchmark"
            implementationClass = "BenchmarkConventionPlugin"
        }
        register("jvmApplication") {
            id = "jagfx.jvm.application"
            implementationClass = "JvmApplicationConventionPlugin"
        }
    }
}
