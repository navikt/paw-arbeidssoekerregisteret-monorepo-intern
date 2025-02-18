package no.nav.paw.arbeidssokerregisteret.testdata

import com.nimbusds.jwt.SignedJWT
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord

sealed interface TestCase  {
    fun producesRecord(kafkaKeysClient: KafkaKeysClient): ProducerRecord<Long, out Hendelse>?
    val feilretting: Feilretting? get() = null
    val configure: TestCaseBuilder.() -> Unit
    val id: String
    val producesHttpResponse: HttpStatusCode
    val producesError: FeilV2?
    val tilstand: ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand
}

sealed interface StartPeriodeTestCase: TestCase {
    val forhaandsGodkjent: Boolean get() = false
    val person: Person?
    override val tilstand: ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand get() = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STARTET
}

sealed interface StoppPeriodeTestCase: TestCase {
    override val tilstand: ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand get() = ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand.STOPPET
}

class TestCaseBuilder(
    val mockOAuth2Server: MockOAuth2Server,
    val autorisasjonService: AutorisasjonService
) {
    var authToken: SignedJWT? = null
}