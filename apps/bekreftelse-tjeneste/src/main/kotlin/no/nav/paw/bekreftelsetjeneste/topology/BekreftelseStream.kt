package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.partially1
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.bekreftelse.internehendelser.*
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelsetjeneste.config.ApplicationConfig
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.bekreftelsetjeneste.tilstand.InternBekreftelsePaaVegneAvStartet
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

fun StreamsBuilder.buildBekreftelseStream(applicationConfig: ApplicationConfig) {
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
                        .partially1(applicationConfig.bekreftelseKonfigurasjon)
                ),
            ) { record ->
                val bekreftelseTilstandStateStore = getStateStore<BekreftelseTilstandStateStore>(internStateStoreName)
                val paaVegneAvTilstandStateStore =
                    getStateStore<PaaVegneAvTilstandStateStore>(bekreftelsePaaVegneAvStateStoreName)

                val gjeldendeTilstand: BekreftelseTilstand? = retrieveState(bekreftelseTilstandStateStore, record)
                val paaVegneAvTilstand = paaVegneAvTilstandStateStore[record.value().periodeId]
                val melding = record.value()

                val hendelser = when (gjeldendeTilstand) {
                    null -> {
                        meldingsLogger.warn("Melding mottatt for periode som ikke er aktiv/eksisterer")
                        addTraceEventIkkeAktivPeriode(record.value().bekreftelsesloesning.name)
                        emptyList()
                    }

                    else -> {
                        haandterBekreftelseMottatt(
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

private fun addTraceEventIkkeAktivPeriode(
    bekreftelseLoesing: String,
) {
    with(Span.current()) {
        addEvent(
            errorEvent, Attributes.of(
                domainKey, "bekreftelse",
                actionKey, bekreftelseLevertAction,
                bekreftelseloesingKey, bekreftelseLoesing,
                periodeFunnetKey, false,
                harAnsvarKey, false
            )
        )
        setStatus(StatusCode.ERROR, "ingen aktiv periode funnet")
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
    hendelse: no.nav.paw.bekreftelse.melding.v1.Bekreftelse,
    gjeldeneTilstand: BekreftelseTilstand,
    paaVegneAvTilstand: PaaVegneAvTilstand?
): Pair<BekreftelseTilstand, List<BekreftelseHendelse>> {
    val bekreftelse = gjeldeneTilstand.findBekreftelse(hendelse.id)
    val registeretHarAnsvar = paaVegneAvTilstand?.paaVegneAvList?.isEmpty() ?: true
    return if (bekreftelse == null) {
        with(Span.current()) {
            addEvent(
                errorEvent, Attributes.of(
                    domainKey, "bekreftelse",
                    actionKey, bekreftelseLevertAction,
                    bekreftelseloesingKey, Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET.name,
                    feilMeldingKey, "Bekreftelse ikke funnet",
                    harAnsvarKey, registeretHarAnsvar
                )
            )
            setStatus(StatusCode.ERROR, "Bekreftelse ikke funnet")
        }
        meldingsLogger.warn("Melding {} har ingen matchene bekreftelse", hendelse.id)
        gjeldeneTilstand to emptyList()
    } else {
        when (val sisteTilstand = bekreftelse.sisteTilstand()) {
            is VenterSvar,
            is KlarForUtfylling,
            is GracePeriodeVarselet,
            is InternBekreftelsePaaVegneAvStartet -> {
                Span.current().addEvent(
                    okEvent, Attributes.of(
                        domainKey, "bekreftelse",
                        actionKey, bekreftelseLevertAction,
                        bekreftelseloesingKey, Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET.name,
                        tilstandKey, sisteTilstand.toString(),
                        harAnsvarKey, registeretHarAnsvar
                    )
                )
                val (hendelser, oppdatertBekreftelse) = behandleGyldigSvar(gjeldeneTilstand, hendelse, bekreftelse)
                gjeldeneTilstand.oppdaterBekreftelse(oppdatertBekreftelse) to hendelser
            }

            else -> {
                with(Span.current()) {
                    addEvent(
                        errorEvent, Attributes.of(
                            domainKey, "bekreftelse",
                            actionKey, bekreftelseLevertAction,
                            bekreftelseloesingKey, Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET.name,
                            feilMeldingKey, "Melding har ikke forventet tilstand",
                            tilstandKey, sisteTilstand.toString(),
                            harAnsvarKey, registeretHarAnsvar
                        )
                    )
                    setStatus(StatusCode.ERROR, "Melding har ikke forventet tilstand")
                }
                meldingsLogger.error(
                    "Melding {} har ikke forventet tilstand, tilstand={}",
                    hendelse.id,
                    sisteTilstand
                )
                gjeldeneTilstand to emptyList()
            }
        }
    }
}

fun BekreftelseTilstand.findBekreftelse(id: UUID): Bekreftelse? = bekreftelser.find { it.bekreftelseId == id }

fun behandleGyldigSvar(
    gjeldeneTilstand: BekreftelseTilstand,
    record: no.nav.paw.bekreftelse.melding.v1.Bekreftelse,
    bekreftelse: Bekreftelse
): Pair<List<BekreftelseHendelse>, Bekreftelse> {
    val arbeidssoekerId = gjeldeneTilstand.periode.arbeidsoekerId
    val oppdatertBekreftelse = bekreftelse + Levert(Instant.now())
    val vilFortsette = record.svar.vilFortsetteSomArbeidssoeker
    val baOmAaAvslutte = if (!vilFortsette) {
        val baOmAaAvslutteHendelse = BaOmAaAvsluttePeriode(
            hendelseId = UUID.randomUUID(),
            periodeId = record.periodeId,
            arbeidssoekerId = arbeidssoekerId,
            hendelseTidspunkt = Instant.now()
        )
        baOmAaAvslutteHendelse
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
        Span.current().addEvent(
            "publish", Attributes.of(
                AttributeKey.stringKey("type"), it.value().hendelseType
            )
        )
    }
}

private val meldingsLogger = LoggerFactory.getLogger("meldingsLogger")
