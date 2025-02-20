package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.processors.oppdaterHendelseState
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
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
    pdlHentPerson: PdlHentPerson,
    sendAvsluttetHendelser: Boolean
): Topology {
    val publisertTag = Tag.of("publisert", sendAvsluttetHendelser.toString())
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
        .peek { _, value ->
            if (value is Avsluttet) {
                prometheusRegistry
                    .counter(
                        "generert_avsluttet_hendelse", listOf(
                            Tag.of("bosatt", value.opplysninger.contains(Opplysning.BOSATT_ETTER_FREG_LOVEN).toString()),
                            Tag.of("ikke_bosatt", value.opplysninger.contains(Opplysning.IKKE_BOSATT).toString()),
                            Tag.of("adresse_eoes", value.opplysninger.contains(Opplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES).toString()),
                            Tag.of("statsborger_eoes", value.opplysninger.contains(Opplysning.ER_EU_EOES_STATSBORGER).toString()),
                            Tag.of("doed", value.opplysninger.contains(Opplysning.DOED).toString()),
                            Tag.of("norsk", value.opplysninger.contains(Opplysning.ER_NORSK_STATSBORGER).toString()),
                            Tag.of("savnet", value.opplysninger.contains(Opplysning.SAVNET).toString()),
                            publisertTag
                        )
                    ).increment()
            }
        }
        .filter { _, _ -> sendAvsluttetHendelser }
        .to(hendelseLoggTopic, Produced.with(Serdes.Long(), HendelseSerde()))

    return build()
}