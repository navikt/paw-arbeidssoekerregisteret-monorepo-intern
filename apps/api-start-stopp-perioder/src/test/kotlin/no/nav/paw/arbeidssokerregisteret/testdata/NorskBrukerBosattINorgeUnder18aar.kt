package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisning
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegelId
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.application.Under18Aar
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Foedsel
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data object NorskBrukerBosattINorgeUnder18aar: TestCase {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
    val yearFormatter = DateTimeFormatter.ofPattern("yyyy")
        .withZone(ZoneId.systemDefault())
    override val id = "12345678909"
    override val person = Person(
        foedsel = Instant.now().let { dato ->
            Foedsel(dateFormatter.format(dato), yearFormatter.format(dato).toInt()).list()
        },
        statsborgerskap = "NOR".statsborgerskap(),
        opphold = emptyList(),
        folkeregisterpersonstatus = bosatt.folkeregisterpersonstatus(),
        bostedsadresse = bostedsadresse(
            vegadresse = Vegadresse("1201")
        ),
        innflyttingTilNorge = "2018-01-02T13:23:12".innflytting(),
        utflyttingFraNorge = "2017-01-02".utflytting()
    )

    override val configure: TestCaseBuilder.() -> Unit =  {
        authToken = mockOAuth2Server.personToken(id)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: Feil = Feil(
        melding = Under18Aar.beskrivelse,
        feilKode = Feil.FeilKode.AVVIST,
        aarsakTilAvvisning = AarsakTilAvvisning(
            beskrivelse = Under18Aar.beskrivelse,
            regel = ApiRegelId.UNDER_18_AAR,
            detaljer = listOf(
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.ER_UNDER_18_AAR,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.HAR_NORSK_ADRESSE,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.ER_NORSK_STATSBORGER,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.ER_EU_EOES_STATSBORGER,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.IKKE_ANSATT,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning.BOSATT_ETTER_FREG_LOVEN
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
                    type = BrukerType.SLUTTBRUKER
                ),
                aarsak = "any",
                tidspunktFraKilde = null
            ),
            opplysninger = setOf(
                Opplysning.ER_UNDER_18_AAR,
                Opplysning.BOSATT_ETTER_FREG_LOVEN,
                Opplysning.ER_EU_EOES_STATSBORGER,
                Opplysning.HAR_NORSK_ADRESSE,
                Opplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE,
                Opplysning.IKKE_ANSATT,
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                Opplysning.ER_NORSK_STATSBORGER
            )
        )
    )
}