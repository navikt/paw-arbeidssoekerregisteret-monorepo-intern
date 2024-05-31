package no.nav.paw.arbeidssokerregisteret.profilering

import java.net.URL
import java.time.Instant
import java.util.jar.Manifest

private const val buildTimeKey = "build-timestamp"
private const val gitShaKey = "GIT-SHA"
private const val versionKey = "Implementation-Version"
private const val moduleKey = "Arbeidssokerregisteret-Modul"
private const val nameKey = "Implementation-Title"
private const val groupKey = "Group"

fun getModuleInfo(module: String): ModuleInfo? =
    ClassLoader
        .getSystemClassLoader()
        .getResources("META-INF/MANIFEST.MF")
        .asSequence()
        .let(::getModelInfo)
        .firstOrNull { it.module == module }


fun getModelInfo(urls: Sequence<URL>): Sequence<ModuleInfo> =
    urls
        .map { it.openStream().use(::Manifest) }
        .map { it.mainAttributes }
        .filter { it.getValue(moduleKey) != null }
        .map {
            ModuleInfo(
                group = it.getValue(groupKey),
                name = it.getValue(nameKey),
                version = it.getValue(versionKey),
                module = it.getValue(moduleKey),
                buildTime = Instant.parse(it.getValue(buildTimeKey)),
                gitSha = it.getValue(gitShaKey)
            )
        }

data class ModuleInfo(
    val group: String,
    val name: String,
    val version: String,
    val module: String,
    val buildTime: Instant,
    val gitSha: String?
)