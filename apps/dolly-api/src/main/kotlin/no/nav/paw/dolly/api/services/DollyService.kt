package no.nav.paw.dolly.api.services

import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.dolly.api.kafka.HendelseKafkaProducer
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.utils.buildLogger
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import java.util.UUID
import no.nav.paw.dolly.api.models.medStandardverdier
import no.nav.paw.dolly.api.models.toMetadata
import no.nav.paw.dolly.api.models.toOpplysningerOmArbeidssoeker

class DollyService(
    private val kafkaKeysClient: KafkaKeysClient,
    private val hendelseKafkaProducer: HendelseKafkaProducer,
) {
    private val logger = buildLogger

    suspend fun registrerArbeidssoeker(request: ArbeidssoekerregistreringRequest) {
        val (id, key) = kafkaKeysClient.getIdAndKey(request.identitetsnummer)
        val requestMedDefaultVerdier = request.medStandardverdier()
        val metadata = requestMedDefaultVerdier.toMetadata()
        logger.info("Sender Startet-hendelse for identitetsnummer: ${request.identitetsnummer} på key: $key")
        hendelseKafkaProducer.sendHendelse(
            key,
            Startet(
                hendelseId = UUID.randomUUID(),
                id = id,
                identitetsnummer = request.identitetsnummer,
                metadata = metadata,
                opplysninger = emptySet()
            )
        )

        logger.info("Sender OpplysningerOmArbeidssoekerMottatt-hendelse for identitetsnummer: ${request.identitetsnummer} på key: $key")
        hendelseKafkaProducer.sendHendelse(
            key,
            OpplysningerOmArbeidssoekerMottatt(
                hendelseId = UUID.randomUUID(),
                id = id,
                identitetsnummer = request.identitetsnummer,
                opplysningerOmArbeidssoeker = requestMedDefaultVerdier.toOpplysningerOmArbeidssoeker(metadata)
            )
        )
    }
}




