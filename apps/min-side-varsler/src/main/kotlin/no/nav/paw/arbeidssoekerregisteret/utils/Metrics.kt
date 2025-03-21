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

private const val METRIC_PREFIX = "paw_min_side_varsler"

val Periode.eventName get(): String = avsluttet?.let { "periode.avsluttet" } ?: "periode.startet"
val BekreftelseHendelse?.eventType: String get() = this?.let { it::class.java.name } ?: "null"
val BekreftelseHendelse?.eventName: String get() = this?.hendelseType ?: "bekreftelse.null"
val VarselHendelse.verboseEventName: String get() = eventName.verboseName
val VarselMelding.eventName: String get() = "varsel.sendt"
val VarselEventName.verboseName: String get() = "varsel.$value"

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
    VARSEL_KANAL("x_varsel_kanal");

    fun asTag(value: String): Tag = Tag.of(key, value)
    fun asTag(type: Type): Tag = Tag.of(key, type.value)
    fun asTag(action: Action): Tag = Tag.of(key, action.value)
    fun asTag(source: Source): Tag = Tag.of(key, source.value)
    fun asTag(target: Target): Tag = Tag.of(key, target.value)
    fun asTag(eventName: VarselEventName): Tag = Tag.of(key, eventName.verboseName)
    fun asTag(varselType: VarselType): Tag = Tag.of(key, varselType.value)
    fun asTag(status: VarselStatus?): Tag = Tag.of(key, status?.value ?: "null")
    fun asTag(kanal: VarselKanal?): Tag = Tag.of(key, kanal?.value ?: "null")
}

fun MeterRegistry.readPeriodeCounter(periode: Periode) =
    periodeCounter(Action.READ, periode)

fun MeterRegistry.insertPeriodeCounter(periode: Periode) =
    periodeCounter(Action.INSERT, periode)

fun MeterRegistry.updatePeriodeCounter(periode: Periode) =
    periodeCounter(Action.UPDATE, periode)

fun MeterRegistry.ignorePeriodeCounter(periode: Periode) =
    periodeCounter(Action.IGNORE, periode)

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
            TagKey.VARSEL_TYPE.asTag("null"),
            TagKey.VARSEL_STATUS.asTag("null"),
            TagKey.VARSEL_KANAL.asTag("null")
        )
    )
}

fun MeterRegistry.readBekreftelseHendelseCounter(hendelse: BekreftelseHendelse?) =
    bekreftelseHendelseCounter(Action.READ, hendelse)

fun MeterRegistry.insertBekreftelseHendelseCounter(hendelse: BekreftelseHendelse?) =
    bekreftelseHendelseCounter(Action.INSERT, hendelse)

fun MeterRegistry.deleteBekreftelseHendelseCounter(hendelse: BekreftelseHendelse?) =
    bekreftelseHendelseCounter(Action.DELETE, hendelse)

fun MeterRegistry.failBekreftelseHendelseCounter(hendelse: BekreftelseHendelse?) =
    bekreftelseHendelseCounter(Action.FAIL, hendelse)

fun MeterRegistry.ignoreBekreftelseHendelseCounter(hendelse: BekreftelseHendelse?) =
    bekreftelseHendelseCounter(Action.IGNORE, hendelse)

fun MeterRegistry.bekreftelseHendelseCounter(
    action: Action,
    hendelse: BekreftelseHendelse?
) {
    genericCounter(
        type = Type.BEKREFTELSE_HENDELSE,
        action = action,
        source = Source.KAFKA,
        target = Target.DATABASE,
        extraTags = Tags.of(
            TagKey.EVENT_TOPIC.asTag("paw.arbeidssoker-bekreftelse-hendelseslogg-v1"),
            TagKey.EVENT_TYPE.asTag(hendelse.eventType),
            TagKey.EVENT_NAME.asTag(hendelse.eventName),
            TagKey.VARSEL_TYPE.asTag("null"),
            TagKey.VARSEL_STATUS.asTag("null"),
            TagKey.VARSEL_KANAL.asTag("null")
        )
    )
}

fun MeterRegistry.beskjedVarselCounter(
    source: Source,
    melding: VarselMelding
) {
    varselCounter(source, VarselType.BESKJED, melding)
}

fun MeterRegistry.oppgaveVarselCounter(
    source: Source,
    melding: VarselMelding
) {
    varselCounter(source, VarselType.OPPGAVE, melding)
}

fun MeterRegistry.varselCounter(
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
            TagKey.EVENT_NAME.asTag(melding.eventName),
            TagKey.VARSEL_TYPE.asTag(varselType),
            TagKey.VARSEL_STATUS.asTag(VarselStatus.UKJENT),
            TagKey.VARSEL_KANAL.asTag(VarselKanal.SMS)
        )
    )
}

fun MeterRegistry.readVarselHendelseCounter(hendelse: VarselHendelse) =
    varselHendelseCounter(Action.READ, hendelse)

fun MeterRegistry.insertVarselHendelseCounter(hendelse: VarselHendelse) =
    varselHendelseCounter(Action.INSERT, hendelse)

fun MeterRegistry.updateVarselHendelseCounter(hendelse: VarselHendelse) =
    varselHendelseCounter(Action.UPDATE, hendelse)

fun MeterRegistry.ignoreVarselHendelseCounter(hendelse: VarselHendelse) =
    varselHendelseCounter(Action.IGNORE, hendelse)

fun MeterRegistry.failVarselHendelseCounter(hendelse: VarselHendelse) =
    varselHendelseCounter(Action.FAIL, hendelse)

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
            TagKey.VARSEL_KANAL.asTag(hendelse.kanal)
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
