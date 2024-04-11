package no.nav.paw.arbeidssokerregisteret.app.helse

import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Instant
import java.util.jar.Manifest

private const val buildTimeKey = "build-timestamp"
private const val gitShaKey = "GIT-SHA"
private const val versionKey = "Implementation-Version"
private const val moduleKey = "Arbeidssokerregisteret-Modul"
private const val nameKey = "Implementation-Title"

private val manifestLogger = LoggerFactory.getLogger("manifest")

fun getModuleInfo(module: String): ModuleInfo? =
    ClassLoader
        .getSystemClassLoader()
        .getResources("META-INF/MANIFEST.MF")
        .asSequence()
        .onEach { manifestLogger.debug("Found manifest: {}", it) }
        .let(::getModelInfo)
        .firstOrNull { it.module == module }


fun getModelInfo(urls: Sequence<URL>): Sequence<ModuleInfo> =
    urls
        .map { it.openStream().use(::Manifest) }
        .map { it.mainAttributes }
        .onEach { manifestLogger.debug("Found manifest attributes: {}", it.map { kv -> "${kv.key}=${kv.value}" }) }
        .filter { it.getValue(moduleKey) != null }
        .map {
            ModuleInfo(
                name = it.getValue(nameKey),
                version = it.getValue(versionKey),
                module = it.getValue(moduleKey),
                buildTime = Instant.parse(it.getValue(buildTimeKey)),
                gitSha = it.getValue(gitShaKey)
            )
        }

data class ModuleInfo(
    val name: String,
    val version: String,
    val module: String,
    val buildTime: Instant,
    val gitSha: String?
)