package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.OppgaveMelding
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselKanal
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.namespaceOrDefaultForLocal

private const val METRIC_PREFIX = "paw_bekreftelse_min_side_oppgaver"

fun MeterRegistry.periodeCounter(
    periode: Periode
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "database"),
            Tag.of("action", "read"),
            Tag.of("event.topic", "paw.arbeidssokerperioder-v1"),
            Tag.of("event.name", "periode"),
            Tag.of("event.status", if (periode.avsluttet == null) "aapen" else "lukket")
        )
    ).increment()
}

fun MeterRegistry.bekreftelseHendelseCounter(
    hendelse: BekreftelseHendelse
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "database"),
            Tag.of("action", "read"),
            Tag.of("event.topic", "paw.arbeidssoker-bekreftelse-hendelseslogg-v1"),
            Tag.of("event.name", hendelse::class.java.simpleName),
            Tag.of("event.type", hendelse.hendelseType)
        )
    ).increment()
}

fun MeterRegistry.varselCounter(
    runtimeEnvironment: RuntimeEnvironment,
    melding: OppgaveMelding
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "kafka"),
            Tag.of("action", "write"),
            Tag.of("event.topic", "min-side.aapen-brukervarsel-v1"),
            Tag.of("event.name", melding::class.java.simpleName),
            Tag.of("event.status", VarselStatus.UKJENT.value),
            Tag.of("event.type", VarselType.OPPGAVE.value),
            Tag.of("event.channel", VarselKanal.SMS.value),
            Tag.of("event.namespace", runtimeEnvironment.namespaceOrDefaultForLocal()),
            Tag.of("event.app", runtimeEnvironment.appNameOrDefaultForLocal()),
        )
    ).increment()
}

fun MeterRegistry.varselHendelseCounter(
    hendelse: VarselHendelse
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "database"),
            Tag.of("action", "read"),
            Tag.of("event.topic", "min-side.aapen-varsel-hendelse-v1"),
            Tag.of("event.name", hendelse.eventName.value),
            Tag.of("event.status", hendelse.status?.value ?: "null"),
            Tag.of("event.type", hendelse.varseltype.value),
            Tag.of("event.channel", hendelse.kanal?.value ?: "null"),
            Tag.of("event.renotifikasjon", hendelse.renotifikasjon?.toString() ?: "null"),
            Tag.of("event.sendtSomBatch", hendelse.sendtSomBatch?.toString() ?: "null"),
            Tag.of("event.namespace", hendelse.namespace),
            Tag.of("event.app", hendelse.appnavn),
        )
    ).increment()
}
