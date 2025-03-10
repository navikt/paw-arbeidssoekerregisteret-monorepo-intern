package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.partially1
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.metrics.tellBekreftelseMottatt
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import no.nav.paw.bekreftelsetjeneste.tilstand.InternBekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.Levert
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import no.nav.paw.bekreftelsetjeneste.tilstand.oppdaterBekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.plus
import no.nav.paw.bekreftelsetjeneste.tilstand.sisteTilstand
import no.nav.paw.kafka.processor.Punctuation
import no.nav.paw.kafka.processor.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Record
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import no.nav.paw.bekreftelse.internehendelser.vo.BrukerType as InterntBrukerType

fun StreamsBuilder.buildBekreftelseStream(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    applicationConfig: ApplicationConfig,
    bekreftelseKonfigurasjon: BekreftelseKonfigurasjon
) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>(bekreftelseTopic)
            .genericProcess<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse, Long, BekreftelseHendelse>(
                name = "meldingMottatt",
                internStateStoreName,
                bekreftelsePaaVegneAvStateStoreName,
                punctuation = Punctuation(
                    punctuationInterval,
                    PunctuationType.WALL_CLOCK_TIME,
                    ::bekreftelsePunctuator
                        .partially1(internStateStoreName)
                        .partially1(bekreftelsePaaVegneAvStateStoreName)
                        .partially1(bekreftelseKonfigurasjon)
                ),
            ) { record ->
                val wallClock = WallClock(Instant.ofEpochMilli(currentSystemTimeMs()))
                val bekreftelseTilstandStateStore = getStateStore<BekreftelseTilstandStateStore>(internStateStoreName)
                val paaVegneAvTilstandStateStore =
                    getStateStore<PaaVegneAvTilstandStateStore>(bekreftelsePaaVegneAvStateStoreName)

                val gjeldendeTilstand: BekreftelseTilstand? = retrieveState(bekreftelseTilstandStateStore, record)
                val paaVegneAvTilstand = paaVegneAvTilstandStateStore[record.value().periodeId]
                val melding = record.value()
                prometheusMeterRegistry.tellBekreftelseMottatt(
                    bekreftelse = melding,
                    periodeFunnet = gjeldendeTilstand != null,
                    harAnsvar = harAnsvar(melding.bekreftelsesloesning, paaVegneAvTilstand)
                )
                val hendelser = when (gjeldendeTilstand) {
                    null -> {
                        logWarning(
                            Loesning.from(record.value().bekreftelsesloesning),
                            bekreftelseLevertAction,
                            Feil.PERIODE_IKKE_FUNNET
                        )
                        emptyList()
                    }
                    else -> {
                        haandterBekreftelseMottatt(
                            wallClock,
                            gjeldendeTilstand,
                            paaVegneAvTilstand,
                            melding
                        ).also { (oppdatertTilstand, _) ->
                            if (oppdatertTilstand != gjeldendeTilstand) {
                                bekreftelseTilstandStateStore.put(
                                    oppdatertTilstand.periode.periodeId,
                                    oppdatertTilstand
                                )
                            }
                        }.second
                    }
                }
                forwardHendelser(record, hendelser, this::forward)
            }
            .to(bekreftelseHendelseloggTopic, Produced.with(Serdes.Long(), BekreftelseHendelseSerde()))
    }
}

fun retrieveState(
    bekreftelseTilstandStateStore: BekreftelseTilstandStateStore,
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>
): BekreftelseTilstand? {
    val periodeId = record.value().periodeId
    val state = bekreftelseTilstandStateStore[periodeId]
    return state
}

