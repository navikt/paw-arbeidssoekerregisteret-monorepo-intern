package no.nav.paw.kafkakeygenerator.context

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.service.KafkaKeysService
import no.nav.paw.kafkakeygenerator.service.PdlService
import no.nav.paw.kafkakeygenerator.test.genererResponse
import no.nav.paw.kafkakeygenerator.test.initTestDatabase
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.pdl.PdlClient
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.util.*
import javax.sql.DataSource

data class TestContext(
    val dataSource: DataSource = initTestDatabase(),
    val pdlClient: PdlClient = pdlClient(),
    val meterRegistry: MeterRegistry = LoggingMeterRegistry(),
    val kafkaKeysRepository: KafkaKeysRepository = KafkaKeysRepository(),
    val pdlService: PdlService = PdlService(pdlClient),
    val kafkaKeysService: KafkaKeysService = KafkaKeysService(
        meterRegistry = meterRegistry,
        kafkaKeysRepository = kafkaKeysRepository,
        pdlService = pdlService
    )
) {
    init {
        Database.connect(dataSource)
        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    fun hentEllerOpprett(identitetsnummer: String): Either<Failure, ArbeidssoekerId> = runBlocking {
        kafkaKeysService.hentEllerOpprett(
            callId = CallId(UUID.randomUUID().toString()),
            identitet = Identitetsnummer(identitetsnummer)
        )
    }

    companion object {
        fun build(): TestContext = TestContext()

        fun pdlClient(): PdlClient = PdlClient(
            url = "http://mock",
            tema = "tema",
            HttpClient(MockEngine {
                genererResponse(it)
            })
        ) { "fake token" }
    }
}
