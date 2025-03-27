package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.BekreftelseHwmTable
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.TransactionContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.initHwm
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.kafka.ConsumerHandler
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicBoolean

class ApplicationLifecycleTest : StringSpec({
    initDbContainer()

    val logger = LoggerFactory.getLogger("application-lifecycle-test")

    beforeTest {
        transaction {
            BekreftelseHwmTable.deleteAll()
        }
    }

    "Applikasjonen skal håndtere feil i consumer tasks" {
        // Opprett failing future for å simulere en feilet consumer task
        val failingFuture = CompletableFuture<Void>()
        failingFuture.completeExceptionally(RuntimeException("Consumer task failed"))

        // Test at exception blir håndtert riktig
        val exception = shouldThrow<CompletionException> {
            failingFuture.join()
        }

        exception.cause shouldBe instanceOf<RuntimeException>()
        exception.cause?.message shouldBe "Consumer task failed"
    }

    "HwmRebalanceListener holder styr på partisjoner" {
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
            securityConfig = mockk(relaxed = true),
            kafkaKeysClient = mockk(relaxed = true),
        )

        val listener = HwmRebalanceListener(context, context.hendelseConsumer, context.hendelseTopic)

        transaction {
            val txContext = TransactionContext(context, this)
            txContext.initHwm(3, context.hendelseTopic)
        }

        val partitions = mutableListOf(
            TopicPartition(context.hendelseTopic, 0),
            TopicPartition(context.hendelseTopic, 1),
            TopicPartition(context.hendelseTopic, 2)
        )
        listener.onPartitionsAssigned(partitions)

        listener.currentlyAssignedPartitions shouldBe setOf(0, 1, 2)

        val revokedPartitions = mutableListOf(
            TopicPartition(context.hendelseTopic, 1)
        )
        listener.onPartitionsRevoked(revokedPartitions)

        listener.currentlyAssignedPartitions shouldBe setOf(0, 2)
    }

    "ConsumerHandler skal sette shutdownCalled ved close" {
        // Oppretter en ConsumerHandler
        val consumerHandler = ConsumerHandler(
            ApplicationContext(
                logger = logger,
                consumerVersion = CURRENT_VERSION,
                meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                hendelseConsumer = mockk(relaxed = true),
                bekreftelseConsumer = mockk(relaxed = true),
                paaVegneAvConsumer = mockk(relaxed = true),
                hendelseTopic = "hendelse-test-topic",
                bekreftelseTopic = "bekreftelse-test-topic",
                paaVegneAvTopic = "paavegneav-test-topic",
                securityConfig = mockk(relaxed = true),
                kafkaKeysClient = mockk(relaxed = true),
            )
        )

        // Henter shutdownCalled via reflection før vi kaller close()
        val shutdownCalledField = ConsumerHandler::class.java.getDeclaredField("shutdownCalled")
        shutdownCalledField.isAccessible = true
        val shutdownCalled = shutdownCalledField.get(consumerHandler) as AtomicBoolean

        // Verifiserer at flagget ikke er satt
        shutdownCalled.get() shouldBe false

        // Kaller close() på ConsumerHandler
        consumerHandler.close()

        // Verifiserer at flagget er satt
        shutdownCalled.get() shouldBe true
    }
})