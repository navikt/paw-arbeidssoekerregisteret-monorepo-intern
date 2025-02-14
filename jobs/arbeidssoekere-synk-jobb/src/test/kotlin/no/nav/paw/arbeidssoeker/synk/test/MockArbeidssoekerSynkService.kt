package no.nav.paw.arbeidssoeker.synk.test

import io.ktor.http.*
import no.nav.paw.arbeidssoeker.synk.consumer.InngangHttpConsumer
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.service.ArbeidssoekerSynkService
import java.nio.file.Path

class MockArbeidssoekerSynkService(
    private val arbeidssoekerSynkRepository: ArbeidssoekerSynkRepository,
    var responseMapping: Map<String, Pair<HttpStatusCode, String>> = emptyMap(),
) {
    fun synkArbeidssoekere(path: Path) {
        val mockHttpClient = buildMockHttpClient(responseMapping)
        val inngangHttpConsumer = InngangHttpConsumer("http://whatever", mockHttpClient) { "dummy token" }
        val arbeidssoekerSynkService = ArbeidssoekerSynkService(arbeidssoekerSynkRepository, inngangHttpConsumer)
        arbeidssoekerSynkService.synkArbeidssoekere(path)
    }
}