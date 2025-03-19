package no.nav.paw.arbeidssoekerregisteret.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.arbeidssoekerregisteret.model.VarselKanal
import no.nav.paw.arbeidssoekerregisteret.model.VarselMelding
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.namespaceOrDefaultForLocal

private const val METRIC_PREFIX = "paw_min_side_varsler"

val Periode.eventName get() = if (avsluttet == null) "periode.startet" else "periode.avsluttet"

enum class Type(val value: String) {
    PERIODE("periode"),
    BEKREFTELSE_HENDELSE("bekreftelse_hendelse"),
    VARSEL("varsel"),
    VARSEL_HENDELSE("varsel_hendelse");
}

enum class Action(val value: String) {
    READ("read"),
    WRITE("write"),
    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete"),
    IGNORE("ignore"),
    FAIL("fail");
}

enum class Source(val value: String) {
    DATABASE("database"),
    KAFKA("kafka");
}

enum class Target(val value: String) {
    DATABASE("database"),
    KAFKA("kafka");
}

enum class TagKey(val key: String) {
    TYPE("x_type"),
    ACTION("x_action"),
    SOURCE("x_source"),
    TARGET("x_target"),
    EVENT_TOPIC("x_event_topic"),
    EVENT_TYPE("x_event_type"),
    EVENT_NAME("x_event_name"),
    VARSEL_TYPE("x_varsel_type"),
    VARSEL_STATUS("x_varsel_status"),
    VARSEL_KANAL("x_varsel_kanal"),
    VARSEL_RENOTIFIKASJON("x_varsel_renotifikasjon"),
    VARSEL_SENDT_SOM_BATCH("x_varsel_sendt_som_batch"),
    VARSEL_NAMESPACE("x_varsel_namespace"),
    VARSEL_APP("x_varsel_app");

    fun asTag(value: String): Tag = Tag.of(key, value)
    fun asTag(boolean: Boolean?): Tag = Tag.of(key, boolean?.toString() ?: "null")
    fun asTag(type: Type): Tag = Tag.of(key, type.value)
    fun asTag(action: Action): Tag = Tag.of(key, action.value)
    fun asTag(source: Source): Tag = Tag.of(key, source.value)
    fun asTag(target: Target): Tag = Tag.of(key, target.value)
    fun asTag(eventName: VarselEventName): Tag = Tag.of(key, eventName.value)
    fun asTag(varselType: VarselType): Tag = Tag.of(key, varselType.value)
    fun asTag(status: VarselStatus?): Tag = Tag.of(key, status?.value ?: "null")
    fun asTag(kanal: VarselKanal?): Tag = Tag.of(key, kanal?.value ?: "null")
}

fun MeterRegistry.periodeCounter(
    action: Action,
    periode: Periode
) {
    genericCounter(
        type = Type.PERIODE,
        action = action,
        source = Source.KAFKA,
        target = Target.DATABASE,
        extraTags = Tags.of(
            TagKey.EVENT_TOPIC.asTag("paw.arbeidssokerperioder-v1"),
            TagKey.EVENT_TYPE.asTag(periode::class.java.name),
            TagKey.EVENT_NAME.asTag(periode.eventName),
        )
    )
}

fun MeterRegistry.bekreftelseHendelseCounter(
    action: Action,
    hendelse: BekreftelseHendelse
) {
    bekreftelseHendelseCounter(
        action = action,
        eventType = hendelse::class.java.name,
        eventName = hendelse.hendelseType
    )
}

fun MeterRegistry.bekreftelseHendelseCounter(
    action: Action,
    eventType: String,
    eventName: String
) {
    genericCounter(
        type = Type.BEKREFTELSE_HENDELSE,
        action = action,
        source = Source.KAFKA,
        target = Target.DATABASE,
        extraTags = Tags.of(
            TagKey.EVENT_TOPIC.asTag("paw.arbeidssoker-bekreftelse-hendelseslogg-v1"),
            TagKey.EVENT_TYPE.asTag(eventType),
            TagKey.EVENT_NAME.asTag(eventName),
        )
    )
}

fun MeterRegistry.beskjedVarselCounter(
    runtimeEnvironment: RuntimeEnvironment,
    source: Source,
    melding: VarselMelding
) {
    varselCounter(runtimeEnvironment, source, VarselType.BESKJED, melding)
}

fun MeterRegistry.oppgaveVarselCounter(
    runtimeEnvironment: RuntimeEnvironment,
    source: Source,
    melding: VarselMelding
) {
    varselCounter(runtimeEnvironment, source, VarselType.OPPGAVE, melding)
}

fun MeterRegistry.varselCounter(
    runtimeEnvironment: RuntimeEnvironment,
    source: Source,
    varselType: VarselType,
    melding: VarselMelding
) {
    genericCounter(
        type = Type.VARSEL,
        action = Action.WRITE,
        source = source,
        target = Target.KAFKA,
        extraTags = Tags.of(
            TagKey.EVENT_TOPIC.asTag("min-side.aapen-brukervarsel-v1"),
            TagKey.EVENT_TYPE.asTag(melding::class.java.name),
            TagKey.EVENT_NAME.asTag(melding::class.java.simpleName),
            TagKey.VARSEL_TYPE.asTag(varselType),
            TagKey.VARSEL_STATUS.asTag(VarselStatus.UKJENT),
            TagKey.VARSEL_KANAL.asTag(VarselKanal.SMS),
            TagKey.VARSEL_RENOTIFIKASJON.asTag("null"),
            TagKey.VARSEL_SENDT_SOM_BATCH.asTag("null"),
            TagKey.VARSEL_NAMESPACE.asTag(runtimeEnvironment.namespaceOrDefaultForLocal()),
            TagKey.VARSEL_APP.asTag(runtimeEnvironment.appNameOrDefaultForLocal())
        )
    )
}

fun MeterRegistry.varselHendelseCounter(
    action: Action,
    hendelse: VarselHendelse
) {
    genericCounter(
        type = Type.VARSEL_HENDELSE,
        action = action,
        source = Source.KAFKA,
        target = Target.DATABASE,
        extraTags = Tags.of(
            TagKey.EVENT_TOPIC.asTag("min-side.aapen-varsel-hendelse-v1"),
            TagKey.EVENT_TYPE.asTag(hendelse::class.java.name),
            TagKey.EVENT_NAME.asTag(hendelse.eventName),
            TagKey.VARSEL_TYPE.asTag(hendelse.varseltype),
            TagKey.VARSEL_STATUS.asTag(hendelse.status),
            TagKey.VARSEL_KANAL.asTag(hendelse.kanal),
            TagKey.VARSEL_RENOTIFIKASJON.asTag(hendelse.renotifikasjon),
            TagKey.VARSEL_SENDT_SOM_BATCH.asTag(hendelse.sendtSomBatch),
            TagKey.VARSEL_NAMESPACE.asTag(hendelse.namespace),
            TagKey.VARSEL_APP.asTag(hendelse.appnavn)
        )
    )
}

fun MeterRegistry.genericCounter(
    type: Type,
    action: Action,
    source: Source,
    target: Target,
    extraTags: Tags = Tags.empty()
) {
    counter(
        "${METRIC_PREFIX}_antall_operasjoner",
        Tags.of(
            TagKey.TYPE.asTag(type),
            TagKey.ACTION.asTag(action),
            TagKey.SOURCE.asTag(source),
            TagKey.TARGET.asTag(target)
        ).and(extraTags)
    ).increment()
}
