package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.http.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner
import org.apache.kafka.clients.producer.internals.BuiltInPartitioner.partitionForKey
import org.apache.kafka.common.serialization.LongSerializer
import org.slf4j.LoggerFactory

class RequestHandler(
    private val hendelseTopic: String,
    private val requestValidator: RequestValidator,
    private val producer: Producer<Long, Hendelse>,
    private val kafkaKeysClient: KafkaKeysClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun calculatePartition(recordKey: Long): Int? {
        return if (hendelseTopic == "paw.arbeidssoker-hendelseslogg-beta-v9") {
            partitionForKey(LongSerializer().serialize(hendelseTopic, recordKey), 6)
        } else {
            null
        }
    }
    context(RequestScope)
    suspend fun startArbeidssokerperiode(identitetsnummer: Identitetsnummer): EndeligResultat {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        val resultat = requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
        val hendelse = somHendelse(id, identitetsnummer, resultat)
        val record = ProducerRecord(
            hendelseTopic,
            key,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return resultat
    }

    context(RequestScope)
    suspend fun avsluttArbeidssokerperiode(identitetsnummer: Identitetsnummer): TilgangskontrollResultat {
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
    suspend fun kanRegistreresSomArbeidssoker(identitetsnummer: Identitetsnummer): EndeligResultat {
        return requestValidator.validerStartAvPeriodeOenske(identitetsnummer)
    }

    context(RequestScope)
    suspend fun opprettBrukeropplysninger(opplysningerRequest: OpplysningerRequest): Either<out IkkeTilgang, out ValidationResult> {
        val identitetsnummer = opplysningerRequest.getId()

        val validerTilgangResultat = requestValidator.validerTilgang(identitetsnummer)

        if (validerTilgangResultat is IkkeTilgang) {
            return Left(validerTilgangResultat)
        }

        val validerOpplysninger = validerOpplysninger(opplysningerRequest.opplysningerOmArbeidssoeker)

        if (validerOpplysninger is ValidationErrorResult) {
            return Right(validerOpplysninger)
        }
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        val hendelse = opplysningerHendelse(id, opplysningerRequest)
        val record = ProducerRecord(
            hendelseTopic,
            key,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return Right(ValidationResultOk)
    }
}


sealed interface Either<L, R>
data class Left<L>(val value: L) : Either<L, Nothing>
data class Right<R>(val value: R) : Either<Nothing, R>
