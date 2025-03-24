package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelse.internehendelser.LeveringsfristUtloept
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random.Default.nextLong

object TestData {
    fun bekreftelseHendelser(): Sequence<BekreftelseHendelse> {
        return sequence {
            while (true) {
                yieldAll(
                    listOf(
                        bekreftelseTilgjengelig(),
                        leveringsfristUtloept(),
                        bekreftelseMottatt()
                    )
                )
            }
        }
    }

    fun bekreftelseTilgjengelig(
        id: Long = nextLong(0, 20),
        timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    ): BekreftelseTilgjengelig = BekreftelseTilgjengelig(
        hendelseId = UUID.randomUUID(),
        periodeId = UUID.randomUUID(),
        arbeidssoekerId = id,
        hendelseTidspunkt = Instant.now(),
        bekreftelseId = UUID.randomUUID(),
        gjelderFra = timestamp,
        gjelderTil = timestamp.plus(14, ChronoUnit.DAYS)
    )

    fun leveringsfristUtloept(
        id: Long = nextLong(0, 20),
        timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    ): LeveringsfristUtloept = LeveringsfristUtloept(
        hendelseId = UUID.randomUUID(),
        periodeId = UUID.randomUUID(),
        arbeidssoekerId = id,
        hendelseTidspunkt = Instant.now(),
        bekreftelseId = UUID.randomUUID(),
        leveringsfrist = timestamp,
    )

    fun bekreftelseMottatt(
        id: Long = nextLong(0, 20),
        timestamp: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    ): BekreftelseMeldingMottatt = BekreftelseMeldingMottatt(
        hendelseId = UUID.randomUUID(),
        periodeId = UUID.randomUUID(),
        arbeidssoekerId = id,
        hendelseTidspunkt = timestamp,
        bekreftelseId = UUID.randomUUID(),
    )
}
