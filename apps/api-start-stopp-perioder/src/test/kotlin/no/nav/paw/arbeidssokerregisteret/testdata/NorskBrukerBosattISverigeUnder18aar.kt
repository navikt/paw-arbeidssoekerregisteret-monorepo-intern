package no.nav.paw.arbeidssokerregisteret.testdata

import kotlinx.coroutines.runBlocking
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisningV2
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.application.IkkeBosattINorgeIHenholdTilFolkeregisterloven
import no.nav.paw.arbeidssokerregisteret.application.Under18Aar
import no.nav.paw.arbeidssokerregisteret.bostedsadresse
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
import no.nav.paw.arbeidssokerregisteret.utflytting
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

data object NorskBrukerBosattISverigeUnder18aar : StartPeriodeTestCase {
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
        statsborgerskap = "NOR".statsborgerskap(),
        opphold = emptyList(),
        folkeregisterpersonstatus = ikkeBosatt.folkeregisterpersonstatus(),
        bostedsadresse = bostedsadresse(
            utenlandskAdresse = UtenlandskAdresse(landkode = "SWE")
        ),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = "2017-01-02".utflytting()
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
                IkkeBosattINorgeIHenholdTilFolkeregisterloven.apiRegel()
            ),
            detaljer = listOf(
                ApiOpplysning.ER_UNDER_18_AAR,
                ApiOpplysning.HAR_UTENLANDSK_ADRESSE,
                ApiOpplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                ApiOpplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE,
                ApiOpplysning.ER_NORSK_STATSBORGER,
                ApiOpplysning.ER_EU_EOES_STATSBORGER,
                ApiOpplysning.IKKE_ANSATT,
                ApiOpplysning.SAMME_SOM_INNLOGGET_BRUKER,
                ApiOpplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                ApiOpplysning.IKKE_BOSATT,
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
                Opplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE,
                Opplysning.IKKE_ANSATT,
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                Opplysning.ER_NORSK_STATSBORGER,
                Opplysning.IKKE_SYSTEM
            )
        )
    )
}