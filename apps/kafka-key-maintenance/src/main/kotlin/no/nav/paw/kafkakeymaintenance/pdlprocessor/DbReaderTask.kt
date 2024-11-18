package no.nav.paw.kafkakeymaintenance.pdlprocessor

import io.opentelemetry.api.trace.*
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.health.model.HealthStatus
import no.nav.paw.health.model.LivenessHealthIndicator
import no.nav.paw.health.model.ReadinessHealthIndicator
import no.nav.paw.health.repository.HealthIndicatorRepository
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.ApplicationContext
import no.nav.paw.kafkakeymaintenance.ErrorOccurred
import no.nav.paw.kafkakeymaintenance.ShutdownSignal
import no.nav.paw.kafkakeymaintenance.kafka.topic
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.HendelseRecord
import no.nav.paw.kafkakeymaintenance.pdlprocessor.functions.metadata
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.Data
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.delete
import no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring.getBatch
import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.common.serialization.Deserializer
import org.jetbrains.exposed.sql.transactions.transaction
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
                    transaction {
                        val txContext = ctxFactory()
                        val batch = txContext.getBatch(
                            size = dbReaderContext.aktorConfig.batchSize,
                            time = Instant.now() - dbReaderContext.aktorConfig.supressionDelay
                        )
                        val count = batch
                            .filter { entry -> txContext.delete(entry.id) }
                            .flatMap { entry ->
                                processAktorMessage(entry)

                            }.map { hendelseRecord -> dbReaderContext.receiver(hendelseRecord) }
                            .count()
                        if (batch.isEmpty()) {
                            applicationContext.logger.info("Ingen meldinger klare for prosessering, venter ${dbReaderContext.aktorConfig.interval}")
                            Thread.sleep(dbReaderContext.aktorConfig.interval.toMillis())
                        } else {
                            applicationContext.logger.info("Genererte {} hendelser fra {} meldinger", count, batch.size)
                        }
                    }
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
        value = "process_pdl_aktor_v2_record",
        kind = SpanKind.INTERNAL
    )
    fun processAktorMessage(entry: Data): List<HendelseRecord<Hendelse>> {
        linkSpan(entry)
        val metadata = metadata(
            kilde = dbReaderContext.aktorConfig.aktorTopic,
            tidspunkt = Instant.now(),
            tidspunktFraKilde = TidspunktFraKilde(
                tidspunkt = entry.time,
                avviksType = AvviksType.FORSINKELSE
            )
        )
        return procesAktorMelding(
            meterRegistry = applicationContext.meterRegistry,
            hentAlias = dbReaderContext.hentAlias,
            aktorTopic = topic(dbReaderContext.aktorConfig.aktorTopic),
            perioder = dbReaderContext.perioder,
            aktorDeserializer = dbReaderContext.aktorDeSerializer,
            data = entry
        )
    }

    private fun linkSpan(entry: Data) {
        val traceparent = entry.traceparant?.let { String(it, Charsets.UTF_8) }
        applicationContext.logger.debug("Lastet traceparent fra database: $traceparent")
        traceparent?.let { tp ->
            val asArray = tp.split("-")
            SpanContext.createFromRemoteParent(
                asArray[1],
                asArray[2],
                TraceFlags.fromHex(asArray[3], 0),
                TraceState.getDefault()
            )
        }?.also { Span.current().addLink(it) }
    }
}