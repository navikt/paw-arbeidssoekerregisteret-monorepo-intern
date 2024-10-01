package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.partially1
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.Tilstand.VenterSvar
import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.config.kafka.streams.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/*fun StreamsBuilder.buildBekreftelseStream(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>(bekreftelseTopic)
            .genericProcess<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse, Long, BekreftelseHendelse>(
                name = "meldingMottatt",
                internStateStoreName,
                punctuation = Punctuation(
                    punctuationInterval,
                    PunctuationType.WALL_CLOCK_TIME,
                    ::bekreftelsePunctuator.partially1(internStateStoreName).partially1(applicationConfig.bekreftelseIntervals)
                ),
            ) { record ->
                val stateStore = getStateStore<StateStore>(internStateStoreName)
                val gjeldeneTilstand: InternTilstand? = stateStore[record.value().periodeId]
                if (gjeldeneTilstand == null) {
                    // TODO: håndtere potensiell tom tilstand når vi starter med ansvarsTopic
                    meldingsLogger.warn("Melding mottatt for periode som ikke er aktiv/eksisterer")
                    return@genericProcess
                }
                if (record.value().namespace == "paw") {
                    val bekreftelse =
                        gjeldeneTilstand.bekreftelser.find { bekreftelse -> bekreftelse.bekreftelseId == record.value().id }
                    when {
                        bekreftelse == null -> {
                            meldingsLogger.warn("Melding {} har ingen matchene bekreftelse", record.value().id)
                        }

                        bekreftelse.tilstand is VenterSvar || bekreftelse.tilstand is KlarForUtfylling -> {
                            val (hendelser, oppdatertBekreftelse) = behandleGyldigSvar(
                                gjeldeneTilstand.periode.arbeidsoekerId,
                                record,
                                bekreftelse
                            )
                            val oppdatertBekreftelser = gjeldeneTilstand.bekreftelser
                                .filterNot { t -> t.bekreftelseId == oppdatertBekreftelse.bekreftelseId } + oppdatertBekreftelse
                            val oppdatertTilstand = gjeldeneTilstand.copy(bekreftelser = oppdatertBekreftelser)
                            stateStore.put(oppdatertTilstand.periode.periodeId, oppdatertTilstand)
                            hendelser
                                .map(record::withValue)
                                .forEach(::forward)
                        }

                        else -> {
                            meldingsLogger.warn(
                                "Melding {} har ikke forventet tilstand, tilstand={}",
                                record.value().id,
                                bekreftelse.tilstand
                            )
                        }
                    }
                }
            }.to(bekreftelseHendelseloggTopic, Produced.with(Serdes.Long(), BekreftelseHendelseSerde()))
    }
}*/

fun StreamsBuilder.buildBekreftelseStream(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>(bekreftelseTopic)
            .genericProcess<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse, Long, BekreftelseHendelse>(
                name = "meldingMottatt",
                internStateStoreName,
                punctuation = Punctuation(
                    punctuationInterval,
                    PunctuationType.WALL_CLOCK_TIME,
                    ::bekreftelsePunctuator.partially1(internStateStoreName).partially1(applicationConfig.bekreftelseIntervals)
                ),
            ) { record ->
                val stateStore = getStateStore<StateStore>(internStateStoreName)

                val gjeldendeTilstand: InternTilstand? = retrieveState(stateStore, record)

                if (gjeldendeTilstand == null) {
                    Span.current().addEvent("tilstand is null for record", Attributes.of(
                        AttributeKey.longKey("recordKey"), record.key()
                    ))
                    meldingsLogger.warn("Melding mottatt for periode som ikke er aktiv/eksisterer")
                    return@genericProcess
                }

                if (record.value().namespace == "paw") {
                    processPawNamespace(record, stateStore, gjeldendeTilstand, ::forward)
                }
            }
            .to(bekreftelseHendelseloggTopic, Produced.with(Serdes.Long(), BekreftelseHendelseSerde()))
    }
}

@WithSpan(
    value = "retrieveState",
    kind = SpanKind.INTERNAL
)
fun retrieveState(
    stateStore: StateStore,
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>
): InternTilstand? {
    val periodeId = record.value().periodeId
    val state = stateStore[periodeId]

    Span.current().setAttribute("periodeId", periodeId.toString())
    return state
}

