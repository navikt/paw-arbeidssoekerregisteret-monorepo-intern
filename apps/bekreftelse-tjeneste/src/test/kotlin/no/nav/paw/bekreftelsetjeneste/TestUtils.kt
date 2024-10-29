package no.nav.paw.bekreftelsetjeneste

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseKonfigurasjon
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import no.nav.paw.test.days
import java.time.Instant
import java.util.*

val standardIntervaller = BekreftelseKonfigurasjon(
    migreringstidspunkt = Instant.parse("2024-09-29T22:00:00Z"),
    interval = 14.days,
    tilgjengeligOffset = 3.days,
    graceperiode = 7.days,
    varselFoerGraceperiodeUtloept = 3.days
)

inline fun <T1: Any, reified T2: T1> List<T1>.assertExactlyOne(f: T2.() -> Unit) =
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
    avsluttet: Instant? = null,
    bekreftelser: List<Bekreftelse> = emptyList()
) = BekreftelseTilstand(
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