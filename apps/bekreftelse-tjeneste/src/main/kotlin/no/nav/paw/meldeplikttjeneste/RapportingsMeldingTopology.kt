package no.nav.paw.meldeplikttjeneste

import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.rapportering.melding.v1.Melding
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processRapporteringsMeldingTopic() {
    stream<Long, Melding>(rapporteringsTopic)
        .genericProcess<Long, Melding, Long, BekreftelseHendelse>(
            name = "meldingMottatt",
            statStoreName
        ) { record ->
            val gjeldeneTilstand: InternTilstand? = getStateStore<StateStore>(statStoreName)[record.value().periodeId]
            if (gjeldeneTilstand == null) {
                meldingsLogger.warn("Melding mottatt for periode som ikke er aktiv/eksisterer")
            } else {
                TODO()
            }

        }

}

private val meldingsLogger = LoggerFactory.getLogger("meldingsLogger")


