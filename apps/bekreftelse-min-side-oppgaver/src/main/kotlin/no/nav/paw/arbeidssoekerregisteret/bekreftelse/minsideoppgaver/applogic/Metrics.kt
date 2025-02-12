package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.VarselHendelse

private const val METRIC_PREFIX = "paw_bekreftelse_min_side_oppgaver"

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