fun processPawNamespace(
    wallClock: WallClock,
    hendelse: no.nav.paw.bekreftelse.melding.v1.Bekreftelse,
    gjeldeneTilstand: BekreftelseTilstand,
    paaVegneAvTilstand: PaaVegneAvTilstand?
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    val bekreftelse = gjeldeneTilstand.findBekreftelse(hendelse.id)
    val registeretHarAnsvar = paaVegneAvTilstand?.paaVegneAvList?.isEmpty() ?: true
    return if (bekreftelse == null) {
        logWarning(
            Loesning.from(Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET),
            bekreftelseLevertAction,
            Feil.BEKREFTELSE_IKKE_FUNNET,
            registeretHarAnsvar
        )
        gjeldeneTilstand to emptyList()
    } else {
        when (val sisteTilstand = bekreftelse.sisteTilstand()) {
            is VenterSvar,
            is KlarForUtfylling,
            is GracePeriodeVarselet,
            is InternBekreftelsePaaVegneAvStartet -> {
                log(
                    loesning = Loesning.from(Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET),
                    handling = bekreftelseLevertAction,
                    periodeFunnet = true,
                    harAnsvar = registeretHarAnsvar,
                    tilstand = sisteTilstand
                )
                val (hendelser, oppdatertBekreftelse) = behandleGyldigSvar(wallClock, gjeldeneTilstand, hendelse, bekreftelse)
                gjeldeneTilstand.oppdaterBekreftelse(oppdatertBekreftelse) to hendelser
            }

            else -> {
                logWarning(
                    loesning = Loesning.from(Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET),
                    handling = bekreftelseLevertAction,
                    feil = Feil.UVENTET_TILSTAND,
                    harAnsvar = registeretHarAnsvar,
                    tilstand = sisteTilstand
                )
                gjeldeneTilstand to emptyList()
            }
        }
    }
}

fun BekreftelseTilstand.findBekreftelse(id: UUID): Bekreftelse? = bekreftelser.find { it.bekreftelseId == id }

fun behandleGyldigSvar(
    wallClock: WallClock,
    gjeldeneTilstand: BekreftelseTilstand,
    record: no.nav.paw.bekreftelse.melding.v1.Bekreftelse,
    bekreftelse: Bekreftelse
): Pair<List<BekreftelseHendelse>, Bekreftelse> {
    val arbeidssoekerId = gjeldeneTilstand.periode.arbeidsoekerId
    val oppdatertBekreftelse = bekreftelse + Levert(wallClock.value)
    val vilFortsette = record.svar.vilFortsetteSomArbeidssoeker
    val baOmAaAvslutte = if (!vilFortsette) {
        BaOmAaAvsluttePeriode(
            hendelseId = UUID.randomUUID(),
            periodeId = record.periodeId,
            arbeidssoekerId = arbeidssoekerId,
            hendelseTidspunkt = wallClock.value,
            utfoertAv = Bruker(
                type = record.svar.sendtInnAv.utfoertAv.type.tilInternBrukerType(),
                id = record.svar.sendtInnAv.utfoertAv.id,
                sikkerhetsnivaa = record.svar.sendtInnAv.utfoertAv.sikkerhetsnivaa
            )
        )
    } else null

    val meldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = UUID.randomUUID(),
        periodeId = record.periodeId,
        arbeidssoekerId = arbeidssoekerId,
        bekreftelseId = bekreftelse.bekreftelseId,
        hendelseTidspunkt = Instant.now()
    )
    return listOfNotNull(meldingMottatt, baOmAaAvslutte) to oppdatertBekreftelse
}

fun BrukerType.tilInternBrukerType(): InterntBrukerType =
    when (this) {
        BrukerType.UKJENT_VERDI -> InterntBrukerType.UKJENT_VERDI
        BrukerType.UDEFINERT -> InterntBrukerType.UDEFINERT
        BrukerType.VEILEDER -> InterntBrukerType.VEILEDER
        BrukerType.SYSTEM -> InterntBrukerType.SYSTEM
        BrukerType.SLUTTBRUKER -> InterntBrukerType.SLUTTBRUKER
    }

fun forwardHendelser(
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>,
    hendelser: List<BekreftelseHendelse>,
    forward: (Record<Long, BekreftelseHendelse>) -> Unit
) {
    hendelser.map(record::withValue).forEach {
        forward(it.withTimestamp(Instant.now().toEpochMilli()))
        Span.current().addEvent(
            "publish", Attributes.of(
                AttributeKey.stringKey("type"), it.value().hendelseType.replace(".", "_")
            )
        )
    }
}
