package no.nav.paw.arbeidssoeker.synk.context

import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoeker.synk.config.JOB_CONFIG
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.consumer.InngangHttpConsumer
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.service.ArbeidssoekerSynkService
import no.nav.paw.arbeidssoeker.synk.test.buildMockHttpClient
import no.nav.paw.arbeidssoeker.synk.test.createTestDataSource
import no.nav.paw.arbeidssoeker.synk.utils.flywayMigrate
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.jetbrains.exposed.sql.Database
import kotlin.io.path.toPath

class TestContext {
    private val fileUrl = javaClass.getResource("/v1.csv")!!
    val filePath = fileUrl.toURI().toPath()
    private val jobConfig = loadNaisOrLocalConfiguration<JobConfig>(JOB_CONFIG)
    val arbeidssoekerSynkRepository = ArbeidssoekerSynkRepository()

    fun initArbeidssoekerSynkService(
        responseMapping: Map<String, Pair<HttpStatusCode, String>>
    ): ArbeidssoekerSynkService {
        val mockHttpClient = buildMockHttpClient(responseMapping)
        val inngangHttpConsumer = InngangHttpConsumer("http://whatever", mockHttpClient) { "dummy token" }
        return ArbeidssoekerSynkService(
            jobConfig = jobConfig,
            arbeidssoekerSynkRepository = arbeidssoekerSynkRepository,
            inngangHttpConsumer = inngangHttpConsumer
        )
    }

    fun initDatabase() {
        val dataSource = createTestDataSource()
        dataSource.flywayMigrate()
        Database.connect(dataSource)
    }
}