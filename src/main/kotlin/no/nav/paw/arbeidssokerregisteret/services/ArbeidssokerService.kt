package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import no.nav.paw.arbeidssokerregisteret.kafka.producers.ArbeidssokerperiodeStartProducer
import no.nav.paw.pdl.PdlClient
import java.time.Instant
import java.time.LocalDateTime

class ArbeidssokerService(
    private val pdlClient: PdlClient,
    private val arbeidssokerperiodeStartProducer: ArbeidssokerperiodeStartProducer
) {
    fun startArbeidssokerperiode(foedselsnummer: Foedselsnummer, fraOgMedDato: LocalDateTime = LocalDateTime.now()) {
        arbeidssokerperiodeStartProducer.publish(StartV1(foedselsnummer.verdi, Instant.now(), foedselsnummer.verdi, "kilde"))
    }

    fun avsluttArbeidssokerperiode(foedselsnummer: Foedselsnummer, tilOgMedDato: LocalDateTime = LocalDateTime.now()) {
        TODO()
    }
}
