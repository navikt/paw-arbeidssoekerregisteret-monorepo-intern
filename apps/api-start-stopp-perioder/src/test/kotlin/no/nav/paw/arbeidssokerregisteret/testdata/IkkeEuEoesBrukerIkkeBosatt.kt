package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisning
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegelId
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.application.IkkeBosattINorgeIHenholdTilFolkeregisterloven
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Foedsel
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.UtenlandskAdresse
import no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning as ApiOpplysning

data object IkkeEuEoesBrukerIkkeBosatt: TestCase {
    override val id = "12345678919"
    override val person = Person(
        foedsel = Foedsel("2000-03-04", 2000).list(),
        statsborgerskap = "USA".statsborgerskap(),
        opphold = ("2018-01-01" to "2018-02-01").opphold(),
        folkeregisterpersonstatus = dNummer.folkeregisterpersonstatus(),
        bostedsadresse = bostedsadresse(
            utenlandskAdresse = UtenlandskAdresse(landkode = "USA")
        ),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList()
    )

    override val configure: TestCaseBuilder.() -> Unit =  {
        authToken = mockOAuth2Server.personToken(id)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: Feil = Feil(
        melding = IkkeBosattINorgeIHenholdTilFolkeregisterloven.beskrivelse,
        feilKode = Feil.FeilKode.AVVIST,
        aarsakTilAvvisning = AarsakTilAvvisning(
            beskrivelse = IkkeBosattINorgeIHenholdTilFolkeregisterloven.beskrivelse,
            regel = ApiRegelId.IKKE_BOSATT_I_NORGE_I_HENHOLD_TIL_FOLKEREGISTERLOVEN,
            detaljer = listOf(
                ApiOpplysning.ER_OVER_18_AAR,
                ApiOpplysning.HAR_UTENLANDSK_ADRESSE,
                ApiOpplysning.INGEN_FLYTTE_INFORMASJON,
                ApiOpplysning.IKKE_ANSATT,
                ApiOpplysning.SAMME_SOM_INNLOGGET_BRUKER,
                ApiOpplysning.OPPHOLDSTILATELSE_UTGAATT,
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
                Opplysning.HAR_UTENLANDSK_ADRESSE,
                Opplysning.INGEN_FLYTTE_INFORMASJON,
                Opplysning.IKKE_ANSATT,
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.OPPHOLDSTILATELSE_UTGAATT,
                Opplysning.DNUMMER
            )
        )
    )
}