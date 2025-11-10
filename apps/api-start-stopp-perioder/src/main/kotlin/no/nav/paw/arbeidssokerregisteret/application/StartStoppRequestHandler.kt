package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.felles.collection.PawNonEmptyList
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafka.producer.sendDeferred
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

    @WithSpan
    suspend fun startArbeidssokerperiode(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean,
        feilretting: Feilretting?
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> =
        coroutineScope {
            val kafkaKeysResponse = async { kafkaKeysClient.getIdAndKey(identitetsnummer.value) }
            val resultat = requestValidator.validerStartAvPeriodeOenske(
                requestScope = requestScope,
                identitetsnummer = identitetsnummer,
                erForhaandsGodkjentAvVeileder = erForhaandsGodkjentAvVeileder,
                feilretting = feilretting
            )
            val (id, key) = kafkaKeysResponse.await()
            val hendelse = somHendelse(
                requestScope = requestScope,
                id = id,
                identitetsnummer = identitetsnummer,
                resultat = resultat,
                feilretting = feilretting
            )
            val record = ProducerRecord(
                hendelseTopic,
                key,
                hendelse
            )
            producer.sendDeferred(record).await()
            resultat
        }

    @WithSpan
    suspend fun avsluttArbeidssokerperiode(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer,
        feilretting: Feilretting?
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.value)
        val tilgangskontrollResultat = requestValidator.validerRequest(
            requestScope = requestScope,
            identitetsnummer = identitetsnummer,
            feilretting = feilretting
        )

        val kanStarteResultat = try {
            requestValidator.validerStartAvPeriodeOenske(
                requestScope = requestScope,
                identitetsnummer = identitetsnummer,
                feilretting = null
            ).fold(
                ifLeft = {
                    it.first.regel.id.toAvsluttetAarsak()
                },
                ifRight = {
                    Aarsak.IngenAarsakFunnet
                }
            )
        } catch (e: Exception) {
            logger.error("Feil under validering av start av periodeønske", e)
            Aarsak.TekniskFeilUnderKalkuleringAvAarsak
        }

        val hendelse = stoppResultatSomHendelse(
            requestScope = requestScope,
            id = id,
            identitetsnummer = identitetsnummer,
            resultat = tilgangskontrollResultat,
            aarsak = kanStarteResultat,
            feilretting = feilretting
        )
        val record = ProducerRecord(
            hendelseTopic,
            key,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return tilgangskontrollResultat
    }

    suspend fun kanRegistreresSomArbeidssoker(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.value)
        val resultat = requestValidator.validerStartAvPeriodeOenske(
            requestScope = requestScope,
            identitetsnummer = identitetsnummer,
            feilretting = null
        )
        if (resultat.isLeft()) {
            val hendelse = somHendelse(
                requestScope = requestScope,
                id = id,
                identitetsnummer = identitetsnummer,
                resultat = resultat, feilretting = null
            )
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
