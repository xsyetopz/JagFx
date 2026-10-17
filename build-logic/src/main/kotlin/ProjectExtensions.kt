import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String =
    findVersion(alias).get().requiredVersion

/** `:core:format` -> `io.github.xsyetopz.jagfx.core.format` */
internal val Project.jagfxNamespace: String
    get() = "$group." + path.removePrefix(":").replace(':', '.').replace('-', '_')
