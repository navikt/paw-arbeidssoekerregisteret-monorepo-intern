package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegelId
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
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

data object AnsattRegistrererIkkeEuEoesBrukerIkkeBosattMedForhaandgodkjenning: TestCase {
    override val id = "12345678906"
    override val forhaandsGodkjent: Boolean = true
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
    private val ansatt = NavAnsatt(UUID.randomUUID(), UUID.randomUUID().toString())
    override val configure: TestCaseBuilder.() -> Unit =  {
        authToken = mockOAuth2Server.ansattToken(ansatt)
        autorisasjonService.setHarTilgangTilBruker(ansatt, id, true)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.NoContent
    override val producesError: FeilV2? = null

    override fun producesRecord(
        kafkaKeysClient: KafkaKeysClient
    ) = ProducerRecord(
        "any",
        runBlocking { kafkaKeysClient.getIdAndKey(id).key },
        Startet(
            hendelseId = UUID.randomUUID(),
            id = runBlocking { kafkaKeysClient.getIdAndKey(id).id },
            identitetsnummer = id,
            metadata = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata(
                tidspunkt = Instant.now(),
                kilde = "paw-arbeidssokerregisteret-api-start-stopp-perioder",
                utfoertAv = Bruker(
                    id = ansatt.ident,
                    type = BrukerType.VEILEDER
                ),
                aarsak = "any",
                tidspunktFraKilde = null
            ),
            opplysninger = setOf(
                Opplysning.ER_OVER_18_AAR,
                Opplysning.HAR_NORSK_ADRESSE,
                Opplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES,
                Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE,
                Opplysning.ANSATT_TILGANG,
                Opplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE,
                Opplysning.DNUMMER,
                Opplysning.TOKENX_PID_IKKE_FUNNET,
                Opplysning.FORHAANDSGODKJENT_AV_ANSATT
            )
        )
    )
}