plugins {
    id("jagfx.jvm.application")
    application
}

dependencies {
    implementation(projects.core.format)
    implementation(projects.core.engine)
    implementation(projects.core.vorbis)
    implementation(libs.clikt)
}

application {
    applicationName = "jagfx"
    mainClass = "io.github.xsyetopz.jagfx.cli.MainKt"
    // Clikt's terminal backend loads JNA; JDK 24+ warns on undeclared native access.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    val references = rootProject.layout.projectDirectory.dir("references")
    inputs.dir(references).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("jagfx.references", references.asFile.absolutePath)
}
