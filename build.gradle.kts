plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

detekt {
    source.setFrom(
        fileTree(rootDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**", ".claude/**")
        },
    )
    buildUponDefaultConfig = true
    config.setFrom("config/detekt/detekt.yml")
}

// A file tree with excludes does not walk into build directories, which other tasks write to.
fun sourceTree(pattern: String) =
    fileTree(rootDir) {
        include(pattern)
        exclude("**/build/**", "**/.gradle/**", ".kotlin/**", ".claude/**")
    }

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    // Naming the file makes it a task input, so an .editorconfig edit invalidates cached results.
    val editorConfig = rootProject.file(".editorconfig")
    kotlin {
        target(sourceTree("**/*.kt"))
        ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
    }
    kotlinGradle {
        target(sourceTree("**/*.gradle.kts"))
        ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
    }
}
