package no.nav.paw.arbeidssokerregisteret.app

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Situasjon
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.genererNyInternTilstandOgNyeApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikatStartOgStopp
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lagreInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lastInternTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.KStream

fun topology(
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    periodeTopic: String,
    situasjonTopic: String
): Topology {
    val strøm: KStream<Long, SpecificRecord> = builder.stream(innTopic)
    strøm
        .lastInternTilstand(dbNavn)
        .filter(::ignorerDuplikatStartOgStopp)
        .mapValues(::genererNyInternTilstandOgNyeApiTilstander)
        .lagreInternTilstand(dbNavn)
        .flatMap { key, value ->
            listOfNotNull(
                value.nyePeriodeTilstand?.let { KeyValue(key, it) },
                value.nySituasjonTilstand?.let { KeyValue(key, it) }
            )
        }.split()
        .branch(
            { _, value -> value is Periode },
            Branched.withConsumer { consumer -> consumer.to(periodeTopic) }
        )
        .branch(
            { _, value -> value is Situasjon },
            Branched.withConsumer { consumer -> consumer.to(situasjonTopic) }
        )
        .noDefaultBranch()
    return builder.build()
}
