import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // Puts `build-logic` on the build script classpath for `GenerateHexFixturesTask`.
    id("jagfx.kmp.library") apply false
}

val generateReferenceSamples =
    tasks.register<GenerateHexFixturesTask>("generateReferenceSamples") {
        sourceDir.set(rootProject.layout.projectDirectory.dir("references/vorbis"))
        extension.set("jagvorbis")
        packageName.set("io.github.xsyetopz.jagfx.bindings")
        objectName.set("ReferenceSamples")
        outputDir.set(layout.buildDirectory.dir("generated/fixtures/vorbis/commonTest/kotlin"))
    }

// JavaScript entry points of the npm packages under `packages/`: Kotlin/JS for Node
// (`jagfx`, `jagfx-node`) and Kotlin/Wasm for browsers (`jagfx-wasm`).
kotlin {
    js {
        outputModuleName = "jagfx-bindings"
        nodejs()
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "jagfx-bindings"
        browser()
        nodejs()
        binaries.library()
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.format)
            implementation(projects.core.engine)
            implementation(projects.core.vorbis)
        }
        commonTest {
            generatedKotlin.srcDir(generateReferenceSamples.flatMap { it.outputDir })
            dependencies {
                implementation(kotlin("test"))
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

// Copies the library outputs into the npm packages, which bundle (`jagfx`, `jagfx-node`) or ship
// (`jagfx-wasm`) them. Run before the packages' `bun run build`.
val packagesDir = rootProject.layout.projectDirectory.dir("packages")
val jsLibrary = tasks.named("jsNodeProductionLibraryDistribution")
val syncJsPackages =
    mapOf(
        "syncJagfxPackage" to "jagfx",
        "syncJagfxNodePackage" to "jagfx-node",
    ).map { (task, name) ->
        tasks.register<Sync>(task) {
            from(jsLibrary) { exclude("package.json", "*.map") }
            into(packagesDir.dir("$name/kotlin"))
        }
    }
val syncWasmPackage =
    tasks.register<Sync>("syncJagfxWasmPackage") {
        from(tasks.named("wasmJsBrowserProductionLibraryDistribution")) {
            exclude("package.json", "*.map")
        }
        into(packagesDir.dir("jagfx-wasm/dist/kotlin"))
    }
tasks.register("syncNpmPackages") {
    group = "build"
    description = "Copies the Kotlin/JS and Kotlin/Wasm libraries into packages/."
    dependsOn(syncJsPackages, syncWasmPackage)
}
