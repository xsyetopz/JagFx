import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("jagfx.kmp.library")
}

val generateReferenceSamples =
    tasks.register<GenerateHexFixturesTask>("generateReferenceSamples") {
        sourceDir.set(rootProject.layout.projectDirectory.dir("references/vorbis"))
        extension.set("jagvorbis")
        packageName.set("io.github.xsyetopz.jagfx.core.vorbis")
        objectName.set("ReferenceSamples")
        outputDir.set(layout.buildDirectory.dir("generated/fixtures/vorbis/commonTest/kotlin"))
    }

kotlin {
    // Kotlin/JS has no f32 arithmetic, so `fround` has a JS actual and one actual for the rest.
    applyDefaultHierarchyTemplate {
        common {
            group("nonJs") {
                withCompilations { it.platformType != KotlinPlatformType.js }
            }
        }
    }

    sourceSets {
        commonTest {
            generatedKotlin.srcDir(generateReferenceSamples.flatMap { it.outputDir })
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
