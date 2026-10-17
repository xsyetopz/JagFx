import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("jagfx.jvm.application")
    id("jagfx.compose")
}

dependencies {
    implementation(projects.sharedUI)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

val appIcons = rootProject.layout.projectDirectory.dir("assets")

tasks.processResources {
    from(appIcons.file("jagfx-icon.png"))
}

compose.desktop {
    application {
        mainClass = "io.github.xsyetopz.jagfx.desktop.MainKt"
        // Bundle a Temurin runtime; jpackage output from Homebrew JDKs is unreliable (CMP #3107).
        javaHome =
            javaToolchains
                .launcherFor {
                    languageVersion =
                        JavaLanguageVersion.of(
                            libs.versions.jvm.toolchain
                                .get(),
                        )
                    vendor = JvmVendorSpec.ADOPTIUM
                }.get()
                .metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "JagFx"
            packageVersion = version.toString()
            description = "Editor for Old School RuneScape .synth sound effects"
            vendor = "xsyetopz"
            macOS {
                bundleID = "io.github.xsyetopz.jagfx"
                minimumSystemVersion = "13.0"
                iconFile.set(appIcons.file("jagfx-icon.icns"))
                val entitlements = layout.projectDirectory.file("macos/entitlements.plist")
                entitlementsFile.set(entitlements)
                runtimeEntitlementsFile.set(entitlements)
            }
            windows {
                iconFile.set(appIcons.file("jagfx-icon.ico"))
                menuGroup = "JagFx"
                // Fixed so later versions upgrade the installed one instead of installing beside it.
                upgradeUuid = "2eb41d08-7eb5-45ba-bff4-020e26a1caa4"
            }
            linux {
                iconFile.set(appIcons.file("jagfx-icon.png"))
                packageName = "jagfx"
            }
        }

        buildTypes.release.proguard {
            isEnabled.set(true)
        }
    }
}
