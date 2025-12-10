package no.nav.paw.arbeidssokerregisteret.testdata

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisningV2
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.application.EuEoesStatsborgerMenHarStatusIkkeBosatt
import no.nav.paw.arbeidssokerregisteret.application.IkkeBosattINorgeIHenholdTilFolkeregisterloven
import no.nav.paw.arbeidssokerregisteret.application.Under18Aar
import no.nav.paw.arbeidssokerregisteret.bostedsadresse
import no.nav.paw.arbeidssokerregisteret.dNummer
import no.nav.paw.arbeidssokerregisteret.folkeregisterpersonstatus
import no.nav.paw.arbeidssokerregisteret.ikkeBosatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.arbeidssokerregisteret.list
import no.nav.paw.arbeidssokerregisteret.personToken
import no.nav.paw.arbeidssokerregisteret.routes.apiRegel
import no.nav.paw.arbeidssokerregisteret.statsborgerskap
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.UtenlandskAdresse
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning as ApiOpplysning

data object SvenskBrukerBosattISverigeUnder18aarMedStatusIkkeBosatt : StartPeriodeTestCase {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
    val yearFormatter = DateTimeFormatter.ofPattern("yyyy")
        .withZone(ZoneId.systemDefault())
    override val id = "12345678909"
    override val person = Person(
        foedselsdato = Instant.now().let { dato ->
            Foedselsdato(
                dateFormatter.format(dato),
                yearFormatter.format(dato).toInt()
            ).list()
        },
        statsborgerskap = "SWE".statsborgerskap(),
        opphold = emptyList(),
        folkeregisterpersonstatus = folkeregisterpersonstatus(dNummer, ikkeBosatt),
        bostedsadresse = bostedsadresse(
            utenlandskAdresse = UtenlandskAdresse(landkode = "SWE")
        ),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList()
    )

    override val configure: TestCaseBuilder.() -> Unit = {
        authToken = mockOAuth2Server.personToken(id)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: FeilV2 = FeilV2(
        melding = IkkeBosattINorgeIHenholdTilFolkeregisterloven.beskrivelse,
        feilKode = FeilV2.FeilKode.AVVIST,
        aarsakTilAvvisning = AarsakTilAvvisningV2(
            regler = listOf(
                Under18Aar.apiRegel(),
                EuEoesStatsborgerMenHarStatusIkkeBosatt.apiRegel()
            ),
            detaljer = listOf(
                ApiOpplysning.ER_UNDER_18_AAR,
                ApiOpplysning.HAR_UTENLANDSK_ADRESSE,
                ApiOpplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                ApiOpplysning.ER_EU_EOES_STATSBORGER,
                ApiOpplysning.IKKE_ANSATT,
                ApiOpplysning.SAMME_SOM_INNLOGGET_BRUKER,
                ApiOpplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                ApiOpplysning.IKKE_BOSATT,
                ApiOpplysning.DNUMMER,
                ApiOpplysning.INGEN_FLYTTE_INFORMASJON,
                ApiOpplysning.IKKE_SYSTEM
            )
        )
    )

    override fun producesRecord(
        kafkaKeysClient: KafkaKeysClient
    ) = ProducerRecord(
        "any",
        runBlocking { kafkaKeysClient.getIdAndKey(id).key },
        Avvist(
            hendelseId = UUID.randomUUID(),
            id = runBlocking { kafkaKeysClient.getIdAndKey(id).id },
            identitetsnummer = id,
            metadata = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata(
                tidspunkt = Instant.now(),
                kilde = "paw-arbeidssokerregisteret-api-start-stopp-perioder",
                utfoertAv = Bruker(
                    id = id,
                    type = BrukerType.SLUTTBRUKER,
                    sikkerhetsnivaa = "idporten-loa-high"
                ),
                aarsak = "any",
                tidspunktFraKilde = null
            ),
            opplysninger = setOf(
                Opplysning.ER_UNDER_18_AAR,
                Opplysning.IKKE_BOSATT,
                Opplysning.ER_EU_EOES_STATSBORGER,
                Opplysning.HAR_UTENLANDSK_ADRESSE,
                Opplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                Opplysning.IKKE_ANSATT,
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                Opplysning.DNUMMER,
                Opplysning.INGEN_FLYTTE_INFORMASJON,
                Opplysning.IKKE_SYSTEM
            )
        )
    )
}