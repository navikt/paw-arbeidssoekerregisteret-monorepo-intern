package no.nav.paw.kafkakeymaintenance.kafka

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.opentelemetry.api.trace.Span
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeymaintenance.ApplicationContext
import no.nav.paw.kafkakeymaintenance.ErrorOccurred
import no.nav.paw.kafkakeymaintenance.ShutdownSignal
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
    private val function: HwmRunnerProcessor<K, V>,
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
                                    .forEach { record ->
                                        val ignore = function.ignore(record)
                                        val aboveHwm = !ignore && txContext.updateHwm(
                                            topic = Topic(record.topic()),
                                            partition = record.partition(),
                                            offset = record.offset(),
                                            time = Instant.ofEpochMilli(record.timestamp()),
                                            lastUpdated = Instant.now()
                                        )
                                        Span.current()
                                            .setAttribute("hwm_result", if (aboveHwm) "above_hwm" else "below_hwm")
                                            .setAttribute("hwm_ignore", ignore)
                                        applicationContext.meterRegistry.counter(
                                            "paw_hwm_consumer_v2",
                                            Tags.of(
                                                Tag.of("name", name),
                                                Tag.of("topic", record.topic()),
                                                Tag.of("partition", record.partition().toString()),
                                                Tag.of("above_hwm", aboveHwm.toString()),
                                                Tag.of("ignore", ignore.toString())
                                            )
                                        ).increment()
                                        if (aboveHwm) {
                                            function.process(txContext, record)
                                        }
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