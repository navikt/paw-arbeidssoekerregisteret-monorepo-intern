package no.nav.paw.bekreftelsetjeneste.topology

import arrow.core.partially1
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
}

fun behandleGyldigSvar(
    arbeidssoekerId: Long,
    record: Record<Long, no.nav.paw.bekreftelse.melding.v1.Bekreftelse>,
    bekreftelse: Bekreftelse
): Pair<List<BekreftelseHendelse>, Bekreftelse> {
    val oppdatertBekreftelse = bekreftelse.copy(tilstand = Tilstand.Levert)
    val baOmAaAvslutte = if (!record.value().svar.vilFortsetteSomArbeidssoeker) {
        BaOmAaAvsluttePeriode(
            hendelseId = UUID.randomUUID(),
            periodeId = record.value().periodeId,
            arbeidssoekerId = arbeidssoekerId,
            hendelseTidspunkt = Instant.now()
        )
    } else null
    val meldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = UUID.randomUUID(),
        periodeId = record.value().periodeId,
        arbeidssoekerId = arbeidssoekerId,
        bekreftelseId = bekreftelse.bekreftelseId,
        hendelseTidspunkt = Instant.now()
    )
    return listOfNotNull(meldingMottatt, baOmAaAvslutte) to oppdatertBekreftelse
}


private val meldingsLogger = LoggerFactory.getLogger("meldingsLogger")


