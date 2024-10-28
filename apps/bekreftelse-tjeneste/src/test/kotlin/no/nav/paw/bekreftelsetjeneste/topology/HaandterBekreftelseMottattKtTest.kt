package no.nav.paw.bekreftelsetjeneste.topology

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.bekreftelse
import no.nav.paw.bekreftelsetjeneste.standardIntervaller
import no.nav.paw.bekreftelsetjeneste.tilstand.InternBekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelsetjeneste.tilstand.Levert
import no.nav.paw.test.days
import no.nav.paw.test.seconds
import java.time.Instant

class HaandterBekreftelseMottattKtTest : FreeSpec({
    "Når antall bekreftelser for alle tilstander er mindre enn maks antall bekreftelser, skal alle bekreftelser returneres" {
        val bekrefteler = listOf(
            standardIntervaller.bekreftelse(gracePeriodeUtloept = null),
            standardIntervaller.bekreftelse(gracePeriodeUtloept = null, gracePeriodeVarselet = null),
            standardIntervaller.bekreftelse(gracePeriodeUtloept = null, gracePeriodeVarselet = null, venterSvar = null),
            standardIntervaller.bekreftelse(gracePeriodeUtloept = null, gracePeriodeVarselet = null, venterSvar = null),
            standardIntervaller.bekreftelse(gracePeriodeUtloept = null, levert = Levert(Instant.now())),
            standardIntervaller.bekreftelse(
                gracePeriodeUtloept = null,
                internBekreftelsePaaVegneAvStartet = InternBekreftelsePaaVegneAvStartet(Instant.now())
            )
        )
        val result = bekrefteler.filterByStatusAndCount(maksAntallBekreftelserEtterStatus)
        result shouldContainExactlyInAnyOrder bekrefteler
    }
    "Når vi har flere levete bekreftelser skal vi bare beholde den siste" {
        val tid = Instant.now() + 1.days
        val input = listOf(
            standardIntervaller.bekreftelse(
                gjelderFra = tid - 64.days,
                gracePeriodeUtloept = null,
                gracePeriodeVarselet = null,
                levert = Levert(tid - 31.days)
            ),
            standardIntervaller.bekreftelse(
                gjelderFra = tid + 1.seconds,
                gracePeriodeUtloept = null,
                gracePeriodeVarselet = null,
                levert = Levert(tid + 33.days)
            ),
            standardIntervaller.bekreftelse(
                gjelderFra = tid + 1.days,
                gracePeriodeUtloept = null,
                gracePeriodeVarselet = null,
                levert = Levert(tid + 29.days)
            ),
            standardIntervaller.bekreftelse(
                gjelderFra = tid - 10.seconds,
                gracePeriodeUtloept = null,
                gracePeriodeVarselet = null,
                levert = Levert(tid + 29.days)
            )
        )
        val resultat = input.filterByStatusAndCount(mapOf(Levert::class to 1))
        withClue("Forventet en bekreftelse, men fikk $resultat") {
            resultat shouldBe listOf(input[2])
        }
    }
})