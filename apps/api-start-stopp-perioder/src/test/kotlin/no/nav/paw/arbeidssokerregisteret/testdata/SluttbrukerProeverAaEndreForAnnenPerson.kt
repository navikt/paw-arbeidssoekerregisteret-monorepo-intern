package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.application.regler.EndreForAnnenBruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.arbeidssokerregisteret.personToken
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

data object SluttbrukerProeverAaEndreForAnnenPerson : TestCase {
    override val id: String = "09876543211"
    override val person: Person = Person(
        foedselsdato = emptyList(),
        foedested = emptyList(),
        statsborgerskap = emptyList(),
        opphold = emptyList(),
        folkeregisterpersonstatus = emptyList(),
        bostedsadresse = emptyList(),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList()
    )
    private val autentiserBruker = "1234567890"
    override val configure: TestCaseBuilder.() -> Unit =  {
        authToken = mockOAuth2Server.personToken(autentiserBruker)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: FeilV2
        get() = FeilV2(
            melding = EndreForAnnenBruker.beskrivelse,
            feilKode = FeilV2.FeilKode.IKKE_TILGANG,
            aarsakTilAvvisning = null
        )

    override fun producesRecord(kafkaKeysClient: KafkaKeysClient): ProducerRecord<Long, out Hendelse> = ProducerRecord(
        "any",
        runBlocking { kafkaKeysClient.getIdAndKey(id).key },
        Avvist(
            hendelseId = UUID.randomUUID(),
            id = runBlocking { kafkaKeysClient.getIdAndKey(id).id },
            identitetsnummer = id,
            metadata = Metadata(
                tidspunkt = Instant.now(),
                kilde = "paw-arbeidssokerregisteret-api-start-stopp-perioder",
                utfoertAv = Bruker(
                    id = autentiserBruker,
                    type = BrukerType.SLUTTBRUKER
                ),
                aarsak = "any",
                tidspunktFraKilde = null
            ),
            opplysninger = setOf(
                Opplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER,
                Opplysning.IKKE_ANSATT
            )
        )
    )
}