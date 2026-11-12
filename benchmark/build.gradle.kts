plugins {
    id("jagfx.benchmark")
    alias(libs.plugins.kotlinx.benchmark)
}

val generateReferenceSynths =
    tasks.register<GenerateHexFixturesTask>("generateReferenceSynths") {
        sourceDir.set(rootProject.layout.projectDirectory.dir("references/synths"))
        extension.set("synth")
        packageName.set("io.github.xsyetopz.jagfx.benchmark")
        objectName.set("ReferenceSynths")
        outputDir.set(layout.buildDirectory.dir("generated/fixtures/synths/commonMain/kotlin"))
    }

kotlin {
    sourceSets {
        commonMain {
            generatedKotlin.srcDir(generateReferenceSynths.flatMap { it.outputDir })
            dependencies {
                implementation(projects.core.format)
                implementation(projects.core.engine)
                implementation(libs.kotlinx.benchmark.runtime)
            }
        }
    }
}

// Run `benchmark`, then `checkBenchmarkBaseline`; see `baseline/README.md`.
benchmark {
    targets {
        register("jvm")
        register("macosArm64")
    }
}
