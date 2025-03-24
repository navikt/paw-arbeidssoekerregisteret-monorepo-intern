package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.TestData.bekreftelseTilgjengelig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelseHendelserTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelseHwmTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelserTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.PaaVegneAvTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.TransactionContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.getAllTopicHwms
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.getHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.writeBekreftelseRecord
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.writeHendelseRecord
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.writePaaVegneAvRecord
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class BackupDatabaseOperationsTest : StringSpec({
    initDbContainer()

    val logger = LoggerFactory.getLogger("backup-database-operations-test")

    val context = ApplicationContext(
        logger = logger,
        consumerVersion = CURRENT_VERSION,
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        hendelseConsumer = mockk(relaxed = true),
        bekreftelseConsumer = mockk(relaxed = true),
        paaVegneAvConsumer = mockk(relaxed = true),
        hendelseTopic = "hendelse-test-topic",
        bekreftelseTopic = "bekreftelse-test-topic",
        paaVegneAvTopic = "paavegneav-test-topic",
    )

    beforeTest {
        transaction {
            BekreftelseHendelserTable.deleteAll()
            BekreftelserTable.deleteAll()
            PaaVegneAvTable.deleteAll()
            BekreftelseHwmTable.deleteAll()
        }
    }

    "High Water Mark init for alle partisjoner" {
        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(3, "test-topic")

            val hwms = txContext.getAllTopicHwms("test-topic")
            hwms.size shouldBe 3
            hwms.all { it.offset == -1L } shouldBe true
            hwms.map { it.partition }.sorted() shouldBe listOf(0, 1, 2)
        }
    }

    "High Water Mark oppdateres bare når nytt offset er høyere verdi" {
        transaction {
            val txContext = TransactionContext(context, this)
            // init HWM med offset -1
            txContext.initHwm(1, "test-topic")

            // Første oppdatering skal lykkes
            txContext.updateHwm(0, 100L, "test-topic") shouldBe true
            txContext.getHwm(0, "test-topic") shouldBe 100L

            // Samme offset feiler
            txContext.updateHwm(0, 100L, "test-topic") shouldBe false
            txContext.getHwm(0, "test-topic") shouldBe 100L

            // Lavere offset skal feile
            txContext.updateHwm(0, 50L, "test-topic") shouldBe false
            txContext.getHwm(0, "test-topic") shouldBe 100L

            // Høyere offset skal lykkes
            txContext.updateHwm(0, 200L, "test-topic") shouldBe true
            txContext.getHwm(0, "test-topic") shouldBe 200L
        }
    }

    "BekreftelseHendelse records kan lagres og hentes" {
        val serializer = BekreftelseHendelseSerializer()
        val hendelse = bekreftelseTilgjengelig(id = 1234L)

        val headers = RecordHeaders().apply {
            add(RecordHeader("traceparent", "test-traceparent".toByteArray()))
        }
        val record = ConsumerRecord<Long, BekreftelseHendelse>(
            "test-topic",
            0,
            100L,
            5678L,
            hendelse
        ).apply {
            headers.forEach { header ->
                headers().add(header)
            }
        }

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.writeHendelseRecord(serializer, record)
        }

        transaction {
            val count = BekreftelseHendelserTable.selectAll().count()
            count shouldBe 1

            val storedRecord = BekreftelseHendelserTable.selectAll().first()
            storedRecord[BekreftelseHendelserTable.version] shouldBe CURRENT_VERSION
            storedRecord[BekreftelseHendelserTable.partition] shouldBe 0
            storedRecord[BekreftelseHendelserTable.offset] shouldBe 100L
            storedRecord[BekreftelseHendelserTable.recordKey] shouldBe 5678L
            storedRecord[BekreftelseHendelserTable.arbeidssoekerId] shouldBe 1234L
            storedRecord[BekreftelseHendelserTable.traceparent] shouldBe "test-traceparent"
            storedRecord[BekreftelseHendelserTable.data].toString().contains("1234") shouldBe true
        }
    }

    "Bekreftelse records blir lagret som byte array" {
        val data = "test data".toByteArray()
        val headers = RecordHeaders().apply {
            add(RecordHeader("traceparent", "test-traceparent".toByteArray()))
        }
        val record = ConsumerRecord<Long, ByteArray>(
            "test-topic",
            0,
            100L,
            5678L,
            data
        ).apply {
            headers.forEach { header ->
                headers().add(header)
            }
        }

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.writeBekreftelseRecord(record)
        }

        transaction {
            val count = BekreftelserTable.selectAll().count()
            count shouldBe 1

            val storedRecord = BekreftelserTable.selectAll().first()
            storedRecord[BekreftelserTable.version] shouldBe CURRENT_VERSION
            storedRecord[BekreftelserTable.partition] shouldBe 0
            storedRecord[BekreftelserTable.offset] shouldBe 100L
            storedRecord[BekreftelserTable.recordKey] shouldBe 5678L
            storedRecord[BekreftelserTable.traceparent] shouldBe "test-traceparent"
            storedRecord[BekreftelserTable.data] shouldBe data
        }
    }

    "PaaVegneAv records blir lagret som byte array" {
        val data = "test data".toByteArray()
        val headers = RecordHeaders().apply {
            add(RecordHeader("traceparent", "test-traceparent".toByteArray()))
        }
        val record = ConsumerRecord<Long, ByteArray>(
            "test-topic",
            0,
            100L,
            5678L,
            data
        ).apply {
            headers.forEach { header ->
                headers().add(header)
            }
        }

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.writePaaVegneAvRecord(record)
        }

        transaction {
            val count = PaaVegneAvTable.selectAll().count()
            count shouldBe 1

            val storedRecord = PaaVegneAvTable.selectAll().first()
            storedRecord[PaaVegneAvTable.version] shouldBe CURRENT_VERSION
            storedRecord[PaaVegneAvTable.partition] shouldBe 0
            storedRecord[PaaVegneAvTable.offset] shouldBe 100L
            storedRecord[PaaVegneAvTable.recordKey] shouldBe 5678L
            storedRecord[PaaVegneAvTable.traceparent] shouldBe "test-traceparent"
            storedRecord[PaaVegneAvTable.data] shouldBe data
        }
    }
})
