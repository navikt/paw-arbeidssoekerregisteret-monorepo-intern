package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.assertions.any
import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.Row2
import io.kotest.data.Row3
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.fakta.navAnsattTilgangFakta
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.requestScope
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.utils.AzureNavIdent
import no.nav.paw.arbeidssokerregisteret.utils.AzureOID
import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.paw.pdl.graphql.generated.hentperson.Foedsel
import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import org.junit.jupiter.api.Assertions.*
import java.util.*

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
        "Når bruker er logget inn" - {
            val requestScope = RequestScope(
                claims = ResolvedClaims()
                    .add(TokenXPID, "12345678909"),
                callId = "123",
                traceparent = "123",
                navConsumerId = "123"
            )
            val autorisasjonService: AutorisasjonService = mockk()
            val requestValidator = RequestValidator(autorisasjonService, personInfoService)
            "standardbruker" - {
                val tilgangskontrollresultat = with(requestScope) {
                    requestValidator.validerTilgang(identitsnummer)
                }
                tilgangskontrollresultat.opplysning shouldContain Opplysning.IKKE_ANSATT
                tilgangskontrollresultat.opplysning shouldNotContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
            }
            "forhåndsgodkjentflagg" - {
                val tilgangskontrollresultat = with(requestScope) {
                    requestValidator.validerTilgang(identitsnummer, true)
                }
                tilgangskontrollresultat.opplysning shouldContain Opplysning.IKKE_ANSATT
                tilgangskontrollresultat.opplysning shouldContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                tilgangskontrollresultat.regel.id.shouldBe(RegelId.IKKE_ANSATT_OG_FORHAANDSGODKJENT_AV_ANSATT)
            }
        }

        "Sjekker valider start av periode" - {
            val identitsnummer = Identitetsnummer("12345678909")
            val personInfoService: PersonInfoService = mockk()
            "Når bruker er innlogget" - {

                val autorisasjonService: AutorisasjonService = mockk()
                val requestValidator = RequestValidator(autorisasjonService, personInfoService)
                "over 18 år og bosatt etter folketrygdloven" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosattEtterFolkeregisterloven")),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                    "godkjent av veilederflagg er true" - {
                        val resultat = with(requestScope) {
                            requestValidator.validerStartAvPeriodeOenske(identitsnummer, true)
                        }
                        resultat.opplysning shouldContain Opplysning.IKKE_ANSATT
                        resultat.opplysning shouldContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                    }

                    "godkjent av veileder er false" - {
                        val resultat = with(requestScope) {
                            requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                        }
                        resultat.opplysning shouldContain Opplysning.IKKE_ANSATT
                        resultat.opplysning shouldNotContain Opplysning.FORHAANDSGODKJENT_AV_ANSATT
                    }
                }

                "Bruker ikke bosatt" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("ikkeBosatt")),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }
                    resultat.opplysning shouldContain Opplysning.IKKE_BOSATT

                }
                "Bruker har dNummer" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(
                            Folkeregisterpersonstatus("ikkeBosatt"),
                            Folkeregisterpersonstatus("dNummer")
                        ),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }
                    resultat.opplysning shouldContain Opplysning.IKKE_BOSATT
                    resultat.opplysning shouldContain Opplysning.DNUMMER
                }
                "Person ikke funnet" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns null
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }
                    resultat.opplysning shouldContain Opplysning.PERSON_IKKE_FUNNET
                }
                "Ukjent alder" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = emptyList(),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = emptyList(),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }
                    resultat.opplysning shouldContain Opplysning.UKJENT_FOEDSELSDATO
                }
                "Registrert som død" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("doedIFolkeregisteret")),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }
                    resultat.opplysning shouldContain Opplysning.DOED
                }
                "Registrert som savnet" - {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("forsvunnet")),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList()
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }
                    resultat.opplysning shouldContain Opplysning.SAVNET
                }

            }
        }
    }
})

