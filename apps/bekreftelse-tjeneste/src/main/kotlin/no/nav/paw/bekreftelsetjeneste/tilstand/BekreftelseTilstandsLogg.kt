package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import org.slf4j.LoggerFactory

@JvmRecord
data class BekreftelseTilstandsLogg(
    val siste: BekreftelseTilstand,
    val tidligere: List<BekreftelseTilstand>
)

private val bekreftelseTilstandsLoggProblemerLogger = LoggerFactory.getLogger(BekreftelseTilstandsLogg::class.java)
operator fun BekreftelseTilstandsLogg.plus(bekreftelseTilstand: BekreftelseTilstand): BekreftelseTilstandsLogg =
    (tidligere + siste + bekreftelseTilstand)
        .groupBy { it::class }
        .values
        .map { gruppe ->
            if (gruppe.size == 1) gruppe.first()
            else {
                bekreftelseTilstandsLoggProblemerLogger.warn("Flere tilstander av samme type i tilstandslogg: $gruppe, beholder den nyeste")
                gruppe.maxBy { it.timestamp }
            }
        }
        .let { alle ->
            val siste = alle.maxBy { it.timestamp }
            val tidligere = alle.filterNot { it == siste }.sortedBy { it.timestamp }
            BekreftelseTilstandsLogg(siste, tidligere)
        }

inline fun <reified T : BekreftelseTilstand> BekreftelseTilstandsLogg.get(): T? =
    tidligere.filterIsInstance<T>().firstOrNull() ?: siste as? T

fun BekreftelseTilstandsLogg.asList(): NonEmptyList<BekreftelseTilstand> = nonEmptyListOf(siste) + tidligere
