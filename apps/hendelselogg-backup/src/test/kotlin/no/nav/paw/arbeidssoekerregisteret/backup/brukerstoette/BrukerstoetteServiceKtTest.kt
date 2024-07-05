package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Tilstand
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.TilstandApiKall
import java.time.Instant
import java.util.*

class BrukerstoetteServiceKtTest : FreeSpec({
    "BrukerstoetteService" - {
        "enrich" - {
            val tilstand1 = Tilstand(
                harAktivePeriode = true,
                periodeId = UUID.randomUUID(),
                startet = Instant.now(),
                harOpplysningerMottattHendelse = true,
                avsluttet = null,
                gjeldeneOpplysningsId = UUID.randomUUID(),
                apiKall = null
            )
            "Når vi ikke har noe fra APIet settes alle til false" {
                enrich(tilstand1, emptyList()) shouldBe tilstand1.copy(apiKall = TilstandApiKall(
                    harPeriode = false,
                    harOpplysning = false,
                    harProfilering = false
                ))
            }
            "Dersom Api har perioden settes denne til true" {
                enrich(tilstand1, listOf(ApiData(tilstand1.periodeId, null, null))) shouldBe
                        tilstand1.copy(
                            apiKall = TilstandApiKall(
                                harPeriode = true,
                                harOpplysning = false,
                                harProfilering = false
                            )
                        )
            }
            "Dersom Api har perioden og oplysningene settes disse til true" {
                enrich(tilstand1, listOf(ApiData(tilstand1.periodeId, tilstand1.gjeldeneOpplysningsId, null))) shouldBe
                        tilstand1.copy(
                            apiKall = TilstandApiKall(
                                harPeriode = true,
                                harOpplysning = true,
                                harProfilering = false
                            )
                        )
            }
            "Dersom Api har perioden og profilering settes disse til true" {
                enrich(
                    tilstand1,
                    listOf(
                        ApiData(
                            tilstand1.periodeId,
                            tilstand1.gjeldeneOpplysningsId,
                            tilstand1.gjeldeneOpplysningsId
                        )
                    )
                ) shouldBe
                        tilstand1.copy(
                            apiKall = TilstandApiKall(
                                harPeriode = true,
                                harOpplysning = true,
                                harProfilering = true
                            )
                        )
            }
        }
    }
})