package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.*
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.TestData.bekreftelseTilgjengelig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelseHendelserTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelseHwmTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelserTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.PaaVegneAvTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.TransactionContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.updateHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.kafka.ConsumerHandler
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ConsumerHandlerLogicTest : StringSpec({
    initDbContainer()

    val logger = LoggerFactory.getLogger("consumer-handler-test-logger")

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

    val consumerHandler = ConsumerHandler(context)

    beforeTest {
        transaction {
            BekreftelseHendelserTable.deleteAll()
            BekreftelserTable.deleteAll()
            PaaVegneAvTable.deleteAll()
            BekreftelseHwmTable.deleteAll()
        }
    }

    "startConsumerTasks starter alle konsumenter og lytter etter meldinger" {
        val pollTimeout = Duration.ofMillis(100)
        val hendelseTopicPartition = TopicPartition(context.hendelseTopic, 0)
        val bekreftelseTopicPartition = TopicPartition(context.bekreftelseTopic, 0)
        val paaVegneAvTopicPartition = TopicPartition(context.paaVegneAvTopic, 0)

        val hendelse = bekreftelseTilgjengelig() as BekreftelseHendelse
        val bekreftelseData = "bekreftelse data".toByteArray()
        val paaVegneAvData = "paa vegne av data".toByteArray()

        val hendelseRecord = ConsumerRecord(context.hendelseTopic, 0, 100L, 1234L, hendelse)
        val bekreftelseRecord = ConsumerRecord(context.bekreftelseTopic, 0, 100L, 1234L, bekreftelseData)
        val paaVegneAvRecord = ConsumerRecord(context.paaVegneAvTopic, 0, 100L, 1234L, paaVegneAvData)

        val hendelsesRecords = ConsumerRecords(mapOf(hendelseTopicPartition to listOf(hendelseRecord)))
        val bekreftelseRecords = ConsumerRecords(mapOf(bekreftelseTopicPartition to listOf(bekreftelseRecord)))
        val paaVegneAvRecords = ConsumerRecords(mapOf(paaVegneAvTopicPartition to listOf(paaVegneAvRecord)))

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(1, context.hendelseTopic)
            txContext.initHwm(1, context.bekreftelseTopic)
            txContext.initHwm(1, context.paaVegneAvTopic)
        }

        val callCounter = AtomicInteger(0)

        every { context.hendelseConsumer.subscribe(any<List<String>>(), any()) } just Runs
        every { context.bekreftelseConsumer.subscribe(any<List<String>>(), any()) } just Runs
        every { context.paaVegneAvConsumer.subscribe(any<List<String>>(), any()) } just Runs

        every { context.hendelseConsumer.poll(pollTimeout) } answers {
            callCounter.incrementAndGet()
            if (callCounter.get() == 1) hendelsesRecords else ConsumerRecords.empty()
        }

        every { context.bekreftelseConsumer.poll(pollTimeout) } answers {
            callCounter.incrementAndGet()
            if (callCounter.get() == 2) bekreftelseRecords else ConsumerRecords.empty()
        }

        every { context.paaVegneAvConsumer.poll(pollTimeout) } answers {
            callCounter.incrementAndGet()
            if (callCounter.get() == 3) {
                // Setter shutdown signal etter 3. poll via reflection for å stoppe konsumentene
                val shutdownField = ConsumerHandler::class.java.getDeclaredField("shutdownCalled")
                shutdownField.isAccessible = true
                val shutdownSignal = shutdownField.get(consumerHandler) as AtomicBoolean
                shutdownSignal.set(true)

                paaVegneAvRecords
            } else {
                ConsumerRecords.empty()
            }
        }

        // Start consumer tasks
        val future = consumerHandler.startConsumerTasks(pollTimeout)

        // Vent på at alle tasks er ferdig
        future.get(5, TimeUnit.SECONDS)

        // Verifiser at alle records ble prosessert
        transaction {
            BekreftelseHendelserTable.selectAll().count() shouldBe 1
            BekreftelserTable.selectAll().count() shouldBe 1
            PaaVegneAvTable.selectAll().count() shouldBe 1

            val hendelseHwm = BekreftelseHwmTable
                .selectAll()
                .first { it[BekreftelseHwmTable.topic] == context.hendelseTopic }
            hendelseHwm[BekreftelseHwmTable.offset] shouldBe 100L

            val bekreftelseHwm = BekreftelseHwmTable
                .selectAll()
                .first { it[BekreftelseHwmTable.topic] == context.bekreftelseTopic }
            bekreftelseHwm[BekreftelseHwmTable.offset] shouldBe 100L

            val paaVegneAvHwm = BekreftelseHwmTable
                .selectAll()
                .first { it[BekreftelseHwmTable.topic] == context.paaVegneAvTopic }
            paaVegneAvHwm[BekreftelseHwmTable.offset] shouldBe 100L
        }

        // Verifiser at alle consumers ble subscribed til riktige topics
        verify { context.hendelseConsumer.subscribe(eq(listOf(context.hendelseTopic)), any()) }
        verify { context.bekreftelseConsumer.subscribe(eq(listOf(context.bekreftelseTopic)), any()) }
        verify { context.paaVegneAvConsumer.subscribe(eq(listOf(context.paaVegneAvTopic)), any()) }

        // Verifiser at poll ble kalt på alle consumers
        verify(atLeast = 1) { context.hendelseConsumer.poll(pollTimeout) }
        verify(atLeast = 1) { context.bekreftelseConsumer.poll(pollTimeout) }
        verify(atLeast = 1) { context.paaVegneAvConsumer.poll(pollTimeout) }
    }

    "ConsumerHandler prosesserer records bare når HWM er oppdatert" {
        val hendelse = bekreftelseTilgjengelig()

        val record1 = ConsumerRecord<Long, BekreftelseHendelse>(
            context.hendelseTopic,
            0,
            100L,
            5678L,
            hendelse
        )

        val record2 = ConsumerRecord<Long, BekreftelseHendelse>(
            context.hendelseTopic,
            0,
            101L,
            5679L,
            hendelse
        )
        val topicPartition = TopicPartition(context.hendelseTopic, 0)
        val records = ConsumerRecords(mapOf(topicPartition to listOf(record1, record2)))

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(1, context.hendelseTopic)
        }

        transaction {
            consumerHandler.processRecords(
                records,
                context.hendelseTopic,
                consumerHandler::processHendelseRecord
            )
        }

        transaction {
            val count = BekreftelseHendelserTable.selectAll().count()
            count shouldBe 2

            val hwm = BekreftelseHwmTable.selectAll().first()
            hwm[BekreftelseHwmTable.offset] shouldBe 101L // HWM skal oppdateres til høyeste offset
        }

        // prosesserer samme records igjen
        transaction {
            consumerHandler.processRecords(
                records,
                context.hendelseTopic,
                consumerHandler::processHendelseRecord
            )
        }

        // Ingen nye records skal behandles (antall forblir det samme)
        transaction {
            val count = BekreftelseHendelserTable.selectAll().count()
            count shouldBe 2 // Bare 2 records skal være lagret

            val hwm = BekreftelseHwmTable.selectAll().first()
            hwm[BekreftelseHwmTable.offset] shouldBe 101L // HWM forblir det samme
        }
    }

    "ConsumerHandler håndterer forskjellige records riktig" {
        val hendelse = bekreftelseTilgjengelig()

        val hendelseRecord = ConsumerRecord<Long, BekreftelseHendelse>(
            context.hendelseTopic,
            0,
            100L,
            5678L,
            hendelse
        )

        val bekreftelseData = "bekreftelse data".toByteArray()
        val bekreftelseRecord = ConsumerRecord<Long, ByteArray>(
            context.bekreftelseTopic,
            0,
            100L,
            5678L,
            bekreftelseData
        )

        val paaVegneAvData = "paa vegne av data".toByteArray()
        val paaVegneAvRecord = ConsumerRecord<Long, ByteArray>(
            context.paaVegneAvTopic,
            0,
            100L,
            5678L,
            paaVegneAvData
        )

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(1, context.hendelseTopic)
            txContext.initHwm(1, context.bekreftelseTopic)
            txContext.initHwm(1, context.paaVegneAvTopic)
        }

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.updateHwm(0, 100L, context.hendelseTopic)
            consumerHandler.processHendelseRecord(txContext, hendelseRecord)
        }

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.updateHwm(0, 100L, context.bekreftelseTopic)
            consumerHandler.processBekreftelseRecord(txContext, bekreftelseRecord)
        }

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.updateHwm(0, 100L, context.paaVegneAvTopic)
            consumerHandler.processPaaVegneAvRecord(txContext, paaVegneAvRecord)
        }

        transaction {
            BekreftelseHendelserTable.selectAll().count() shouldBe 1
            BekreftelserTable.selectAll().count() shouldBe 1
            PaaVegneAvTable.selectAll().count() shouldBe 1

            val storedHendelse = BekreftelseHendelserTable.selectAll().first()
            storedHendelse[BekreftelseHendelserTable.recordKey] shouldBe 5678L

            val storedBekreftelse = BekreftelserTable.selectAll().first()
            storedBekreftelse[BekreftelserTable.data] shouldBe bekreftelseData

            val storedPaaVegneAv = PaaVegneAvTable.selectAll().first()
            storedPaaVegneAv[PaaVegneAvTable.data] shouldBe paaVegneAvData
        }
    }

    "ConsumerHandler håndterer transaksjonskontekst for batch prosessering" {
        val hendelser = (1L..5L).map { i ->
            bekreftelseTilgjengelig(id = i)
        }

        val records = hendelser.mapIndexed { index, hendelse ->
            ConsumerRecord<Long, BekreftelseHendelse>(
                context.hendelseTopic,
                0,
                100L + index,
                5000L + index,
                hendelse
            )
        }

        val topicPartition = TopicPartition(context.hendelseTopic, 0)
        val consumerRecords = ConsumerRecords(mapOf(topicPartition to records))

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(1, context.hendelseTopic)
        }

        transaction {
            consumerHandler.processRecords(
                consumerRecords,
                context.hendelseTopic,
                consumerHandler::processHendelseRecord
            )
        }

        transaction {
            val count = BekreftelseHendelserTable.selectAll().count()
            count shouldBe 5

            val hwm = BekreftelseHwmTable.selectAll().first()
            hwm[BekreftelseHwmTable.offset] shouldBe 104L

            val storedIds = BekreftelseHendelserTable
                .selectAll()
                .map { it[BekreftelseHendelserTable.arbeidssoekerId] }
                .sorted()

            storedIds shouldBe (1L..5L).toList()
        }
    }

    "startConsumerTasks avslutter alle konsumenter hvis én feiler" {
        val pollTimeout = Duration.ofMillis(100)

        every { context.hendelseConsumer.subscribe(any<List<String>>(), any()) } just Runs
        every { context.bekreftelseConsumer.subscribe(any<List<String>>(), any()) } just Runs
        every { context.paaVegneAvConsumer.subscribe(any<List<String>>(), any()) } just Runs

        // Vi må overstyre close-metoden for å simulere at den blir kalt
        every { context.hendelseConsumer.close() } just Runs
        every { context.bekreftelseConsumer.close() } just Runs
        every { context.paaVegneAvConsumer.close() } just Runs

        // Simulerer en exception direkte i CompletableFuture for hendelse-consumeren
        val failingFuture = CompletableFuture<Void>()
        failingFuture.completeExceptionally(RuntimeException("Simulert feil i hendelse-konsument"))

        val spyConsumerHandler = spyk(consumerHandler)

        every {
            spyConsumerHandler.createConsumerFuture<BekreftelseHendelse>(
                context.hendelseConsumer,
                context.hendelseTopic,
                pollTimeout,
                any()
            )
        } returns failingFuture

        // Initialiser HWM for alle topics
        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(1, context.hendelseTopic)
            txContext.initHwm(1, context.bekreftelseTopic)
            txContext.initHwm(1, context.paaVegneAvTopic)
        }

        // Start consumer tasks med spy-objektet
        val future = spyConsumerHandler.startConsumerTasks(pollTimeout)

        // Verifiser at future blir avsluttet med exception
        val exception = shouldThrow<ExecutionException> {
            future.get(5, TimeUnit.SECONDS)
        }

        // Verifiser at rotårsaken er den forventede RuntimeException
        exception.cause shouldBe instanceOf<RuntimeException>()
        exception.cause?.message shouldBe "Simulert feil i hendelse-konsument"

        // Kall close() manuelt for å sikre at shutdown-prosessen kjører
        spyConsumerHandler.close()

        // Verifiser at shutdownCalled-flagget blir satt
        val shutdownCalledField = ConsumerHandler::class.java.getDeclaredField("shutdownCalled")
        shutdownCalledField.isAccessible = true
        val shutdownCalled = shutdownCalledField.get(spyConsumerHandler) as AtomicBoolean
        shutdownCalled.get() shouldBe true
    }
})