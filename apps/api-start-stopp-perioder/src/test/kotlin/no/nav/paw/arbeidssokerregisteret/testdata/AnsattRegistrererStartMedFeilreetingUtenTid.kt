package no.nav.paw.arbeidssokerregisteret.testdata

import kotlinx.coroutines.runBlocking
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting
import no.nav.paw.arbeidssokerregisteret.ansattToken
import no.nav.paw.arbeidssokerregisteret.bosatt
import no.nav.paw.arbeidssokerregisteret.bostedsadresse
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.folkeregisterpersonstatus
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.list
import no.nav.paw.arbeidssokerregisteret.setHarTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.statsborgerskap
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Foedested
import no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration.ofDays
import java.time.Instant
import java.util.*

data object AnsattRegistrererStartMedFeilreetingUtenTid : StartPeriodeTestCase {
    override val id = "12345678906"
    override val forhaandsGodkjent: Boolean = false
    override val person = Person(
        foedselsdato = Foedselsdato("2000-03-04", 2000).list(),
        foedested = Foedested("NOR", "Oslo", "Oslo").list(),
        statsborgerskap = "NOR".statsborgerskap(),
        opphold = emptyList(),
        folkeregisterpersonstatus = bosatt.folkeregisterpersonstatus(),
        bostedsadresse = bostedsadresse(
            vegadresse = Vegadresse("1201")
        ),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList()
    )
    private val ansatt = NavAnsatt(UUID.randomUUID(), UUID.randomUUID().toString())
    override val configure: TestCaseBuilder.() -> Unit = {
        authToken = mockOAuth2Server.ansattToken(ansatt)
        autorisasjonService.setHarTilgangTilBruker(ansatt, id, true)
    }

    override val feilretting: Feilretting = Feilretting(
        feilType = Feilretting.FeilType.FeilTidspunkt,
        melding = "Fikk ikke registrert seg grunnet feil i Nav systemer",
        tidspunkt = null
    )

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.BadRequest
    override val producesError: FeilV2? = null

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
                    id = ansatt.ident,
                    type = BrukerType.VEILEDER,
                    sikkerhetsnivaa = null
                ),
                aarsak = "any",
                tidspunktFraKilde = TidspunktFraKilde(
                    tidspunkt = anyTime,
                    avviksType = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType.TIDSPUNKT_KORRIGERT
                )
            ),
            opplysninger = setOf(
                Opplysning.ANSATT_TILGANG,
                Opplysning.TOKENX_PID_IKKE_FUNNET,
                Opplysning.IKKE_SYSTEM,
                Opplysning.ER_FEILRETTING,
                Opplysning.UGYLDIG_FEILRETTING
            )
        )
    )
}