package no.nav.paw.arbeidssoeker.synk.test

import io.ktor.http.*
import no.nav.paw.arbeidssoeker.synk.config.JOB_CONFIG
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.consumer.InngangHttpConsumer
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.service.ArbeidssoekerSynkService
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import java.nio.file.Path

class MockArbeidssoekerSynkService(
    private val arbeidssoekerSynkRepository: ArbeidssoekerSynkRepository,
    private val jobConfig: JobConfig = loadNaisOrLocalConfiguration<JobConfig>(JOB_CONFIG),
    var responseMapping: Map<String, Pair<HttpStatusCode, String>> = emptyMap(),
) {
    fun synkArbeidssoekere(path: Path) {
        val mockHttpClient = buildMockHttpClient(responseMapping)
        val inngangHttpConsumer = InngangHttpConsumer("http://whatever", mockHttpClient) { "dummy token" }
        val arbeidssoekerSynkService = ArbeidssoekerSynkService(
            jobConfig = jobConfig,
            arbeidssoekerSynkRepository = arbeidssoekerSynkRepository,
            inngangHttpConsumer = inngangHttpConsumer
        )
        arbeidssoekerSynkService.synkArbeidssoekere(path)
    }
}