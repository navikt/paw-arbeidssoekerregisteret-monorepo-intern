package no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tellUtgåendeTilstand
import no.nav.paw.arbeidssokerregisteret.app.metrics.registerLatencyGauge
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.processor.RecordContext
import org.apache.kafka.streams.processor.TopicNameExtractor
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

private const val RECORD_CONTEXT_NOT_PROVIDED = -2

class MeteredOutboundTopicNameExtractor(
    private val periodeTopic: String,
    private val opplysningerTopic: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) : TopicNameExtractor<Long, SpecificRecord> {

    val map: ConcurrentMap<Pair<String, Int>, AtomicLong> = ConcurrentHashMap()
    override fun extract(key: Long, value: SpecificRecord, recordContext: RecordContext?): String {
        val partition = recordContext?.partition() ?: RECORD_CONTEXT_NOT_PROVIDED
        val (tidspunkt, topic) = when (value) {
            is Periode -> (value.avsluttet?.tidspunkt
                ?: value.startet.tidspunkt) to periodeTopic

            is OpplysningerOmArbeidssoeker -> value.sendtInnAv.tidspunkt to opplysningerTopic
            else -> throw IllegalArgumentException("Ukjent type: ${value.javaClass.name}")
        }
        with(prometheusMeterRegistry) {
            val atomicLong = getOrCreateLatencyHolder(topic, partition)
            atomicLong.set(Duration.between(tidspunkt, Instant.now()).toMillis())
            tellUtgåendeTilstand(topic, value)
        }
        return topic
    }

    private fun PrometheusMeterRegistry.getOrCreateLatencyHolder(
        topic: String,
        partition: Int
    ): AtomicLong {
        val key = topic to partition
        val current = map[key]
        if (current != null) {
            return current
        } else {
            synchronized(map) {
                val current2 = map[key]
                if (current2 != null) {
                    return current2
                } else {
                    val atomicLong = AtomicLong()
                    map[key] = atomicLong
                    registerLatencyGauge(topic, partition, atomicLong)
                    return atomicLong
                }
            }
        }
    }
}