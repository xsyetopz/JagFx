import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("jagfx.compose")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "jagfx.js"
            }
        }
        binaries.executable()
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(projects.sharedUI)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.browser)
        }
    }
}
