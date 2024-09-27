package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.backup.database.*
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class DataFunctionsTest : FreeSpec({
    val logger = LoggerFactory.getLogger("test-logger")
    "Verify data functions" - {
        initDbContainer()
        val hendelseSerde = HendelseSerde()
        val record = ConsumerRecord<Long, Hendelse>(
            "t1",
            1,
            23,
            567,
            Startet(
                hendelseId = UUID.randomUUID(),
                id = 244L,
                identitetsnummer = "12345678901",
                metadata = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata(
                    tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                    utfoertAv = Bruker(
                        type = BrukerType.SLUTTBRUKER,
                        id = "12345678901"
                    ),
                    kilde = "unit-test",
                    aarsak = "tester",
                    tidspunktFraKilde = null
                ),
                opplysninger = setOf(
                    Opplysning.ER_OVER_18_AAR,
                    Opplysning.OPPHOERT_IDENTITET,
                    Opplysning.OPPHOLDSTILATELSE_UTGAATT
                )
            )
        ).apply {
            headers().add(
                "traceparent",
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01".toByteArray()
            )
        }
        "we can write to the log without errors" {
            transaction {
                val appCtx = ApplicationContext(
                    consumerVersion = 1,
                    logger = logger,
                    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    azureConfig = loadNaisOrLocalConfiguration("azure.toml")
                )
                txContext(appCtx)().writeRecord(hendelseSerde.serializer(), record)
            }
        }
        val recordVersion2 = ConsumerRecord(
            record.topic(),
            record.partition(),
            record.offset(),
            record.key(),
            (record.value() as Startet).copy(
                hendelseId = UUID.randomUUID(),
            )
        )
        "we can write a different version to the log without errors" {
            transaction {
                val appCtx = ApplicationContext(
                    consumerVersion = 2,
                    logger = logger,
                    meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    azureConfig = loadNaisOrLocalConfiguration("azure.toml")
                )
                txContext(appCtx)().writeRecord(hendelseSerde.serializer(), recordVersion2)
            }
        }
        "we can read a record based on id" {
            val storedData = transaction {
                getOneRecordForId(hendelseSerde.deserializer(), record.value().identitetsnummer)
            }.shouldNotBeNull()
            storedData.partition shouldBe record.partition()
            storedData.offset shouldBe record.offset()
            storedData.recordKey shouldBe record.key()
            storedData.arbeidssoekerId shouldBe record.value().id
            storedData.data shouldBe record.value()
            storedData.traceparent shouldBe record.headers().lastHeader("traceparent")
                ?.let { h -> String(h.value()) }
        }
        "we can read multiple versions from the log" {
            val storedDataV1 = ApplicationContext(
                consumerVersion = 1,
                logger = logger,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                azureConfig = loadNaisOrLocalConfiguration("azure.toml")
            ).let { appCtx ->
                transaction {
                    txContext(appCtx)().readRecord(hendelseSerde.deserializer(), record.partition(), record.offset())
                }
            }.shouldNotBeNull()
            storedDataV1.partition shouldBe record.partition()
            storedDataV1.offset shouldBe record.offset()
            storedDataV1.recordKey shouldBe record.key()
            storedDataV1.arbeidssoekerId shouldBe record.value().id
            storedDataV1.data shouldBe record.value()
            storedDataV1.traceparent shouldBe record.headers().lastHeader("traceparent")
                ?.let { h -> String(h.value()) }
            val storedDataV2 = ApplicationContext(
                consumerVersion = 2,
                logger = logger,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                azureConfig = loadNaisOrLocalConfiguration("azure.toml")
            ).let { appCtx ->
                transaction {
                    txContext(appCtx)().readRecord(hendelseSerde.deserializer(), record.partition(), record.offset())
                }
            }.shouldNotBeNull()
            storedDataV2.partition shouldBe record.partition()
            storedDataV2.offset shouldBe record.offset()
            storedDataV2.recordKey shouldBe record.key()
            storedDataV2.arbeidssoekerId shouldBe record.value().id
            storedDataV2.data shouldBe recordVersion2.value()
            storedDataV2.traceparent shouldBe null
        }
    }
})

fun PostgreSQLContainer<*>.databaseConfig(): DatabaseConfig {
    return DatabaseConfig(
        host = host,
        port = firstMappedPort,
        username = username,
        password = password,
        name = databaseName,
        jdbc = null
    )
}