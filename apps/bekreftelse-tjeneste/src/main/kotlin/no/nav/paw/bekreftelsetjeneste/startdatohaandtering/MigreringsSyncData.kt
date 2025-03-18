package no.nav.paw.bekreftelsetjeneste.startdatohaandtering

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import no.nav.paw.bekreftelsetjeneste.logger
import no.nav.paw.bekreftelsetjeneste.topology.actionKey
import no.nav.paw.bekreftelsetjeneste.topology.bekreftelseHentUke
import no.nav.paw.bekreftelsetjeneste.topology.generiskVerdiKey
import no.nav.paw.bekreftelsetjeneste.topology.intern
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import java.nio.file.Path

interface Ukenummer
object Partallsuke : Ukenummer
object Oddetallsuke : Ukenummer
object Ukjent : Ukenummer

interface OddetallPartallMap {
    operator fun get(identitetsnummer: Identitetsnummer): Ukenummer
}

class StatiskMapOddetallPartallMap(kilde: Sequence<Pair<Identitetsnummer, Ukenummer>>) : OddetallPartallMap {
    private val data = kilde.toMap().also { map ->
        val size = map.size
        logger.info("[Partall/Oddetal] Leste $size elementer fra CSV fil")
    }

    override operator fun get(identitetsnummer: Identitetsnummer): Ukenummer = (data[identitetsnummer] ?: Ukjent)
        .also { resultat ->
            Span.current().addEvent(
                intern,
                Attributes.of(
                actionKey,
                bekreftelseHentUke,
                generiskVerdiKey,
                    resultat::class.simpleName!!.lowercase()
                )
            )
        }
}

fun oddetallPartallMapFraCsvFil(
    header: Boolean,
    filer: List<Path>,
    delimiter: String,
    identitetsnummerKolonne: Int,
    ukenummerKolonne: Int,
    partall: String,
    oddetall: String
): OddetallPartallMap =
    filer
        .asSequence()
        .flatMap { it.toFile().readLines() }
        .let { if (header) it.drop(1) else it }
        .map { line -> line.split(delimiter) }
        .onEachIndexed { index, line ->
            require(
                line.size > maxOf(
                    identitetsnummerKolonne,
                    ukenummerKolonne
                )
            ) { "Linje $index mangler kolone(r)" }
        }
        .map { line -> line.let { it[identitetsnummerKolonne].trim() to it[ukenummerKolonne].trim() } }
        .mapIndexed { index, (identitetsnummer, ukenummer) ->
            identitetsnummer.asIdentitetsnummer() to when (ukenummer.uppercase()) {
                partall.uppercase() -> Partallsuke
                oddetall.uppercase() -> Oddetallsuke
                else -> throw IllegalArgumentException("Linje $index: Ukjent ukenummer verdi: $ukenummer")
            }
        }.let(::StatiskMapOddetallPartallMap)