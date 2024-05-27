package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerRequest
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.api.extensions.getId
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationErrorResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResult
import no.nav.paw.arbeidssokerregisteret.domain.http.ValidationResultOk
import no.nav.paw.arbeidssokerregisteret.domain.http.validerOpplysninger
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.config.kafka.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class OpplysningerRequestHandler(
    private val hendelseTopic: String,
    private val requestValidator: RequestValidator,
    private val producer: Producer<Long, Hendelse>,
    private val kafkaKeysClient: KafkaKeysClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
