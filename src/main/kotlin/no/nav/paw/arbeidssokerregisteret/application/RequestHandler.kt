package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

class RequestHandler(
    private val hendelseTopic: String,
    private val requestValidator: RequestValidator,
    private val producer: Producer<Long, Hendelse>,
    private val kafkaKeysClient: KafkaKeysClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    context(RequestScope)
    suspend fun startArbeidssokerperiode(identitetsnummer: Identitetsnummer): EndeligResultat {
        val resultat = requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
        val hendelse = somHendelse(identitetsnummer, resultat)
        val record = ProducerRecord(
            hendelseTopic,
            kafkaKeysClient.getKey(identitetsnummer.verdi).id,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return resultat
    }

    context(RequestScope)
    suspend fun avsluttArbeidssokerperiode(identitetsnummer: Identitetsnummer): TilgangskontrollResultat {
        val tilgangskontrollResultat = requestValidator.validerTilgang(identitetsnummer)
        val hendelse = stoppResultatSomHendelse(identitetsnummer, tilgangskontrollResultat)
        val record = ProducerRecord(
            hendelseTopic,
            kafkaKeysClient.getKey(identitetsnummer.verdi).id,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return tilgangskontrollResultat
    }

    context(RequestScope)
    suspend fun kanRegistreresSomArbeidssoker(identitetsnummer: Identitetsnummer): EndeligResultat {
        return requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
    }
}
