package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisning
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegelId
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Foedsel
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.UtenlandskAdresse
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning as ApiOpplysning

object NorskStatsborgerIkkeBosattUtenForhåndsgodkjenning: TestCase {
    override val id = "12345678919"
    override val person: Person = Person(
        foedsel = Foedsel("2000-03-04", 2000).list(),
        statsborgerskap = "NOR".statsborgerskap(),
        opphold = emptyList(),
        folkeregisterpersonstatus = ikkeBosatt.folkeregisterpersonstatus(),
        bostedsadresse = bostedsadresse(
            utenlandskAdresse = UtenlandskAdresse("SWE")
        ),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = "2017-01-02".utflytting()
    )

    override val configure: TestCaseBuilder.() -> Unit = {
        authToken = mockOAuth2Server.personToken(id)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: Feil = Feil(
        melding = "any",
        feilKode = Feil.FeilKode.AVVIST,
        aarsakTilAvvisning = AarsakTilAvvisning(
            beskrivelse = "any",
            regel = ApiRegelId.NORSK_STATSBORGER_IKKE_BOSATT_I_NORGE_I_HENHOLD_TIL_FOLKEREGISTERLOVEN,
            detaljer = listOf(
                ApiOpplysning.ER_NORSK_STATSBORGER,
                ApiOpplysning.IKKE_BOSATT,
                ApiOpplysning.ER_EU_EOES_STATSBORGER,
                ApiOpplysning.ER_OVER_18_AAR,
                ApiOpplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                ApiOpplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE,
                ApiOpplysning.SAMME_SOM_INNLOGGET_BRUKER,
                ApiOpplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                ApiOpplysning.HAR_UTENLANDSK_ADRESSE,
                ApiOpplysning.IKKE_ANSATT
            )
        )
    )

    override fun producesRecord(kafkaKeysClient: KafkaKeysClient): ProducerRecord<Long, out Hendelse> =
        ProducerRecord(
            "any",
            runBlocking { kafkaKeysClient.getIdAndKey(id).key },
            Avvist(
                hendelseId = UUID.randomUUID(),
                id = runBlocking { kafkaKeysClient.getIdAndKey(
                    id
                ).id },
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
                    Opplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE,
                    Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                    Opplysning.ER_NORSK_STATSBORGER,
                    Opplysning.ER_EU_EOES_STATSBORGER,
                    Opplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                    Opplysning.IKKE_ANSATT,
                    Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                    Opplysning.HAR_UTENLANDSK_ADRESSE,
                    Opplysning.IKKE_BOSATT
                )
            )
        )
}