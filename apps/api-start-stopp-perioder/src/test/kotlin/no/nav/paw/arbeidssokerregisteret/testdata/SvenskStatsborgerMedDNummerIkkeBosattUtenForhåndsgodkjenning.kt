package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feil
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
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

object SvenskStatsborgerMedDNummerIkkeBosattUtenForhåndsgodkjenning: TestCase {
    override val id = "12345678919"
    override val person: Person = Person(
        foedsel = Foedsel("2000-03-04", 2000).list(),
        statsborgerskap = "SWE".statsborgerskap(),
        opphold = emptyList(),
        folkeregisterpersonstatus = ikkeBosatt.folkeregisterpersonstatus() + dNummer.folkeregisterpersonstatus(),
        bostedsadresse = bostedsadresse(),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList()
    )

    override val configure: TestCaseBuilder.() -> Unit = {
        authToken = mockOAuth2Server.personToken(id)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.NoContent
    override val producesError: Feil? = null

    override fun producesRecord(kafkaKeysClient: KafkaKeysClient): ProducerRecord<Long, out Hendelse>? =
        ProducerRecord(
            "any",
            runBlocking { kafkaKeysClient.getIdAndKey(id).key },
            Startet(
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
                    Opplysning.INGEN_FLYTTE_INFORMASJON,
                    Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                    Opplysning.ER_EU_EOES_STATSBORGER,
                    Opplysning.IKKE_ANSATT,
                    Opplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE,
                    Opplysning.INGEN_ADRESSE_FUNNET,
                    Opplysning.IKKE_BOSATT,
                    Opplysning.DNUMMER
                )
            )
        )
}