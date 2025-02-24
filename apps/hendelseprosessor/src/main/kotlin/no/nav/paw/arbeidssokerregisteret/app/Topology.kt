package no.nav.paw.arbeidssokerregisteret.app

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.config.ApplicationLogicConfig
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.add
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.genererNyInternTilstandOgNyeApiTilstander
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.hendelseAkseptert
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.hendelseType
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerAvsluttetForAnnenPeriode
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerDuplikatStartOgStopp
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.ignorerOpphoerteIdenter
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.MeteredOutboundTopicNameExtractor
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lagreInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.kafkastreamsprocessors.lastInternTilstand
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tellHendelse
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.tilstandKey
import no.nav.paw.arbeidssokerregisteret.app.metrics.fineGrainedDurationToMonthsBucket
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream

fun topology(
    applicationLogicConfig: ApplicationLogicConfig,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    builder: StreamsBuilder,
    dbNavn: String,
    innTopic: String,
    periodeTopic: String,
    opplysningerOmArbeidssoekerTopic: String
): Topology {
    val strøm: KStream<Long, Hendelse> = builder.stream(innTopic, Consumed.with(Serdes.Long(), HendelseSerde()))
    val meteredTopicExtractor =
        MeteredOutboundTopicNameExtractor(periodeTopic, opplysningerOmArbeidssoekerTopic, prometheusMeterRegistry)
        strøm
            .lastInternTilstand(dbNavn)
            .peek { _, (_, tilstand, hendelse) ->
                prometheusMeterRegistry.tellHendelse(innTopic, hendelse)
                Span.current().setAllAttributes(
                    Attributes.builder()
                        .add(hendelseType(hendelse))
                        .put(tilstandKey, tilstand?.gjeldeneTilstand?.name ?: "null")
                        .put(
                            AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.periodeIdErSatt"),
                            (hendelse as? Avsluttet)?.let { it.periodeId != null }?.toString() ?: "NA"
                        ).put(AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand.periodeIdErSatt"),
                            (tilstand?.gjeldenePeriode?.id != null).toString()
                        ).build()
                    )
            }
            .filter(::ignorerOpphoerteIdenter)
            .filter(::ignorerDuplikatStartOgStopp)
            .filter(::ignorerAvsluttetForAnnenPeriode)
            .peek { _, (_, _, hendelse) ->
                Span.current().addEvent(hendelseAkseptert, Attributes.builder().add(hendelseType(hendelse)).build())
            }
            .mapValues { internTilstandOgHendelse ->
                genererNyInternTilstandOgNyeApiTilstander(
                    applicationLogicConfig,
                    internTilstandOgHendelse
                )
            }
            .lagreInternTilstand(dbNavn)
            .flatMapValues { _, value ->
                listOfNotNull(
                    value.nyPeriodeTilstand as SpecificRecord?,
                    value.nyOpplysningerOmArbeidssoekerTilstand as SpecificRecord?
                )
            }
            .peek { _, value ->
                if ((value as? Periode)?.avsluttet != null) {
                    prometheusMeterRegistry.counter("avsluttet_periode_med_varighet", Tags.of(
                        Tag.of("varighet_maaneder", fineGrainedDurationToMonthsBucket(value.startet.tidspunktFraKilde?.tidspunkt ?: value.startet.tidspunkt)),
                        Tag.of("er_feilretting", value.avsluttet?.tidspunktFraKilde?.avviksType?.name ?: "nei")
                    )).increment()
                }
            }
            .to(meteredTopicExtractor)
    return builder.build()
}
