package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.model.VarselHendelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse

private const val METRIC_PREFIX = "paw_bekreftelse_min_side_oppgaver"

fun MeterRegistry.periodeCounter(
    periode: Periode
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("source", "kafka"),
            Tag.of("target", "metrics"),
            Tag.of("action", "count"),
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
            Tag.of("target", "metrics"),
            Tag.of("action", "count"),
            Tag.of("event.topic", "paw.arbeidssoker-bekreftelse-hendelseslogg-v1"),
            Tag.of("event.name", hendelse::class.java.name),
            Tag.of("event.type", hendelse.hendelseType)
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
            Tag.of("target", "metrics"),
            Tag.of("action", "count"),
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
