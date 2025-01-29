package no.nav.paw.kafkakeymaintenance.pdlprocessor

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
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
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.HendelseRecord
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.IdentMedPersonId
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Person
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.hentIdenter
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.hentIkkeProsessertePersoner
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.mergeProsessert
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
            val batch = txContext.hentIkkeProsessertePersoner(
                maksAntall = dbReaderContext.aktorConfig.batchSize,
                sistEndretFoer = Instant.now() - dbReaderContext.aktorConfig.supressionDelay
            )
            val count = batch
                .asSequence()
                .flatMap { entry ->
                    initSpan(
                        traceparent = entry.traceparant,
                        instrumentationScopeName = "no.nav.paw.kafkakeymaintenance.pdlprocessor.DbReaderTask",
                        spanName = "process_pdl_aktor_v2_record"
                    ).use {
                        entry.takeIf { txContext.mergeProsessert(entry.personId) }
                            ?.let { prosesserOppdatertPerson(it, txContext.hentIdenter(it.personId)) }
                            ?.map { hendelser -> dbReaderContext.receiver(hendelser) }
                            ?: emptyList()
                    }
                }
                .count()
            if (batch.isEmpty()) {
                val sleepUntil = Instant.now().plus(dbReaderContext.aktorConfig.interval)
                applicationContext.logger.info("Ingen meldinger klare for prosessering, venter til $sleepUntil (+ ${dbReaderContext.aktorConfig.interval})")
                Thread.sleep(dbReaderContext.aktorConfig.interval.toMillis())
            } else {
                applicationContext.logger.info("Genererte {} hendelser fra {} meldinger", count, batch.size)
            }
        }
    }

    fun prosesserOppdatertPerson(person: Person, identiter: List<IdentMedPersonId>): List<HendelseRecord<Hendelse>> {
        return procesAktorMelding(
            meterRegistry = applicationContext.meterRegistry,
            hentAlias = dbReaderContext.hentAlias,
            aktorTopic = Topic(dbReaderContext.aktorConfig.aktorTopic),
            perioder = dbReaderContext.perioder,
            person = person,
            identiter = identiter
        )
    }



}

private val spanHandlerLogger = LoggerFactory.getLogger("spanHandler")
fun initSpan(
    traceparent: String,
    instrumentationScopeName: String,
    spanName: String
): ClosableSpan {
    spanHandlerLogger.info("traceparent: {}", traceparent)
    return traceparent.let { tp ->
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
            .get(instrumentationScopeName)
        tracer.spanBuilder(spanName)
            .setParent(Context.current().with(spanNoop))
            .startSpan()
            .let(::ClosableSpan)
    } ?: ClosableSpan(null)
}

class ClosableSpan(span: Span?) : AutoCloseable, Span by (span ?: Span.getInvalid()) {
    override fun close() {
        end()
    }
}
