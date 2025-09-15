package no.nav.paw.kafkakeygenerator.model

import java.time.Instant

data class Audit(
    val identitetsnummer: Identitetsnummer,
    val tidligereArbeidssoekerId: ArbeidssoekerId,
    val identitetStatus: KafkaKeyStatus,
    val detaljer: String,
    val tidspunkt: Instant = Instant.now()
)
