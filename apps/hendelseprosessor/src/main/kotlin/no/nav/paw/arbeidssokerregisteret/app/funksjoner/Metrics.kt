package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.metrics.Actions
import no.nav.paw.arbeidssokerregisteret.app.metrics.Labels
import no.nav.paw.arbeidssokerregisteret.app.metrics.Names
import no.nav.paw.arbeidssokerregisteret.app.metrics.eventReceived
import no.nav.paw.arbeidssokerregisteret.app.metrics.fineGrainedDurationToMonthsBucket
import no.nav.paw.arbeidssokerregisteret.app.metrics.stateSent
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.avro.specific.SpecificRecord
import java.time.Instant

fun PrometheusMeterRegistry.tellHendelse(topic: String, hendelse: Hendelse) {
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

fun PrometheusMeterRegistry.tellUtgåendeTilstand(topic: String, state: SpecificRecord) {
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

fun PrometheusMeterRegistry.tellAvsluttetMedAarsak(periodeStartet: Instant, avsluttet: Avsluttet) {
    val aarsak = avsluttet.oppgittAarsak.takeIf { it != Aarsak.Udefinert } ?: avsluttet.kalkulertAarsak
    val kalkulert = avsluttet.oppgittAarsak == Aarsak.Udefinert
    val tidspunkt = avsluttet.metadata.tidspunktFraKilde?.tidspunkt ?: avsluttet.metadata.tidspunkt
    val erFeilretting = avsluttet.metadata.tidspunktFraKilde?.avviksType?.name?.lowercase() ?: "nei"
    val varighet = fineGrainedDurationToMonthsBucket(periodeStartet, tidspunkt)
    val kilde = avsluttet.metadata.kilde
        .split(":")
        .let { parts ->
            if (parts.size >= 2) {
                "${parts[0]}:${parts[1]}"
            } else {
                parts[0]
            }
        }
    counter(
        Names.AVSLUTTET,
        Tags.of(
            Tag.of(Labels.KALKULERT, kalkulert.toString()),
            Tag.of(Labels.AARSAK, aarsak.name),
            Tag.of(Labels.VARIGHET_MAANEDER, varighet),
            Tag.of(Labels.FEILRETTING, erFeilretting),
            Tag.of(Labels.UTFOERT_AV, avsluttet.metadata.utfoertAv.type.name.lowercase()),
            Tag.of(Labels.KILDE, kilde)
        )
    ).increment()
}