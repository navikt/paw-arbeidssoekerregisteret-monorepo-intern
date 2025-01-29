package no.nav.paw.dolly.api.services

import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.dolly.api.kafka.HendelseKafkaProducer
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.utils.buildLogger
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import java.util.UUID
import no.nav.paw.dolly.api.models.Beskrivelse
import no.nav.paw.dolly.api.models.BrukerType
import no.nav.paw.dolly.api.models.Detaljer
import no.nav.paw.dolly.api.models.toMetadata
import no.nav.paw.dolly.api.models.toOpplysningerOmArbeidssoeker

class DollyService(
    private val kafkaKeysClient: KafkaKeysClient,
    private val hendelseKafkaProducer: HendelseKafkaProducer,
) {
    private val logger = buildLogger

    suspend fun registrerArbeidssoeker(request: ArbeidssoekerregistreringRequest) {
        logger.info("Registrerer arbeidssøker med identitetsnummer ${request.identitetsnummer}")
        val (id, key) = kafkaKeysClient.getIdAndKey(request.identitetsnummer)
        val nuskode = request.nuskode
        val erGodkjentUtdanningsnivaa = erGodkjentUtdanningsnivaa(nuskode)
        val requestMedDefaultVerdier = request.copy(
            utfoertAv = request.utfoertAv ?: BrukerType.SLUTTBRUKER,
            kilde = request.kilde ?: "Dolly",
            aarsak = request.aarsak ?: "Registrering av arbeidssøker i Dolly",
            nuskode = request.nuskode ?: "3",
            utdanningBestaatt = if (!erGodkjentUtdanningsnivaa) null else request.utdanningBestaatt ?: true,
            utdanningGodkjent = if (!erGodkjentUtdanningsnivaa) null else  request.utdanningGodkjent ?: true,
            jobbsituasjonBeskrivelse = request.jobbsituasjonBeskrivelse ?: Beskrivelse.HAR_BLITT_SAGT_OPP,
            jobbsituasjonDetaljer = request.jobbsituasjonDetaljer ?: Detaljer(stillingStyrk08 = "00", stilling = "Annen stilling"),
            helsetilstandHindrerArbeid = request.helsetilstandHindrerArbeid ?: false,
            andreForholdHindrerArbeid = request.andreForholdHindrerArbeid ?: false
        )
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

        logger.info("Arbeidssøker registrert med identitetsnummer: ${request.identitetsnummer}")
    }
}


private fun erGodkjentUtdanningsnivaa(nuskode: String?): Boolean {
    val forventerBestaattOgGodkjent = setOf("3", "4", "5", "6", "7", "8")
    if (nuskode !in forventerBestaattOgGodkjent) {
        return false
    }
    return true
}



