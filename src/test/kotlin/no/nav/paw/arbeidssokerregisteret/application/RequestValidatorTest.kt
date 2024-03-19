package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.fakta.navAnsattTilgangFakta
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.utils.AzureNavIdent
import no.nav.paw.arbeidssokerregisteret.utils.AzureOID
import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class RequestValidatorTest : FreeSpec({
    "Tester requestvalidator" - {

        val identitsnummer = Identitetsnummer("12345678909")
        val personInfoService: PersonInfoService = mockk()
        "Når veileder er logget inn" - {
            val requestScope = RequestScope(
                claims = ResolvedClaims()
                    .add(AzureNavIdent, "12345678909")
                    .add(AzureOID, UUID.randomUUID().toString()),
                callId = "123",
                traceparent = "123",
                navConsumerId = "123"
            )

            "Når veileder er har tilgang til bruker" - {
                val autorisasjonService: AutorisasjonService = mockk()
                coEvery {
                    autorisasjonService.verifiserVeilederTilgangTilBruker(any(), any())
                } returns true
                val requestValidator = RequestValidator(autorisasjonService, personInfoService)
                "Når forhandsgodkjent av veileder er false" - {
                    val tilgangskontrollresultat = with(requestScope) {
                        requestValidator.validerTilgang(identitsnummer)
                    }
                    tilgangskontrollresultat.opplysning shouldContain Opplysning.ANSATT_TILGANG
                    tilgangskontrollresultat.opplysning shouldNotContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                }
                "Når forhandsgodkjent av ansatt er true" - {
                    val tilgangskontrollresultat = with(requestScope) {
                        requestValidator.validerTilgang(identitsnummer, true)
                    }
                    tilgangskontrollresultat.opplysning shouldContain Opplysning.ANSATT_TILGANG
                    tilgangskontrollresultat.opplysning shouldContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                }
            }
            "Når veileder ikke har tilgang til bruker" - {
                val autorisasjonService: AutorisasjonService = mockk()
                coEvery {
                    autorisasjonService.verifiserVeilederTilgangTilBruker(any(), any())
                } returns false
                val requestValidator = RequestValidator(autorisasjonService, personInfoService)

                val tilgangskontrollresultat = with(requestScope) {
                    requestValidator.validerTilgang(identitsnummer)
                }
                tilgangskontrollresultat.opplysning shouldContain Opplysning.ANSATT_IKKE_TILGANG
            }
        }
        "Når bruker er logget inn" -{
            val requestScope = RequestScope(
                claims = ResolvedClaims()
                    .add(TokenXPID, "12345678909"),
                callId = "123",
                traceparent = "123",
                navConsumerId = "123"
            )
            val autorisasjonService: AutorisasjonService = mockk()
            val requestValidator = RequestValidator(autorisasjonService, personInfoService)

            val tilgangskontrollresultat = with(requestScope) {
                requestValidator.validerTilgang(identitsnummer)
            }
            tilgangskontrollresultat.opplysning shouldContain Opplysning.IKKE_ANSATT
            tilgangskontrollresultat.opplysning shouldNotContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
        }
        "Når bruker er logget inn og har forhåndsgodkjent av veileder flagg" -{
            val requestScope = RequestScope(
                claims = ResolvedClaims()
                    .add(TokenXPID, "12345678909"),
                callId = "123",
                traceparent = "123",
                navConsumerId = "123"
            )
            val autorisasjonService: AutorisasjonService = mockk()
            val requestValidator = RequestValidator(autorisasjonService, personInfoService)

            val tilgangskontrollresultat = with(requestScope) {
                requestValidator.validerTilgang(identitsnummer, true)
            }
            tilgangskontrollresultat.opplysning shouldContain Opplysning.IKKE_ANSATT
            tilgangskontrollresultat.opplysning shouldContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
            tilgangskontrollresultat.regel.id.shouldBe( RegelId.IKKE_ANSATT_OG_FORHAANDSGODKJENT_AV_ANSATT)
        }
    }
})
