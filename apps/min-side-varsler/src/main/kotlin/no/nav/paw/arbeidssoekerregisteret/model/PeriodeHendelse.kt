package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

data class PeriodeHendelse(
    val periodeId: UUID,
    val identitetsnummer: String,
    val startetTimestamp: Instant,
    val avsluttetTimestamp: Instant? = null,
)

fun Periode.asPeriodeHendelse(): PeriodeHendelse = PeriodeHendelse(
    periodeId = id,
    identitetsnummer = identitetsnummer,
    startetTimestamp = startet.tidspunkt,
    avsluttetTimestamp = avsluttet?.tidspunkt
)
