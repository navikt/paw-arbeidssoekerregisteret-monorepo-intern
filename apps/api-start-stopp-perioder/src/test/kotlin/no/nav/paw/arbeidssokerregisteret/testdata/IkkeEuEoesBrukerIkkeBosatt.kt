package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisning
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegelId
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.*
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
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning as ApiOpplysning

data object IkkeEuEoesBrukerIkkeBosatt: TestCase {
    override val id = "12345678919"
    override val person = Person(
        foedsel = Foedsel("2000-03-04", 2000).list(),
        statsborgerskap = "AFG".statsborgerskap(),
        opphold = ("2018-01-01" to null).opphold(),
        folkeregisterpersonstatus = dNummer.folkeregisterpersonstatus(),
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
        melding = "any",
        feilKode = Feil.FeilKode.AVVIST,
        aarsakTilAvvisning = AarsakTilAvvisning(
            beskrivelse = "any",
            regel = ApiRegelId.IKKE_BOSATT_I_NORGE_I_HENHOLD_TIL_FOLKEREGISTERLOVEN,
            detaljer = listOf(
                ApiOpplysning.ER_OVER_18_AAR,
                ApiOpplysning.HAR_NORSK_ADRESSE,
                ApiOpplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE,
                ApiOpplysning.IKKE_ANSATT,
                ApiOpplysning.SAMME_SOM_INNLOGGET_BRUKER,
                ApiOpplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE,
                ApiOpplysning.DNUMMER
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
                Opplysning.ER_OVER_18_AAR,
                Opplysning.HAR_NORSK_ADRESSE,
                Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE,
                Opplysning.IKKE_ANSATT,
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE,
                Opplysning.DNUMMER
            )
        )
    )
}