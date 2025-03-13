package no.nav.paw.bekreftelsetjeneste.startdatohaandtering

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import java.nio.file.Path

interface Ukenummer
object Partallsuke: Ukenummer
object Oddetallsuke: Ukenummer
object Ukjent: Ukenummer

class OddetallPartallMap(kilde: Sequence<Pair<Identitetsnummer, Ukenummer>>) {
    private val data = kilde.toMap()

    operator fun get(identitetsnummer: Identitetsnummer): Ukenummer = data[identitetsnummer] ?: Ukjent
}

fun oddetallPartallMapFraCsvFil(
    header: Boolean,
    fil: Path,
    delimiter: String,
    identitetsnummerKolonne: Int,
    ukenummerKolonne: Int,
    partall: String,
    oddetall: String
): OddetallPartallMap =
    fil.toFile()
            .readLines()
            .asSequence()
            .let { if (header) it.drop(1) else it }
            .map { line -> line.split(delimiter) }
            .onEachIndexed { index, line -> require(line.size > maxOf(identitetsnummerKolonne, ukenummerKolonne)) {"Linje $index mangler kolone(r)" } }
            .map { line  -> line.let { it[identitetsnummerKolonne].trim() to it[ukenummerKolonne].trim() } }
            .mapIndexed { index, (identitetsnummer, ukenummer) ->
                identitetsnummer.asIdentitetsnummer() to when (ukenummer) {
                    partall -> Partallsuke
                    oddetall -> Oddetallsuke
                    else -> throw IllegalArgumentException("Linje $index: Ukjent ukenummer verdi: $ukenummer")
                }
            }.let(::OddetallPartallMap)