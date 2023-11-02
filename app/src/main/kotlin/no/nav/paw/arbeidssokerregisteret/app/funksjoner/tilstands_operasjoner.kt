package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.streams.kstream.KStream
import java.time.Instant
import java.util.*

fun KStream<String, PeriodeTilstandV1>.oppdaterLagretPeriode(
    tilstandDbNavn: String,
    oppdaterer: (PeriodeTilstandV1) -> Operasjon
): KStream<String, PeriodeTilstandV1> {
    val processorSupplier = { SkrivNyTilstandTilDb(tilstandDbNavn, oppdaterer) }
    return process(processorSupplier, tilstandDbNavn)
}
fun Context.avsluttPeriode(fraOgMed: Instant) = gjeldeneTilstand?.endre(
    tilOgMed = fraOgMed
)

fun Context.ingenEndring() = gjeldeneTilstand

fun startPeriode(startHendelse: Hendelse) = PeriodeTilstandV1(
    UUID.randomUUID(),
    startHendelse.identitetsnummer,
    startHendelse.metadata.tidspunkt,
    startHendelse.metadata.tidspunkt,
    null
)

fun PeriodeTilstandV1.endre(
    id: UUID = this.id,
    fødselsnummer: String = this.identitetsnummer,
    sistEndret: Instant = this.sistEndret,
    fraOgMed: Instant = this.fraOgMed,
    tilOgMed: Instant? = this.tilOgMed
): PeriodeTilstandV1 {
    return PeriodeTilstandV1(
        id,
        fødselsnummer,
        sistEndret,
        fraOgMed,
        tilOgMed
    )
}