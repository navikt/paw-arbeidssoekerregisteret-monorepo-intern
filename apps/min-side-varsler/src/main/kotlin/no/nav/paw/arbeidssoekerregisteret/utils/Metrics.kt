package no.nav.paw.arbeidssoekerregisteret.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.model.OppgaveMelding
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKanal
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.namespaceOrDefaultForLocal

private const val METRIC_PREFIX = "paw_min_side_varsler"

fun MeterRegistry.periodeCounter(
    action: String,
    periode: Periode
) {
    kafkaCounter(
        type = "periode",
        action = action,
        target = "database",
        eventTopic = "paw.arbeidssokerperioder-v1",
        eventType = periode::class.java.name,
        eventName = if (periode.avsluttet == null) "periode.startet" else "periode.avsluttet"
    )
}

fun MeterRegistry.bekreftelseHendelseCounter(
    action: String,
    hendelse: BekreftelseHendelse
) {
    kafkaCounter(
        type = "bekreftelse",
        action = action,
        target = "database",
        eventTopic = "paw.arbeidssoker-bekreftelse-hendelseslogg-v1",
        eventType = hendelse::class.java.name,
        eventName = hendelse.hendelseType
    )
}

fun MeterRegistry.varselCounter(
    runtimeEnvironment: RuntimeEnvironment,
    melding: OppgaveMelding
) {
    kafkaCounter(
        type = "varsel.hendelse",
        action = "write",
        target = "database",
        eventTopic = "min-side.aapen-brukervarsel-v1",
        eventType = melding::class.java.name,
        eventName = melding::class.java.simpleName,
        extraTags = Tags.of(
            Tag.of("varsel.type", VarselType.OPPGAVE.value),
            Tag.of("varsel.status", VarselStatus.UKJENT.value),
            Tag.of("varsel.kanal", VarselKanal.SMS.value),
            Tag.of("varsel.renotifikasjon", "null"),
            Tag.of("varsel.sendtSomBatch", "null"),
            Tag.of("varsel.namespace", runtimeEnvironment.namespaceOrDefaultForLocal()),
            Tag.of("varsel.app", runtimeEnvironment.appNameOrDefaultForLocal())
        )
    )
}

fun MeterRegistry.varselHendelseCounter(
    action: String,
    hendelse: VarselHendelse
) {
    kafkaCounter(
        type = "varsel.hendelse",
        action = action,
        target = "database",
        eventTopic = "min-side.aapen-varsel-hendelse-v1",
        eventType = hendelse::class.java.name,
        eventName = hendelse.eventName.value,
        extraTags = Tags.of(
            Tag.of("varsel.type", hendelse.varseltype.value),
            Tag.of("varsel.status", hendelse.status?.value ?: "null"),
            Tag.of("varsel.kanal", hendelse.kanal?.value ?: "null"),
            Tag.of("varsel.renotifikasjon", hendelse.renotifikasjon?.toString() ?: "null"),
            Tag.of("varsel.sendtSomBatch", hendelse.sendtSomBatch?.toString() ?: "null"),
            Tag.of("varsel.namespace", hendelse.namespace),
            Tag.of("varsel.app", hendelse.appnavn)
        )
    )
}

fun MeterRegistry.kafkaCounter(
    type: String,
    action: String,
    target: String,
    eventTopic: String,
    eventType: String,
    eventName: String,
    extraTags: Tags = Tags.empty()
) = genericCounter(
    type, action, "kafka", target, Tags.of(
        Tag.of("event.topic", eventTopic),
        Tag.of("event.type", eventType),
        Tag.of("event.name", eventName)
    ).and(extraTags)
)

fun MeterRegistry.genericCounter(
    type: String,
    action: String,
    source: String,
    target: String,
    extraTags: Tags = Tags.empty()
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            Tag.of("type", type),
            Tag.of("action", action),
            Tag.of("source", source),
            Tag.of("target", target)
        ).and(extraTags)
    ).increment()
}
