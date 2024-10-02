package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.slf4j.LoggerFactory

fun StreamsBuilder.buildBekreftelseUtgangStream(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse>(bekreftelseHendelseloggTopic)
            .genericProcess<Long, no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse, Long, Hendelse>(
                name = "bekreftelseUtgangStream",
                internStateStoreName,
                punctuation = Punctuation(
                    punctuationInterval,
                    PunctuationType.WALL_CLOCK_TIME,
                    { _, _ -> true }, // TODO()
                ),
            ) { record ->
               // TODO()
            }
            .to(hendelseloggTopic, Produced.with(Serdes.Long(), HendelseSerde()))
    }
}

private val bekreftelseUtgangLogger = LoggerFactory.getLogger("bekreftelseUtgangLogger")