@WithSpan(
    value = "processPawNamespace",
    kind = SpanKind.INTERNAL
)
fun processPawNamespace(
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>,
    stateStore: StateStore,
    gjeldeneTilstand: InternTilstand,
    forward: (Record<Long, BekreftelseHendelse>) -> Unit
) {
    val bekreftelse = gjeldeneTilstand.findBekreftelse(record.value().id)

    if (bekreftelse == null) {
        Span.current().addEvent("Bekreftelse not found for message")
        meldingsLogger.warn("Melding {} har ingen matchene bekreftelse", record.value().id)
        return
    }

    when (bekreftelse.tilstand) {
        is VenterSvar, is KlarForUtfylling -> {
            val (hendelser, oppdatertBekreftelse) = behandleGyldigSvar(gjeldeneTilstand, record, bekreftelse)
            oppdaterStateStore(stateStore, gjeldeneTilstand, oppdatertBekreftelse)

            Span.current().setAttribute("bekreftelse.tilstand", bekreftelse.tilstand.toString())

            forwardHendelser(record, hendelser, forward)
        }

        else -> {
            Span.current().setAttribute("unexpected_tilstand", bekreftelse.tilstand.toString())
            meldingsLogger.warn(
                "Melding {} har ikke forventet tilstand, tilstand={}",
                record.value().id,
                bekreftelse.tilstand
            )
        }
    }
}

fun InternTilstand.findBekreftelse(id: UUID): Bekreftelse? = bekreftelser.find { it.bekreftelseId == id }

@WithSpan(
    value = "behandleGyldigSvar",
    kind = SpanKind.INTERNAL
)
fun behandleGyldigSvar(
    gjeldeneTilstand: InternTilstand,
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>,
    bekreftelse: Bekreftelse
): Pair<List<BekreftelseHendelse>, Bekreftelse> {
    val arbeidssoekerId = gjeldeneTilstand.periode.arbeidsoekerId

    Span.current().setAttribute("arbeidsoekerId", arbeidssoekerId.toString())
    Span.current().setAttribute("bekreftelseId", bekreftelse.bekreftelseId.toString())
    Span.current().setAttribute("periodeId", record.value().periodeId.toString())
    Span.current().setAttribute("bekreftelse.oldTilstand", bekreftelse.tilstand.toString())

    val oppdatertBekreftelse = bekreftelse.copy(tilstand = Tilstand.Levert)
    Span.current().setAttribute("bekreftelse.newTilstand", Tilstand.Levert.toString())

    val vilFortsette = record.value().svar.vilFortsetteSomArbeidssoeker
    Span.current().setAttribute("vilFortsetteSomArbeidssoeker", vilFortsette.toString())

    val baOmAaAvslutte = if (!vilFortsette) {
        val baOmAaAvslutteHendelse = BaOmAaAvsluttePeriode(
            hendelseId = UUID.randomUUID(),
            periodeId = record.value().periodeId,
            arbeidssoekerId = arbeidssoekerId,
            hendelseTidspunkt = Instant.now()
        )
        Span.current().setAttribute("avsluttePeriode.hendelseId", baOmAaAvslutteHendelse.hendelseId.toString())
        baOmAaAvslutteHendelse
    } else null

    val meldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = UUID.randomUUID(),
        periodeId = record.value().periodeId,
        arbeidssoekerId = arbeidssoekerId,
        bekreftelseId = bekreftelse.bekreftelseId,
        hendelseTidspunkt = Instant.now()
    )
    Span.current().setAttribute("meldingMottatt.hendelseId", meldingMottatt.hendelseId.toString())

    return listOfNotNull(meldingMottatt, baOmAaAvslutte) to oppdatertBekreftelse
}

@WithSpan(
    value = "oppdaterStateStore",
    kind = SpanKind.INTERNAL
)
fun oppdaterStateStore(
    stateStore: StateStore,
    gjeldeneTilstand: InternTilstand,
    oppdatertBekreftelse: Bekreftelse
) {
    val oppdatertTilstand = gjeldeneTilstand.updateBekreftelse(oppdatertBekreftelse)

    stateStore.put(oppdatertTilstand.periode.periodeId, oppdatertTilstand)

    Span.current().setAttribute("periodeId", oppdatertTilstand.periode.periodeId.toString())
}

fun InternTilstand.updateBekreftelse(oppdatertBekreftelse: Bekreftelse): InternTilstand =
    copy(
        bekreftelser = this.bekreftelser
            .filterNot { it.bekreftelseId == oppdatertBekreftelse.bekreftelseId } + oppdatertBekreftelse
    )

@WithSpan(
    value = "forwardHendelser",
    kind = SpanKind.INTERNAL
)
fun forwardHendelser(
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>,
    hendelser: List<BekreftelseHendelse>,
    forward: (Record<Long, BekreftelseHendelse>) -> Unit
) {
    hendelser.map(record::withValue).forEach {
        forward(it)
        Span.current().addEvent("Forwarded hendelse", Attributes.of(
            AttributeKey.stringKey("hendelseId"), it.value().hendelseId.toString()
        ))
    }

    Span.current().addEvent("Events forwarded", Attributes.of(
        AttributeKey.stringKey("event_count"), hendelser.size.toString()
    ))
}

private val meldingsLogger = LoggerFactory.getLogger("meldingsLogger")
