package no.nav.paw.bekreftelsetjeneste.testutils

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.EksternGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeGjenstaaendeTid
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloeptEtterEksternInnsamling
import no.nav.paw.bekreftelsetjeneste.ApplicationTestContext
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.test.days
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

val standardIntervaller = BekreftelseKonfigurasjon(
    tidligsteBekreftelsePeriodeStart = LocalDate.parse("2024-09-29"),
    interval = 14.days,
    tilgjengeligOffset = 3.days,
    graceperiode = 7.days,
    varselFoerGraceperiodeUtloept = 3.days
)

inline fun <T1 : Any, reified T2 : T1> List<T1>.assertExactlyOne(f: T2.() -> Unit) =
    filterIsInstance<T2>()
        .let {
            withClue("Expected exactly one ${T2::class.simpleName} but found ${it.size}") {
                it.size shouldBe 1
            }
            it.first()
        }.apply(f)

fun bekreftelseTilstand(
    periodeStart: Instant,
    periodeId: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678901",
    arbeidsoekerId: Long = 1L,
    recordKey: Long = 1L,
    kafkaPartition: Int = 0,
    avsluttet: Instant? = null,
    bekreftelser: List<Bekreftelse> = emptyList()
) = BekreftelseTilstand(
    kafkaPartition = kafkaPartition,
    periode = PeriodeInfo(
        periodeId = periodeId,
        identitetsnummer = identitetsnummer,
        arbeidsoekerId = arbeidsoekerId,
        recordKey = recordKey,
        startet = periodeStart,
        avsluttet = avsluttet
    ),
    bekreftelser = bekreftelser
)

fun BekreftelseKonfigurasjon.bekreftelse(
    gjelderFra: Instant = Instant.now(),
    gjelderTil: Instant = gjelderFra + interval,
    ikkeKlarForUtfylling: IkkeKlarForUtfylling? = IkkeKlarForUtfylling(gjelderFra),
    klarForUtfylling: KlarForUtfylling? = KlarForUtfylling(gjelderFra + interval - tilgjengeligOffset),
    venterSvar: VenterSvar? = VenterSvar(gjelderFra + interval),
    gracePeriodeVarselet: GracePeriodeVarselet? = GracePeriodeVarselet(gjelderFra + interval + varselFoerGraceperiodeUtloept),
    gracePeriodeUtloept: GracePeriodeUtloept? = GracePeriodeUtloept(gjelderFra + interval + graceperiode),
    levert: Levert? = null,
    internBekreftelsePaaVegneAvStartet: InternBekreftelsePaaVegneAvStartet? = null,
): Bekreftelse = Bekreftelse(
    tilstandsLogg = listOfNotNull(
        ikkeKlarForUtfylling,
        klarForUtfylling,
        venterSvar,
        gracePeriodeVarselet,
        gracePeriodeUtloept,
        levert,
        internBekreftelsePaaVegneAvStartet
    ).sortedBy { it.timestamp }
        .let {
            BekreftelseTilstandsLogg(
                siste = it.last(),
                tidligere = it.dropLast(1)
            )
        },
    bekreftelseId = UUID.randomUUID(),
    gjelderFra = gjelderFra,
    gjelderTil = gjelderTil
)

fun BekreftelseKonfigurasjon.gracePeriodeVarsel(startTid: Instant): Instant =
    startTid.plus(interval).plus(graceperiode).minus(varselFoerGraceperiodeUtloept)

fun BekreftelseKonfigurasjon.gracePeriodeUtloeper(startTid: Instant): Instant =
    startTid.plus(interval).plus(graceperiode)

fun BekreftelseKonfigurasjon.tilgjengelig(startTid: Instant): Instant = startTid.plus(interval).plus(tilgjengeligOffset)

fun BekreftelseKonfigurasjon.frist(startTid: Instant): Instant = startTid.plus(interval)
fun ApplicationTestContext.internt_varsel_hendelse_skal_være_publisert(gjenstaaendeTid: Duration) {
    withClue("Bekreftelse hendelse er publisert") {
        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
    }
    withClue("Bekreftelse hendelse skal være RegisterGracePeriodeGjenstaaendeTid med gjenstående tid $gjenstaaendeTid") {
        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
        kv.value.shouldBeInstanceOf<RegisterGracePeriodeGjenstaaendeTid>().gjenstaandeTid shouldBe gjenstaaendeTid
    }
}

fun ApplicationTestContext.bekreftelse_siste_frist_utloept(sisteFrist: Instant) {
    withClue("Bekreftelse hendelse er publisert") {
        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
    }
    withClue("Bekreftelse hendelse skal være RegisterGracePeriodeUtloept klokken $sisteFrist") {
        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
        kv.value.shouldBeInstanceOf<RegisterGracePeriodeUtloept>()
    }
}

