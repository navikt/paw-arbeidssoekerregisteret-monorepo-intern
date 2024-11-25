package no.nav.paw.kafkakeygenerator.vo

import java.time.Instant

data class Audit(
    val identitetsnummer: Identitetsnummer,
    val tidligereArbeidssoekerId: ArbeidssoekerId,
    val identitetStatus: IdentitetStatus,
    val detaljer: String,
    val tidspunkt: Instant = Instant.now()
)
