package no.nav.paw.bekreftelsetjeneste.bekreftelsegenerering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.bekreftelsetjeneste.paavegneav.WallClock
import no.nav.paw.bekreftelsetjeneste.testutils.gracePeriodeUtloeper
import no.nav.paw.bekreftelsetjeneste.testutils.bekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.testutils.standardIntervaller
import no.nav.paw.bekreftelsetjeneste.testutils.tilgjengelig
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.topology.prosesserBekreftelseOgPaaVegneAvTilstand
import no.nav.paw.test.days
import no.nav.paw.test.seconds
import java.time.Instant

class NaarPaaVegneAvStoppes : FreeSpec({
    val intervaller = standardIntervaller.copy(
        migreringstidspunkt = Instant.parse("2022-01-27T23:12:11Z"),
    )
    val startTid = Instant.parse("2023-01-27T23:12:11Z")
    val tilstand = bekreftelseTilstand(periodeStart = startTid)
    "Når paaVegneAv stoppes" - {
        "og ingen tidligere bekreftelser finnes" - {
            "bare opprettes en intern bekreftelse før ${startTid + intervaller.interval - intervaller.tilgjengeligOffset}" {
                val (interntTilstand, hendelser) = sequenceOf(tilstand to null)
                    .prosesserBekreftelseOgPaaVegneAvTilstand(
                        bekreftelseKonfigurasjon = intervaller,
                        wallClock = WallClock(startTid + 1.days)
                    ).first()
                interntTilstand shouldNotBe tilstand
                interntTilstand.bekreftelser.size shouldBe 1
                interntTilstand.bekreftelser.first().gjelderFra shouldBe startTid
                interntTilstand.bekreftelser.first().tilstandsLogg.siste.shouldBeInstanceOf<IkkeKlarForUtfylling>()
                interntTilstand.bekreftelser.first().gjelderTil shouldBe Instant.parse("2023-02-12T23:00:00Z")
                hendelser.shouldBeEmpty()
            }
            "genereres det en ny bekreftelse etter ${intervaller.tilgjengelig(startTid)}" {
                val (interntTilstand, hendelser) = sequenceOf(tilstand to null)
                    .prosesserBekreftelseOgPaaVegneAvTilstand(
                        bekreftelseKonfigurasjon = intervaller,
                        wallClock = WallClock(intervaller.tilgjengelig(startTid) + 1.seconds)
                    ).first()
                interntTilstand shouldNotBe tilstand
                interntTilstand.bekreftelser.size shouldBe 1
                interntTilstand.bekreftelser.first().gjelderFra shouldBe startTid
                interntTilstand.bekreftelser.first().gjelderTil shouldBe Instant.parse("2023-02-12T23:00:00Z")
                interntTilstand.bekreftelser.first().tilstandsLogg.siste.shouldBeInstanceOf<KlarForUtfylling>()
                hendelser.size shouldBe 1
                hendelser.first().shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
            "genereres det 2 nye bekreftelser etter ${intervaller.gracePeriodeUtloeper(startTid) - 1.seconds + intervaller.interval + intervaller.graceperiode}" {
                val wallClock = WallClock(intervaller.gracePeriodeUtloeper(startTid) - 1.seconds + intervaller.interval + intervaller.graceperiode)
                val (interntTilstand1, hendelser1) = sequenceOf(tilstand to null)
                    .prosesserBekreftelseOgPaaVegneAvTilstand(
                        bekreftelseKonfigurasjon = intervaller,
                        wallClock = wallClock
                    ).first()
                val (interntTilstand, hendelser2) = sequenceOf(interntTilstand1 to null)
                    .prosesserBekreftelseOgPaaVegneAvTilstand(
                        bekreftelseKonfigurasjon = intervaller,
                        wallClock = wallClock
                    ).first()
                val hendelser = hendelser1 + hendelser2
                interntTilstand shouldNotBe tilstand
                interntTilstand.bekreftelser.size shouldBe 2
                interntTilstand.bekreftelser[0].gjelderFra shouldBe startTid
                interntTilstand.bekreftelser[0].gjelderTil shouldBe Instant.parse("2023-02-12T23:00:00Z")
                interntTilstand.bekreftelser[0].tilstandsLogg.siste.shouldBeInstanceOf<KlarForUtfylling>()
                interntTilstand.bekreftelser[1].gjelderFra shouldBe interntTilstand.bekreftelser[0].gjelderTil
                interntTilstand.bekreftelser[1].gjelderTil shouldBe interntTilstand.bekreftelser[0].gjelderTil + intervaller.interval
                interntTilstand.bekreftelser[1].tilstandsLogg.siste.shouldBeInstanceOf<KlarForUtfylling>()
                hendelser.size shouldBe 2
                hendelser[0].shouldBeInstanceOf<BekreftelseTilgjengelig>()
                hendelser[1].shouldBeInstanceOf<BekreftelseTilgjengelig>()
            }
        }
        "uten at noen bekreftelser er levert, skal det genereres en ny bekreftelse som starter ved periode start" - {
            val (interntTilstand, hendelser) = sequenceOf(tilstand to null)
                .prosesserBekreftelseOgPaaVegneAvTilstand(
                    bekreftelseKonfigurasjon = intervaller,
                    wallClock = WallClock(startTid + 1.days)
                ).first()
            interntTilstand shouldNotBe tilstand
            interntTilstand.bekreftelser.size shouldBe 1
            interntTilstand.bekreftelser.first().gjelderFra shouldBe startTid
            interntTilstand.bekreftelser.first().tilstandsLogg.siste.shouldBeInstanceOf<IkkeKlarForUtfylling>()
            interntTilstand.bekreftelser.first().gjelderTil shouldBe Instant.parse("2023-02-12T23:00:00Z")
            hendelser.shouldBeEmpty()
        }
    }
})