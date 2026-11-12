import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("jagfx.kmp.library")
}

val fixturePackage = "io.github.xsyetopz.jagfx.core.engine"

val generateReferenceSynths =
    tasks.register<GenerateHexFixturesTask>("generateReferenceSynths") {
        sourceDir.set(rootProject.layout.projectDirectory.dir("references/synths"))
        extension.set("synth")
        packageName.set(fixturePackage)
        objectName.set("ReferenceSynths")
        outputDir.set(layout.buildDirectory.dir("generated/fixtures/synths/commonTest/kotlin"))
    }

val generateCoverageSynths =
    tasks.register<GenerateHexFixturesTask>("generateCoverageSynths") {
        sourceDir.set(rootProject.layout.projectDirectory.dir("references/coverage"))
        extension.set("synth")
        packageName.set(fixturePackage)
        objectName.set("CoverageSynths")
        outputDir.set(layout.buildDirectory.dir("generated/fixtures/coverage/commonTest/kotlin"))
    }

val generateReferenceWavs =
    tasks.register<GenerateHexFixturesTask>("generateReferenceWavs") {
        sourceDir.set(rootProject.layout.projectDirectory.dir("references/wav"))
        extension.set("wav")
        packageName.set(fixturePackage)
        objectName.set("ReferenceWavs")
        outputDir.set(layout.buildDirectory.dir("generated/fixtures/wav/commonTest/kotlin"))
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
        commonMain.dependencies {
            api(projects.core.model)
        }
        commonTest {
            generatedKotlin.srcDir(generateReferenceSynths.flatMap { it.outputDir })
            generatedKotlin.srcDir(generateCoverageSynths.flatMap { it.outputDir })
            generatedKotlin.srcDir(generateReferenceWavs.flatMap { it.outputDir })
            dependencies {
                implementation(projects.core.format)
                implementation(kotlin("test"))
            }
        }
    }
}
