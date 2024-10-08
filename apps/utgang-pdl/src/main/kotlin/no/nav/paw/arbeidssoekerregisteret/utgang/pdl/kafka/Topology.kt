package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.processors.oppdaterHendelseState
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

fun StreamsBuilder.appTopology(
    prometheusRegistry: PrometheusMeterRegistry,
    periodeTopic: String,
    hendelseLoggTopic: String,
    hendelseStateStoreName: String,
    pdlHentPerson: PdlHentPerson
): Topology {
    stream(hendelseLoggTopic, Consumed.with(Serdes.Long(), HendelseSerde()))
        .filter { _, value -> value is Startet }
        .mapValues { value -> value as Startet }
        .oppdaterHendelseState(hendelseStateStoreName)

    stream<Long, Periode>(periodeTopic)
        .oppdaterHendelseState(
            hendelseStateStoreName = hendelseStateStoreName,
            prometheusMeterRegistry = prometheusRegistry,
            pdlHentPerson = pdlHentPerson
        )
        .to(hendelseLoggTopic, Produced.with(Serdes.Long(), HendelseSerde()))

    return build()
}