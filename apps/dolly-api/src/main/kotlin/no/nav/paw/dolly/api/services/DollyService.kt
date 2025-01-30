package no.nav.paw.dolly.api.services

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.dolly.api.kafka.HendelseKafkaProducer
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringResponse
import no.nav.paw.dolly.api.models.Beskrivelse
import no.nav.paw.dolly.api.models.BrukerType
import no.nav.paw.dolly.api.models.Detaljer
import no.nav.paw.dolly.api.models.hentAvsluttetMetadata
import no.nav.paw.dolly.api.utils.buildLogger
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import java.util.UUID
import no.nav.paw.dolly.api.models.medStandardverdier
import no.nav.paw.dolly.api.models.toMetadata
import no.nav.paw.dolly.api.models.toOpplysningerOmArbeidssoeker
import java.time.Instant

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

    suspend fun avsluttArbeidssoekerperiode(identitetsnummer: String) {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer)
        val metadata = hentAvsluttetMetadata()
        logger.info("Sender Avsluttet-hendelse for identitetsnummer: $identitetsnummer på key: $key")
        hendelseKafkaProducer.sendHendelse(
            key,
            Avsluttet(
                hendelseId = UUID.randomUUID(),
                id = id,
                identitetsnummer = identitetsnummer,
                metadata = metadata
            )
        )
    }

    //TODO: Implementer oppslags-api client
    suspend fun hentArbeidssoeker(identitetsnummer: String) =
        ArbeidssoekerregistreringResponse(
            identitetsnummer = identitetsnummer,
            utfoertAv = BrukerType.SYSTEM,
            kilde = "Dolly",
            aarsak = "Opprettet i Dolly",
            nuskode = "4",
            utdanningBestaatt = true,
            utdanningGodkjent = true,
            jobbsituasjonBeskrivelse = Beskrivelse.HAR_BLITT_SAGT_OPP,
            jobbsituasjonDetaljer = Detaljer(stillingStyrk08 = "00", stilling = "Annen stilling"),
            helsetilstandHindrerArbeid = false,
            andreForholdHindrerArbeid = false,
            registrertDato = Instant.now()
        )



    fun hentEnumMap(type: String?) = mapOf(
            "UKJENT_VERDI" to "Ukjent verdi",
            "UDEFINERT" to "Udefinert",
            "VEILEDER" to "Veileder",
            "SLUTTBRUKER" to "Sluttbruker",
            "SYSTEM" to "System",
            "HAR_SAGT_OPP" to "Har sagt opp jobben",
            "HAR_BLITT_SAGT_OPP" to "Har blitt sagt opp",
            "ER_PERMITTERT" to "Er permittert",
            "ALDRI_HATT_JOBB" to "Har aldri hatt jobb",
            "IKKE_VAERT_I_JOBB_SISTE_2_AAR" to "Har ikke vært i jobb siste 2 år",
            "AKKURAT_FULLFORT_UTDANNING" to "Har akkurat fullført utdanning",
            "VIL_BYTTE_JOBB" to "Vil bytte jobb",
            "USIKKER_JOBBSITUASJON" to "Usikker jobbsituasjon",
            "MIDLERTIDIG_JOBB" to "Har midlertidig jobb",
            "DELTIDSJOBB_VIL_MER" to "Har deltidsjobb, vil ha mer",
            "NY_JOBB" to "Har fått ny jobb",
            "KONKURS" to "Er konkurs",
            "ANNET" to "Annet"
        ).filterKeys { it.startsWith(type ?: "") }
}





