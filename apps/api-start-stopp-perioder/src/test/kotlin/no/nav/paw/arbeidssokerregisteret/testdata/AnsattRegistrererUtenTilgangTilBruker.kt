package no.nav.paw.arbeidssokerregisteret.testdata

import kotlinx.coroutines.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.application.regler.AnsattIkkeTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato
import no.nav.paw.pdl.graphql.generated.hentperson.Foedested
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

data object AnsattRegistrererUtenTilgangTilBruker: StartPeriodeTestCase {
    override val id = "12345678906"
    override val person = Person(
        foedselsdato = Foedselsdato("2000-03-04", 2000).list(),
        foedested = Foedested("AFG", "Kabul", "Kabul").list(),
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
        autorisasjonService.setHarTilgangTilBruker(ansatt, id, false)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: FeilV2 = FeilV2(
        melding = AnsattIkkeTilgangTilBruker.beskrivelse,
        feilKode = FeilV2.FeilKode.IKKE_TILGANG,
        aarsakTilAvvisning = null
    )

    override fun producesRecord(kafkaKeysClient: KafkaKeysClient) = ProducerRecord(
        "any",
        runBlocking { kafkaKeysClient.getIdAndKey(AnsattRegistrererIkkeEuEoesBrukerIkkeBosattUtenForhaandgodkjenning.id).key },
        Avvist(
            hendelseId = UUID.randomUUID(),
            id = runBlocking { kafkaKeysClient.getIdAndKey(
                AnsattRegistrererIkkeEuEoesBrukerIkkeBosattUtenForhaandgodkjenning.id
            ).id },
            identitetsnummer = AnsattRegistrererIkkeEuEoesBrukerIkkeBosattUtenForhaandgodkjenning.id,
            metadata = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata(
                tidspunkt = Instant.now(),
                kilde = "paw-arbeidssokerregisteret-api-start-stopp-perioder",
                utfoertAv = Bruker(
                    id = ansatt.ident,
                    type = BrukerType.VEILEDER,
                    sikkerhetsnivaa = null
                ),
                aarsak = "any",
                tidspunktFraKilde = null
            ),
            opplysninger = setOf(
                Opplysning.ANSATT_IKKE_TILGANG,
                Opplysning.TOKENX_PID_IKKE_FUNNET,
                Opplysning.IKKE_SYSTEM
            )
        )
    )
}