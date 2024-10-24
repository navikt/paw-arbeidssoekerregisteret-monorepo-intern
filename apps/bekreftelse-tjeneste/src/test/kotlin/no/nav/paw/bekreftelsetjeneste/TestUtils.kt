package no.nav.paw.bekreftelsetjeneste

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.config.BekreftelseIntervals
import no.nav.paw.bekreftelsetjeneste.tilstand.*
import java.time.Instant
import java.util.*

inline fun <T1: Any, reified T2: T1> List<T1>.assertExactlyOne(f: T2.() -> Unit) =
    filterIsInstance<T2>()
        .let {
            withClue("Expected exactly one ${T2::class.simpleName} but found ${it.size}") {
                it.size shouldBe 1
            }
            it.first()
        }.apply(f)

fun internTilstand(
    periodeStart: Instant,
    periodeId: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678901",
    arbeidsoekerId: Long = 1L,
    recordKey: Long = 1L,
    avsluttet: Instant? = null,
    bekreftelser: List<Bekreftelse> = emptyList()
) = InternTilstand(
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

fun BekreftelseIntervals.bekreftelse(
    gjelderFra: Instant = Instant.now(),
    gjelderTil: Instant = gjelderFra + interval,
    ikkeKlarForUtfylling: IkkeKlarForUtfylling? = IkkeKlarForUtfylling(gjelderFra),
    klarForUtfylling: KlarForUtfylling? = KlarForUtfylling(gjelderFra + interval - tilgjengeligOffset),
    venterSvar: VenterSvar? = VenterSvar(gjelderFra + interval),
    gracePeriodeVarselet: GracePeriodeVarselet? = GracePeriodeVarselet(gjelderFra + interval + varselFoerGraceperiodeUtloept),
    gracePeriodeUtloept: GracePeriodeUtloept? = GracePeriodeUtloept(gjelderFra + interval + graceperiode)
): Bekreftelse = Bekreftelse(
    tilstandsLogg = listOfNotNull(
        ikkeKlarForUtfylling,
        klarForUtfylling,
        venterSvar,
        gracePeriodeVarselet,
        gracePeriodeUtloept
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

fun BekreftelseIntervals.gracePeriodeVarsel(startTid: Instant): Instant =
    startTid.plus(interval).plus(graceperiode).minus(varselFoerGraceperiodeUtloept)

fun BekreftelseIntervals.gracePeriodeUtloeper(startTid: Instant): Instant =
    startTid.plus(interval).plus(graceperiode)

fun BekreftelseIntervals.tilgjengelig(startTid: Instant): Instant = startTid.plus(interval).plus(tilgjengeligOffset)

fun BekreftelseIntervals.frist(startTid: Instant): Instant = startTid.plus(interval)