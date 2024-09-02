package no.nav.paw.arbeidssokerregisteret.testdata

import io.kotest.common.runBlocking
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.*
import no.nav.paw.arbeidssokerregisteret.application.IkkeFunnet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning.*
import no.nav.paw.arbeidssokerregisteret.personToken
import no.nav.paw.arbeidssokerregisteret.routes.apiRegel
import no.nav.paw.arbeidssokerregisteret.routes.apiRegelId
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

data object HentPersonReturnererNull : TestCase {
    override val id = "12345678909"
    override val person = null

    override val configure: TestCaseBuilder.() -> Unit = {
        authToken = mockOAuth2Server.personToken(id)
    }

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.Forbidden
    override val producesError: FeilV2 = FeilV2(
        melding = IkkeFunnet.beskrivelse,
        feilKode = FeilV2.FeilKode.AVVIST,
        aarsakTilAvvisning = AarsakTilAvvisningV2(
            regler = listOf(IkkeFunnet.apiRegel()),
            detaljer = listOf(
                Opplysning.PERSON_IKKE_FUNNET,
                Opplysning.SAMME_SOM_INNLOGGET_BRUKER,
                Opplysning.IKKE_ANSATT
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
                SAMME_SOM_INNLOGGET_BRUKER,
                PERSON_IKKE_FUNNET,
                IKKE_ANSATT
            )
        )
    )
}