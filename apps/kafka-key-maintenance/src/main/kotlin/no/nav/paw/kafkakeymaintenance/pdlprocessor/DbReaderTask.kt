package no.nav.paw.kafkakeymaintenance.pdlprocessor

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.ApplicationContext
import no.nav.paw.kafkakeymaintenance.ErrorOccurred
import no.nav.paw.kafkakeymaintenance.ShutdownSignal
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.kafka.topic
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.HendelseRecord
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Data
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.delete
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.getBatch
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.common.serialization.Deserializer
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.Executor

data class DbReaderContext(
    val aktorConfig: AktorConfig,
    val receiver: (HendelseRecord<Hendelse>) -> Unit,
    val perioder: Perioder,
    val hentAlias: (List<String>) -> List<LokaleAlias>,
    val aktorDeSerializer: Deserializer<Aktor>
)

class DbReaderTask(
    healthIndicatorRepository: HealthIndicatorRepository,
    private val applicationContext: ApplicationContext,
    private val dbReaderContext: DbReaderContext
) {
    private val readiness =
        healthIndicatorRepository.addReadinessIndicator(ReadinessHealthIndicator(HealthStatus.UNHEALTHY))
    private val liveness =
        healthIndicatorRepository.addLivenessIndicator(LivenessHealthIndicator(HealthStatus.UNHEALTHY))

    fun run(executor: Executor): CompletableFuture<Unit> {
        return supplyAsync(
            {
                readiness.setHealthy()
                liveness.setHealthy()
                val ctxFactory = txContext(applicationContext.aktorConsumerVersion)
                while (applicationContext.shutdownCalled.get().not()) {
                    processBatch(ctxFactory)
                }
            },
            executor
        ).handle { _, ex ->
            readiness.setUnhealthy()
            liveness.setUnhealthy()
            if (ex != null) {
                applicationContext.eventOccured(ErrorOccurred(ex))
            } else {
                applicationContext.eventOccured(ShutdownSignal("AktorDbReaderTask"))
            }
        }
    }

    @WithSpan(
        value = "process_pdl_aktor_v2_batch",
        kind = SpanKind.INTERNAL
    )
    private fun processBatch(ctxFactory: Transaction.() -> TransactionContext) {
        transaction {
            val txContext = ctxFactory()
            val batch = txContext.getBatch(
                size = dbReaderContext.aktorConfig.batchSize,
                time = Instant.now() - dbReaderContext.aktorConfig.supressionDelay
            )
            val count = batch
                .asSequence()
                .flatMap { entry ->
                    initSpan(entry).use {
                        entry.takeIf { txContext.delete(it.id) }
                            ?.let(::processAktorMessage)
                            ?.map { hendelser -> dbReaderContext.receiver(hendelser) }
                            ?: emptyList()
                    }
                }
                .count()
            if (batch.isEmpty()) {
                applicationContext.logger.info("Ingen meldinger klare for prosessering, venter ${dbReaderContext.aktorConfig.interval}")
                Thread.sleep(dbReaderContext.aktorConfig.interval.toMillis())
            } else {
                applicationContext.logger.info("Genererte {} hendelser fra {} meldinger", count, batch.size)
            }
        }
    }

    fun processAktorMessage(entry: Data): List<HendelseRecord<Hendelse>> {
        return procesAktorMelding(
            meterRegistry = applicationContext.meterRegistry,
            hentAlias = dbReaderContext.hentAlias,
            aktorTopic = topic(dbReaderContext.aktorConfig.aktorTopic),
            perioder = dbReaderContext.perioder,
            aktorDeserializer = dbReaderContext.aktorDeSerializer,
            data = entry
        )
    }

    private val spanHandlerLogger = LoggerFactory.getLogger("spanHandler")
    private fun initSpan(entry: Data): ClossableSpan {
        val traceparent = entry.traceparant?.let { String(it, Charsets.UTF_8) }
        spanHandlerLogger.info("traceparent: {}", traceparent)
        return traceparent?.let { tp ->
            val asArray = tp.split("-")
            SpanContext.createFromRemoteParent(
                asArray[1],
                asArray[2],
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
        }?.let { spanContext ->
            val spanNoop = Span.wrap(spanContext)
            Span.current().addLink(spanContext)
            val telemetry = GlobalOpenTelemetry.get()
            val tracer = telemetry.tracerProvider
                .get("no.nav.paw.kafkakeymaintenance.pdlprocessor.DbReaderTask")
            tracer.spanBuilder("process_pdl_aktor_v2_record")
                .setParent(Context.current().with(spanNoop))
                .startSpan()
                .let(::ClossableSpan)
        } ?: ClossableSpan(null)
    }
}

class ClossableSpan(span: Span?) : AutoCloseable, Span by (span ?: Span.getInvalid()) {
    override fun close() {
        end()
    }
}
