package no.nav.paw.kafkakeygenerator.vo

import java.time.Instant

data class Audit(
    val identitetsnummer: Identitetsnummer,
    val identitetStatus: IdentitetStatus,
    val detaljer: String,
    val tidspunkt: Instant = Instant.now()
)