fun ApplicationTestContext.bekreftelse_venter_på_svar(fristUtloept: Instant) {
    withClue("Bekreftelse hendelse er publisert") {
        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
    }
    withClue("Bekreftelse hendelse skal være LeveringsfristUtloept klokken $fristUtloept") {
        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
        val hendelse = kv.value.shouldBeInstanceOf<LeveringsfristUtloept>()
        hendelse.leveringsfrist shouldBe fristUtloept
    }
}

fun ApplicationTestContext.bekreftelse_er_tilgjengelig(
    fra: Instant,
    til: Instant
) {
    withClue("Bekreftelse hendelse er publisert") {
        bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
    }
    val bekreftelseTilgjengelig = withClue("Hendelsen er av type BekreftelseTilgjengelig") {
        val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
        kv.value.shouldBeInstanceOf<BekreftelseTilgjengelig>()
    }
    withClue("Fra og til for bekreftelsen er riktig") {
        bekreftelseTilgjengelig.gjelderFra shouldBe fra
        bekreftelseTilgjengelig.gjelderTil shouldBe til
    }
}

fun ApplicationTestContext.ingen_flere_hendelser() {
    withClue("Ingen flere hendelser skal være publisert klokken ${wallclock.get()}") {
        bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
    }
}

fun ApplicationTestContext.ingen_bekreftelser_skal_være_publisert() {
    withClue("Ingen bekreftelser skal være publisert klokken ${wallclock.get()}") {
        bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
    }
}

fun ApplicationTestContext.vi_stiller_klokken_frem(tid: Duration) {
    testDriver.advanceWallClockTime(tid)
}

fun setOppTest(
    datoOgKlokkeslettVedStart: Instant,
    tidlistBekreftelsePeriodeStart: LocalDate? = null,
    bekreftelseIntervall: Duration,
    tilgjengeligOffset: Duration,
    innleveringsfrist: Duration
): ApplicationTestContext {
    return ApplicationTestContext(
        initialWallClockTime = datoOgKlokkeslettVedStart,
        bekreftelseKonfigurasjon = BekreftelseKonfigurasjon(
            tidligsteBekreftelsePeriodeStart = tidlistBekreftelsePeriodeStart
                ?: (datoOgKlokkeslettVedStart - 30.dager).atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
            interval = bekreftelseIntervall,
            graceperiode = innleveringsfrist,
            tilgjengeligOffset = tilgjengeligOffset,
        )
    )
}

val Int.dager: Duration get() = Duration.ofDays(this.toLong())
val Int.timer: Duration get() = Duration.ofHours(this.toLong())
private val format = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
val String.timestamp: Instant get() = LocalDateTime.parse(this, format).atZone(ZoneId.of("Europe/Oslo")).toInstant()
private val prettyPrintFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("cccc dd.MM.yyyy HH:mm ('uke' w)")
val Instant.prettyPrint: String
    get() = LocalDateTime.ofInstant(this, ZoneId.systemDefault()).format(prettyPrintFormat)
        .replace("Monday", "Mandag")
        .replace("Tuesday", "Tirsdag")
        .replace("Wednesday", "Onsdag")
        .replace("Thursday", "Torsdag")
        .replace("Friday", "Fredag")
        .replace("Saturday", "Lørdag")
        .replace("Sunday", "Søndag")

fun BekreftelseHendelse.prettyPrint(): String {
    val name = this::class.java.simpleName
    val header = "${name.first().lowercase()}${
        name.drop(1).map { if (it.isUpperCase()) " ${it.lowercase()}" else it }.joinToString("")
    }"
        .replace("oe", "ø")
        .replace("aa", "å")
        .replace("ae", "æ")
        .replace("grace periode", "siste frist")
    val detaljer = when (this) {
        is BaOmAaAvsluttePeriode -> null
        is BekreftelseMeldingMottatt -> "\tbekreftelse_id=${this.bekreftelseId}"
        is BekreftelsePaaVegneAvStartet -> null
        is BekreftelseTilgjengelig -> "\tgjelder, fra: ${this.gjelderFra.prettyPrint}, til: ${this.gjelderTil.prettyPrint}, bekreftelse_id=${this.bekreftelseId}"
        is EksternGracePeriodeUtloept -> TODO()
        is LeveringsfristUtloept -> "\tfrist utløpt: ${this.leveringsfrist.prettyPrint}, bekreftelse_id=${this.bekreftelseId}"
        is PeriodeAvsluttet -> null
        is RegisterGracePeriodeGjenstaaendeTid -> "\tgjenstående tid: ${this.gjenstaandeTid}, bekreftelse_id=${this.bekreftelseId}"
        is RegisterGracePeriodeUtloept -> "\tperiode avsluttes, bekreftelse_id=${this.bekreftelseId}"
        is RegisterGracePeriodeUtloeptEtterEksternInnsamling -> null
    }
    return if (detaljer == null) {
        header
    } else {
        "$header\n$detaljer"
    }
}