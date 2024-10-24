package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.partially1
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.internehendelser.*
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.tilstand.*
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

fun StreamsBuilder.buildBekreftelseStream(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>(bekreftelseTopic)
            .genericProcess<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse, Long, BekreftelseHendelse>(
                name = "meldingMottatt",
                internStateStoreName,
                ansvarStateStoreName,
                punctuation = Punctuation(
                    punctuationInterval,
                    PunctuationType.WALL_CLOCK_TIME,
                    ::bekreftelsePunctuator
                        .partially1(internStateStoreName)
                        .partially1(ansvarStateStoreName)
                        .partially1(applicationConfig.bekreftelseIntervals)
                ),
            ) { record ->
                val internTilstandStateStore = getStateStore<InternTilstandStateStore>(internStateStoreName)
                val ansvarStateStore = getStateStore<AnsvarStateStore>(ansvarStateStoreName)

                val gjeldendeTilstand: InternTilstand? = retrieveState(internTilstandStateStore, record)
                val ansvar = ansvarStateStore[record.value().periodeId]
                val melding = record.value()

                if (gjeldendeTilstand == null) {
                    Span.current().setStatus(StatusCode.ERROR).addEvent("tilstand is null for record", Attributes.of(
                        AttributeKey.longKey("recordKey"), record.key()
                    ))
                    meldingsLogger.warn("Melding mottatt for periode som ikke er aktiv/eksisterer")
                    return@genericProcess
                }
                val (oppdatertTilstand, hendelser) = haandterBekreftelseMottatt(gjeldendeTilstand, ansvar, melding)
                if (oppdatertTilstand != gjeldendeTilstand) {
                    internTilstandStateStore.put(oppdatertTilstand.periode.periodeId, oppdatertTilstand)
                }
                forwardHendelser(record, hendelser, this::forward)
            }
            .to(bekreftelseHendelseloggTopic, Produced.with(Serdes.Long(), BekreftelseHendelseSerde()))
    }
}

@WithSpan(
    value = "retrieveState",
    kind = SpanKind.INTERNAL
)
fun retrieveState(
    internTilstandStateStore: InternTilstandStateStore,
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>
): InternTilstand? {
    val periodeId = record.value().periodeId
    val state = internTilstandStateStore[periodeId]

    Span.current().setAttribute("periodeId", periodeId.toString())
    return state
}

@WithSpan(
    value = "processPawNamespace",
    kind = SpanKind.INTERNAL
)
fun processPawNamespace(
    hendelse: no.nav.paw.bekreftelse.melding.v1.Bekreftelse,
    gjeldeneTilstand: InternTilstand
): Pair<InternTilstand, List<BekreftelseHendelse>> {
    val bekreftelse = gjeldeneTilstand.findBekreftelse(hendelse.id)

    if (bekreftelse == null) {
        Span.current().addEvent("Bekreftelse not found for message")
        meldingsLogger.warn("Melding {} har ingen matchene bekreftelse", hendelse.id)
        return gjeldeneTilstand to emptyList()
    }

    return when (val sisteTilstand = bekreftelse.sisteTilstand()) {
        is VenterSvar,
        is KlarForUtfylling,
        is AnsvarOvertattAvAndre -> {
            val (hendelser, oppdatertBekreftelse) = behandleGyldigSvar(gjeldeneTilstand, hendelse, bekreftelse)
            Span.current().setAttribute("bekreftelse.tilstand", sisteTilstand.toString())
            gjeldeneTilstand.oppdaterBekreftelse(oppdatertBekreftelse) to hendelser
        }

        else -> {
            Span.current().setAttribute("unexpected_tilstand", sisteTilstand.toString())
            meldingsLogger.warn(
                "Melding {} har ikke forventet tilstand, tilstand={}",
                hendelse.id,
                sisteTilstand
            )
            gjeldeneTilstand to emptyList()
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
    record: no.nav.paw.bekreftelse.melding.v1.Bekreftelse,
    bekreftelse: Bekreftelse
): Pair<List<BekreftelseHendelse>, Bekreftelse> {
    val arbeidssoekerId = gjeldeneTilstand.periode.arbeidsoekerId
    val sisteTilstand = bekreftelse.sisteTilstand()
    Span.current().setAttribute("arbeidsoekerId", arbeidssoekerId.toString())
    Span.current().setAttribute("bekreftelseId", bekreftelse.bekreftelseId.toString())
    Span.current().setAttribute("periodeId", record.periodeId.toString())
    Span.current().setAttribute("bekreftelse.oldTilstand", sisteTilstand.toString())

    val oppdatertBekreftelse = bekreftelse + Levert(Instant.now())
    Span.current().setAttribute("bekreftelse.newTilstand", oppdatertBekreftelse.sisteTilstand().toString())

    val vilFortsette = record.svar.vilFortsetteSomArbeidssoeker
    Span.current().setAttribute("vilFortsetteSomArbeidssoeker", vilFortsette.toString())

    val baOmAaAvslutte = if (!vilFortsette) {
        val baOmAaAvslutteHendelse = BaOmAaAvsluttePeriode(
            hendelseId = UUID.randomUUID(),
            periodeId = record.periodeId,
            arbeidssoekerId = arbeidssoekerId,
            hendelseTidspunkt = Instant.now()
        )
        Span.current().setAttribute("avsluttePeriode.hendelseId", baOmAaAvslutteHendelse.hendelseId.toString())
        baOmAaAvslutteHendelse
    } else null

    val meldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = UUID.randomUUID(),
        periodeId = record.periodeId,
        arbeidssoekerId = arbeidssoekerId,
        bekreftelseId = bekreftelse.bekreftelseId,
        hendelseTidspunkt = Instant.now()
    )
    Span.current().setAttribute("meldingMottatt.hendelseId", meldingMottatt.hendelseId.toString())

    return listOfNotNull(meldingMottatt, baOmAaAvslutte) to oppdatertBekreftelse
}

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
        forward(it.withTimestamp(Instant.now().toEpochMilli()))
        Span.current().addEvent("Forwarded hendelse", Attributes.of(
            AttributeKey.stringKey("hendelseId"), it.value().hendelseId.toString()
        ))
    }

    Span.current().addEvent("Antall hendelser forwarded", Attributes.of(
        AttributeKey.stringKey("event_count"), hendelser.size.toString()
    ))
}

private val meldingsLogger = LoggerFactory.getLogger("meldingsLogger")
