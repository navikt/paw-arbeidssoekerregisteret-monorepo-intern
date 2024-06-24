package no.nav.paw.arbeidssokerregisteret.services

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.TestData
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeAnsatt
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.navAnsattTilgangFakta
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.poao_tilgang.client.api.ApiResult

class AutorisasjonServiceTest : FreeSpec({
    "verifiserVeilederTilgangTilBruker should return true if access is granted" {
        val poaoTilgangHttpClient = mockk<PoaoTilgangHttpClient>()
        val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

        val navAnsatt = TestData.navAnsatt
        val foedselsnummer = TestData.foedselsnummer

        every {
            poaoTilgangHttpClient.evaluatePolicy(any())
        } returns ApiResult(
            throwable = null,
            result = Decision.Permit
        )

        val result = autorisasjonService.verifiserVeilederTilgangTilBruker(navAnsatt, foedselsnummer)

        result shouldBe true
    }

    "verifiserVeilederTilgangTilBruker should return false if access is denied" {
        val poaoTilgangHttpClient = mockk<PoaoTilgangHttpClient>()
        val autorisasjonService = AutorisasjonService(poaoTilgangHttpClient)

        val navAnsatt = TestData.navAnsatt
        val foedselsnummer = TestData.foedselsnummer

        every {
            poaoTilgangHttpClient.evaluatePolicy(any())
        } returns ApiResult(
            throwable = null,
            result = Decision.Deny("", "")
        )

        val result = autorisasjonService.verifiserVeilederTilgangTilBruker(navAnsatt, foedselsnummer)

        result shouldBe false
    }
    "verifiser IKKE_NAVANSATT" {
        val autorisasjonService = mockk<AutorisasjonService>()
        val identitet =  Identitetsnummer("12345678909")
        val requestScope = RequestScope(
            claims = ResolvedClaims().add(TokenXPID, "12345678909"),
            callId = "123",
            traceparent = "123",
            navConsumerId = "123",
            path = "test"
        )
        with (requestScope) {
            autorisasjonService.navAnsattTilgangFakta(identitet) shouldBe IkkeAnsatt
        }
    }
})
