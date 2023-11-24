package no.nav.paw.arbeidssokerregisteret.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.genererNyInternTilstandOgNyeApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikatStartOgStopp
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lagreInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lastInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tellHendelse
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tellUtgåendeTilstand
import no.nav.paw.arbeidssokerregisteret.app.metrics.registerGauge
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

fun topology(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    periodeTopic: String,
    situasjonTopic: String
): Topology {
    val strøm: KStream<Long, Hendelse> = builder.stream(innTopic, Consumed.with(Serdes.Long(), HendelseSerde()))
    with(prometheusMeterRegistry) {
        val periodeLatency = AtomicLong(0)
        val situasjonLatency = AtomicLong(0)
        registerGauge(periodeTopic, periodeLatency)
        registerGauge(situasjonTopic, situasjonLatency)
        strøm
            .peek { _, hendelse -> tellHendelse(innTopic, hendelse) }
            .lastInternTilstand(dbNavn)
            .filter(::ignorerDuplikatStartOgStopp)
            .mapValues(::genererNyInternTilstandOgNyeApiTilstander)
            .lagreInternTilstand(dbNavn)
            .flatMap { key, value ->
                listOfNotNull(
                    value.nyePeriodeTilstand?.let { KeyValue(key, it as SpecificRecord) },
                    value.nySituasjonTilstand?.let { KeyValue(key, it as SpecificRecord) }
                )
            }.split()
            .branch(
                { _, value -> value is Periode },
                Branched.withConsumer { consumer ->
                    consumer
                        .peek { _, periode -> tellUtgåendeTilstand(periodeTopic, periode) }
                        .peek { _, periode -> kalkulerForsinkelse(periode)?.let { periodeLatency.set(it) } }
                        .to(periodeTopic)
                }
            )
            .branch(
                { _, value -> value is Situasjon },
                Branched.withConsumer { consumer ->
                    consumer
                        .peek { _, situasjon -> tellUtgåendeTilstand(situasjonTopic, situasjon) }
                        .peek { _, situasjon -> kalkulerForsinkelse(situasjon)?.let { situasjonLatency.set(it) } }
                        .to(situasjonTopic)
                }
            )
            .noDefaultBranch()
        return builder.build()
    }
}

fun kalkulerForsinkelse(tilstand: SpecificRecord): Long? {
    return when (tilstand) {
        is Periode -> Duration.between(Instant.now(), tilstand.avsluttet?.tidspunkt ?: tilstand.startet.tidspunkt).toMillis()
        is Situasjon -> Duration.between(Instant.now(), tilstand.sendtInnAv.tidspunkt).toMillis()
        else -> null
    }
}
