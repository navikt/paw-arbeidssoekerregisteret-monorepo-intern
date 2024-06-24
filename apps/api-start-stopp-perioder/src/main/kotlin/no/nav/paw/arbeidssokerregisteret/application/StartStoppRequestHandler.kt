package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class StartStoppRequestHandler(
    private val hendelseTopic: String,
    private val requestValidator: RequestValidator,
    private val producer: Producer<Long, Hendelse>,
    private val kafkaKeysClient: KafkaKeysClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    context(RequestScope)
    @WithSpan
    suspend fun startArbeidssokerperiode(identitetsnummer: Identitetsnummer, erForhaandsGodkjentAvVeileder: Boolean): Either<Problem, OK> =
        coroutineScope {
            val kafkaKeysResponse = async { kafkaKeysClient.getIdAndKey(identitetsnummer.verdi) }
            val resultat = requestValidator.validerStartAvPeriodeOenske(identitetsnummer, erForhaandsGodkjentAvVeileder)
            val (id, key) = kafkaKeysResponse.await()
            val hendelse = somHendelse(id, identitetsnummer, resultat)
            val record = ProducerRecord(
                hendelseTopic,
                key,
                hendelse
            )
            producer.sendDeferred(record).await()
            resultat
        }

    context(RequestScope)
    @WithSpan
    suspend fun avsluttArbeidssokerperiode(identitetsnummer: Identitetsnummer): Either<Problem, OK> {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        val tilgangskontrollResultat = requestValidator.validerTilgang(identitetsnummer)
        val hendelse = stoppResultatSomHendelse(id, identitetsnummer, tilgangskontrollResultat)
        val record = ProducerRecord(
            hendelseTopic,
            key,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return tilgangskontrollResultat
    }

    context(RequestScope)
    suspend fun kanRegistreresSomArbeidssoker(identitetsnummer: Identitetsnummer): Either<Problem, OK> {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        val resultat = requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
        if (resultat.isLeft()) {
            val hendelse = somHendelse(id, identitetsnummer, resultat)
            val record = ProducerRecord(
                hendelseTopic,
                key,
                hendelse
            )
            val recordMetadata = producer.sendDeferred(record).await()
            logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        }
        return resultat
    }
}
