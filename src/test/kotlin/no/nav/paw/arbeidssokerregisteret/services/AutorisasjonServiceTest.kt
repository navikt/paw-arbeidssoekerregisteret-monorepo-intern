package no.nav.paw.arbeidssokerregisteret.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.TestData
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.poao_tilgang.client.api.ApiResult

class AutorisasjonServiceTest : FunSpec({
    test("verifiserVeilederTilgangTilBruker should return true if access is granted") {
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

    test("verifiserVeilederTilgangTilBruker should return false if access is denied") {
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
})
