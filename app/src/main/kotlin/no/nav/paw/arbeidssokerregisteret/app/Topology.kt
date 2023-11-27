package no.nav.paw.arbeidssokerregisteret.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.genererNyInternTilstandOgNyeApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikatStartOgStopp
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.MeteredOutboundTopicNameExtractor
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lagreInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lastInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tellHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream

fun topology(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    periodeTopic: String,
    opplysningerOmArbeidssoekerTopic: String
): Topology {
    val strøm: KStream<Long, Hendelse> = builder.stream(innTopic, Consumed.with(Serdes.Long(), HendelseSerde()))
    val meteredTopicExtractor = MeteredOutboundTopicNameExtractor(periodeTopic, opplysningerOmArbeidssoekerTopic, prometheusMeterRegistry)
    with(prometheusMeterRegistry) {
        strøm
            .peek { _, hendelse -> tellHendelse(innTopic, hendelse) }
            .lastInternTilstand(dbNavn)
            .filter(::ignorerDuplikatStartOgStopp)
            .mapValues(::genererNyInternTilstandOgNyeApiTilstander)
            .lagreInternTilstand(dbNavn)
            .flatMap { key, value ->
                listOfNotNull(
                    value.nyePeriodeTilstand?.let { KeyValue(key, it as SpecificRecord) },
                    value.nyOpplysningerOmArbeidssoekerTilstand?.let { KeyValue(key, it as SpecificRecord) }
                )
            }.to(meteredTopicExtractor)
        return builder.build()
    }
}