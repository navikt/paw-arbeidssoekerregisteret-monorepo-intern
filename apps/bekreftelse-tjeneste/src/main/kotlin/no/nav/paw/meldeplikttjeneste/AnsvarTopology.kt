package no.nav.paw.meldeplikttjeneste

import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.rapportering.ansvar.v1.AnsvarEndret
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import org.apache.kafka.streams.StreamsBuilder

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processAnsvarTopic() {
    stream<Long, AnsvarEndret>(ansvarsTopic)
        .genericProcess<Long, AnsvarEndret, Long, RapporteringsHendelse>("ansvarEndret", statStoreName) { record ->

        }
}

