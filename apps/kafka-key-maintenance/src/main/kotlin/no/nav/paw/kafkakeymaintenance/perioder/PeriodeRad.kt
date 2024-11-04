package no.nav.paw.kafkakeymaintenance.perioder

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

data class PeriodeRad(
    val periodeId: UUID,
    val identitetsnummer: String,
    val fra: Instant,
    val til: Instant?
) {
    val erAktiv: Boolean = til == null
}

fun periodeRad(periodeId: UUID, identitetsnummer: String, fra: Instant, til: Instant) = PeriodeRad(periodeId, identitetsnummer, fra, til)
fun periodeRad(periode: Periode) = PeriodeRad(periode.id, periode.identitetsnummer, periode.startet.tidspunkt, periode.avsluttet?.tidspunkt)