package no.nav.paw.arbeidssokerregisteret.testdata

import com.nimbusds.jwt.SignedJWT
import io.ktor.http.*
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord

sealed interface TestCase {
    val forhaandsGodkjent: Boolean get() = false
    val id: String
    val person: Person?
    val configure: TestCaseBuilder.() -> Unit
    val producesHttpResponse: HttpStatusCode
    val producesError: FeilV2?
    fun producesRecord(kafkaKeysClient: KafkaKeysClient): ProducerRecord<Long, out Hendelse>?
}

class TestCaseBuilder(
    val mockOAuth2Server: MockOAuth2Server,
    val autorisasjonService: AutorisasjonService
) {
    var authToken: SignedJWT? = null
}