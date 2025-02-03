package no.nav.paw.dolly.api.services

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.dolly.api.kafka.HendelseKafkaProducer
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringResponse
import no.nav.paw.dolly.api.models.hentAvsluttetMetadata
import no.nav.paw.dolly.api.models.medStandardverdier
import no.nav.paw.dolly.api.models.toArbeidssoekerregistreringResponse
import no.nav.paw.dolly.api.models.toMetadata
import no.nav.paw.dolly.api.models.toOpplysningerOmArbeidssoeker
import no.nav.paw.dolly.api.oppslag.OppslagClient
import no.nav.paw.dolly.api.utils.buildLogger
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import java.util.*

class DollyService(
    private val kafkaKeysClient: KafkaKeysClient,
    private val oppslagClient: OppslagClient,
    private val hendelseKafkaProducer: HendelseKafkaProducer,
) {
    private val logger = buildLogger
    private fun genererHendelseId() = UUID.randomUUID()

    suspend fun registrerArbeidssoeker(request: ArbeidssoekerregistreringRequest) {
        val (id, key) = kafkaKeysClient.getIdAndKey(request.identitetsnummer)
        val requestMedDefaultVerdier = request.medStandardverdier()
        val metadata = requestMedDefaultVerdier.toMetadata()
        sendHendelse(
            key,
            request.identitetsnummer,
            Startet(
                hendelseId = genererHendelseId(),
                id = id,
                identitetsnummer = request.identitetsnummer,
                metadata = metadata,
                opplysninger = emptySet()
            )
        )

        sendHendelse(
            key,
            request.identitetsnummer,
            OpplysningerOmArbeidssoekerMottatt(
                hendelseId = genererHendelseId(),
                id = id,
                identitetsnummer = request.identitetsnummer,
                opplysningerOmArbeidssoeker = requestMedDefaultVerdier.toOpplysningerOmArbeidssoeker(metadata)
            )
        )
    }

    suspend fun avsluttArbeidssoekerperiode(identitetsnummer: String) {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer)
        val metadata = hentAvsluttetMetadata()
        sendHendelse(
            key,
            identitetsnummer,
            Avsluttet(
                hendelseId = genererHendelseId(),
                id = id,
                identitetsnummer = identitetsnummer,
                metadata = metadata
            )
        )
    }

    suspend fun hentArbeidssoekerregistrering(identitetsnummer: String): ArbeidssoekerregistreringResponse? =
        oppslagClient.hentAggregerteArbeidssoekerperioder(identitetsnummer)
            ?.toArbeidssoekerregistreringResponse(identitetsnummer)

    private fun sendHendelse(key: Long, identitetsnummer: String, event: Hendelse) {
        logger.info("Sender ${event::class.simpleName} for identitetsnummer: $identitetsnummer på key: $key")
        hendelseKafkaProducer.sendHendelse(key, event)
    }
}





