package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.metrics.Actions
import no.nav.paw.arbeidssokerregisteret.app.metrics.eventReceived
import no.nav.paw.arbeidssokerregisteret.app.metrics.stateSent
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord

context(PrometheusMeterRegistry)
fun tellHendelse(topic: String, hendelse: Hendelse) {
    eventReceived(
        topic = topic,
        messageType = hendelse.hendelseType,
        action = when(hendelse) {
            is no.nav.paw.arbeidssokerregisteret.intern.v1.Startet -> Actions.START
            is no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet -> Actions.STOP
            is no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt -> Actions.INFO_RECEIVED
            is no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist -> Actions.REJECTED
            else -> Actions.UNKNOWN
        }
    )
}

context(PrometheusMeterRegistry)
fun tellUtgåendeTilstand(topic: String, state: SpecificRecord) {
    stateSent(
        topic = topic,
        action = when (state) {
            is Periode -> if (state.avsluttet == null) Actions.START else Actions.STOP
            is OpplysningerOmArbeidssoeker -> Actions.INFO_RECEIVED
            else -> Actions.UNKNOWN
        },
        messageType = "${state.schema.namespace}.${state.schema.name}"
    )
}