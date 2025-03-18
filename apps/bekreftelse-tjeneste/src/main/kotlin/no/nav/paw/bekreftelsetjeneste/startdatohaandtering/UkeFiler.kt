package no.nav.paw.bekreftelsetjeneste.startdatohaandtering

import no.nav.paw.bekreftelsetjeneste.config.StaticConfigValues
import java.nio.file.Path

fun finnFiler(mappe: Path): List<Path> {
    return (mappe
            .toFile().listFiles() ?: emptyArray())
        .asSequence()
        .filter { it.isDirectory }
        .filter { it.name.startsWith(StaticConfigValues.starterMed) }
        .flatMap { (it.listFiles() ?: emptyArray()) .toList() }
        .filter { it.name == StaticConfigValues.filenavn }
        .map { it.toPath() }
        .toList()
}