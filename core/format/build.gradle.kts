plugins {
    id("jagfx.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

val generateReferenceSynths by tasks.registering(GenerateHexFixturesTask::class) {
    sourceDir.set(rootProject.layout.projectDirectory.dir("references/synths"))
    extension.set("synth")
    packageName.set("io.github.xsyetopz.jagfx.core.format")
    objectName.set("ReferenceSynths")
    outputDir.set(layout.buildDirectory.dir("generated/fixtures/commonTest/kotlin"))
}

val generateReferenceJson by tasks.registering(GenerateHexFixturesTask::class) {
    sourceDir.set(rootProject.layout.projectDirectory.dir("references/json"))
    extension.set("json")
    packageName.set("io.github.xsyetopz.jagfx.core.format")
    objectName.set("ReferenceJson")
    outputDir.set(layout.buildDirectory.dir("generated/fixtures/json/commonTest/kotlin"))
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest {
            generatedKotlin.srcDir(generateReferenceSynths.flatMap { it.outputDir })
            generatedKotlin.srcDir(generateReferenceJson.flatMap { it.outputDir })
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
