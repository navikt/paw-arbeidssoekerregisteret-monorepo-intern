package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.raise.either
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Feil
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerRequest
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.api.extensions.getId
import no.nav.paw.arbeidssokerregisteret.domain.http.validering.validerOpplysninger
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

    suspend fun opprettBrukeropplysninger(requestScope: RequestScope, opplysningerRequest: OpplysningerRequest): Either<Feil, Unit> =
        either {
            val identitetsnummer = opplysningerRequest.getId()
            requestValidator.validerTilgang(requestScope, identitetsnummer)
                .mapLeft { problemer ->
                    Feil(
                        melding = problemer.first().regel.id.beskrivelse,
                        feilKode = Feil.FeilKode.IKKE_TILGANG
                    )
                }.bind()
            validerOpplysninger(opplysningerRequest.opplysningerOmArbeidssoeker)
                .mapLeft { validationErrorResult ->
                    Feil(
                        melding = validationErrorResult.message,
                        feilKode = Feil.FeilKode.FEIL_VED_LESING_AV_FORESPORSEL
                    )
                }.bind()
            val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
            val hendelse = opplysningerHendelse(requestScope, id, opplysningerRequest)
            val record = ProducerRecord(
                hendelseTopic,
                key,
                hendelse
            )
            val recordMetadata = producer.sendDeferred(record).await()
            logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        }
}
