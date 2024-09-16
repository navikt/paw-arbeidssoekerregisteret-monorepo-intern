package no.nav.paw.bekretelsetjeneste

import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.bekreftelse.ansvar.v1.AnsvarEndret
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import org.apache.kafka.streams.StreamsBuilder

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processAnsvarTopic() {
    stream<Long, AnsvarEndret>(ansvarsTopic)
        .genericProcess<Long, AnsvarEndret, Long, BekreftelseHendelse>("ansvarEndret", stateStoreName) { record ->

        }
}

