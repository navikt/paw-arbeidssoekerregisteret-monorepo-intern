package no.nav.paw.kafkakeymaintenance.kafka

import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeymaintenance.ApplicationContext
import no.nav.paw.kafkakeymaintenance.ErrorOccurred
import no.nav.paw.kafkakeymaintenance.ShutdownSignal
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class HwmConsumer<K, V>(
    private val name: String,
    healthIndicatorRepository: HealthIndicatorRepository,
    private val applicationContext: ApplicationContext,
    private val contextFactory: ApplicationContext.(Transaction) -> TransactionContext,
    private val consumer: KafkaConsumer<K, V>,
    private val function: TransactionContext.(ConsumerRecord<K, V>) -> Unit,
    private val pollTimeout: Duration = Duration.ofMillis(1000),
) {
    private val liveness =
        healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator(HealthStatus.UNHEALTHY))
    private val readiness =
        healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator(HealthStatus.UNHEALTHY))

    fun run(executor: Executor): CompletableFuture<Unit> =
        CompletableFuture.supplyAsync(
            {
                liveness.setHealthy()
                consumer.use {
                    readiness.setHealthy()
                    while (!Thread.currentThread().isInterrupted && applicationContext.shutdownCalled.get().not()) {
                        val records = consumer.poll(pollTimeout)
                        if (records.isEmpty.not()) {
                            transaction {
                                val txContext = applicationContext.contextFactory(this)
                                records
                                    .filter { record ->
                                        txContext.updateHwm(
                                            topic = topic(record.topic()),
                                            partition = record.partition(),
                                            offset = record.offset(),
                                            time = Instant.ofEpochMilli(record.timestamp()),
                                            lastUpdated = Instant.now()
                                        )
                                    }
                                    .forEach { record ->
                                        txContext.function(record)
                                    }
                            }
                        }
                    }
                }
            },
            executor
        ).handle { _, throwable ->
            liveness.setUnhealthy()
            readiness.setUnhealthy()
            if (throwable != null) {
                applicationContext.eventOccured(ErrorOccurred(throwable))
            } else {
                applicationContext.eventOccured(ShutdownSignal(name))
            }
        }
}