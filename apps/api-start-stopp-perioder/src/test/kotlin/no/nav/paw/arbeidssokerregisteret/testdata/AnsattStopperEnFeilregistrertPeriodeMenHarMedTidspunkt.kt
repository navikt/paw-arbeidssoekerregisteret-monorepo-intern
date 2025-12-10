package no.nav.paw.arbeidssokerregisteret.testdata

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting
import no.nav.paw.arbeidssokerregisteret.ansattToken
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.AvvistStoppAvPeriode
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.setHarTilgangTilBruker
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

data object AnsattStopperEnFeilregistrertPeriodeMenHarMedTidspunkt : StoppPeriodeTestCase {
    override val id = "12345678906"
    private val ansatt = NavAnsatt(UUID.randomUUID(), UUID.randomUUID().toString())
    override val configure: TestCaseBuilder.() -> Unit = {
        authToken = mockOAuth2Server.ansattToken(ansatt)
        autorisasjonService.setHarTilgangTilBruker(ansatt, id, true)
    }

    override val feilretting: Feilretting = Feilretting(
        feilType = Feilretting.FeilType.Feilregistrering,
        melding = "Tastet feil i systemet",
        tidspunkt = Instant.now()
    )

    override val producesHttpResponse: HttpStatusCode = HttpStatusCode.BadRequest
    override val producesError: FeilV2? = null

    override fun producesRecord(
        kafkaKeysClient: KafkaKeysClient
    ) = ProducerRecord(
        "any",
        runBlocking { kafkaKeysClient.getIdAndKey(id).key },
        AvvistStoppAvPeriode(
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
                    tidspunkt = anyTime,//settes server-side så den kan vi ikke validere her
                    avviksType = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType.SLETTET
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