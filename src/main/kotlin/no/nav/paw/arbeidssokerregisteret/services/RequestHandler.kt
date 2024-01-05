package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.domain.somHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.kafka.sendDeferred
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class RequestHandler(
    private val hendelseTopic: String,
    private val requestValidator: RequestValidator,
    private val producer: Producer<Long, Hendelse>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    context(RequestScope)
    suspend fun startArbeidssokerperiode(identitetsnummer: Identitetsnummer): Resultat {
        val resultat = requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
        val hendelse = somHendelse(identitetsnummer, resultat)
        val record = ProducerRecord(
            hendelseTopic,
            0L, //TODO integrere med kafka-keys slik at det blir riktig
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return resultat
    }

    context(RequestScope)
    suspend fun avsluttArbeidssokerperiode(identitetsnummer: Identitetsnummer): Resultat {
        TODO("Ikke støttet enda")
    }

    context(RequestScope)
    suspend fun kanRegistreresSomArbeidssoker(identitetsnummer: Identitetsnummer): Resultat {
        return requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
    }
}
