package no.nav.paw.bekreftelsetjeneste.tilstand

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import org.slf4j.LoggerFactory

@JvmRecord
data class BekreftelseTilstandsLogg(
    val siste: BekreftelseTilstandStatus,
    val tidligere: List<BekreftelseTilstandStatus>
)

private val bekreftelseTilstandsLoggProblemerLogger = LoggerFactory.getLogger(BekreftelseTilstandsLogg::class.java)
operator fun BekreftelseTilstandsLogg.plus(bekreftelseTilstandStatus: BekreftelseTilstandStatus): BekreftelseTilstandsLogg =
    (tidligere + siste + bekreftelseTilstandStatus)
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

inline fun <reified T : BekreftelseTilstandStatus> BekreftelseTilstandsLogg.get(): T? =
    tidligere.filterIsInstance<T>().firstOrNull() ?: siste as? T

fun BekreftelseTilstandsLogg.asList(): NonEmptyList<BekreftelseTilstandStatus> = nonEmptyListOf(siste) + tidligere
