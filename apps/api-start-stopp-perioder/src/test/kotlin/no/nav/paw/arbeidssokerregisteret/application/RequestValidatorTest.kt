package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.AnsattIkkeTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.AnsattTilgang
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning.IkkeAnsatt
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.*
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgForhaandsgodkjentAvAnsatt
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.utils.AzureNavIdent
import no.nav.paw.arbeidssokerregisteret.utils.AzureOID
import no.nav.paw.arbeidssokerregisteret.utils.ResolvedClaims
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.paw.pdl.graphql.generated.enums.Endringstype
import no.nav.paw.pdl.graphql.generated.hentperson.*
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
                navConsumerId = "123",
                path = "test"
            )

            "Når veileder er har tilgang til bruker" - {
                val autorisasjonService: AutorisasjonService = mockk()
                coEvery {
                    autorisasjonService.verifiserVeilederTilgangTilBruker(any(), any())
                } returns true
                val requestValidator = RequestValidator(autorisasjonService, personInfoService)
                "Når forhandsgodkjent av veileder er false" {
                    val tilgangskontrollresultat = with(requestScope) {
                        requestValidator.validerTilgang(identitsnummer)
                    }.shouldBeInstanceOf<Either.Right<OK>>()
                    tilgangskontrollresultat.value.opplysning shouldContain AnsattTilgang
                    tilgangskontrollresultat.value.opplysning shouldNotContain DomeneOpplysning.ErForhaandsgodkjent
                }
                "Når forhandsgodkjent av ansatt er true" {
                    val tilgangskontrollresultat = with(requestScope) {
                        requestValidator.validerTilgang(identitsnummer, true)
                    }.shouldBeInstanceOf<Either.Right<OK>>()
                    tilgangskontrollresultat.value.opplysning shouldContain AnsattTilgang
                    tilgangskontrollresultat.value.opplysning shouldContain DomeneOpplysning.ErForhaandsgodkjent
                }
            }
            "Når veileder ikke har tilgang til bruker" {
                val autorisasjonService: AutorisasjonService = mockk()
                coEvery {
                    autorisasjonService.verifiserVeilederTilgangTilBruker(any(), any())
                } returns false
                val requestValidator = RequestValidator(autorisasjonService, personInfoService)

                val tilgangskontrollresultat = with(requestScope) {
                    requestValidator.validerTilgang(identitsnummer)
                }.shouldBeInstanceOf<Either.Left<Problem>>()
                tilgangskontrollresultat.value.opplysning shouldContain AnsattIkkeTilgang
            }
        }
        "Når bruker er logget inn" - {
            val requestScope = RequestScope(
                claims = ResolvedClaims()
                    .add(TokenXPID, "12345678909"),
                callId = "123",
                traceparent = "123",
                navConsumerId = "123",
                path = "test"
            )
            val autorisasjonService: AutorisasjonService = mockk()
            val requestValidator = RequestValidator(autorisasjonService, personInfoService)
            "standardbruker" {
                val tilgangskontrollresultat = with(requestScope) {
                    requestValidator.validerTilgang(identitsnummer)
                }.shouldBeInstanceOf<Either.Right<OK>>()
                tilgangskontrollresultat.value.opplysning shouldContain IkkeAnsatt
                tilgangskontrollresultat.value.opplysning shouldNotContain DomeneOpplysning.ErForhaandsgodkjent
            }
            "forhåndsgodkjentflagg" {
                val tilgangskontrollresultat = with(requestScope) {
                    requestValidator.validerTilgang(identitsnummer, true)
                }.shouldBeInstanceOf<Either.Left<Problem>>()
                tilgangskontrollresultat.value.opplysning shouldContain IkkeAnsatt
                tilgangskontrollresultat.value.opplysning shouldContain DomeneOpplysning.ErForhaandsgodkjent
                tilgangskontrollresultat.value.regel.id.shouldBe(IkkeAnsattOgForhaandsgodkjentAvAnsatt)
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
                        navConsumerId = "123",
                        path = "test"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(
                            Folkeregisterpersonstatus(
                                forenkletStatus = "bosattEtterFolkeregisterloven",
                                metadata = Metadata(
                                    endringer = emptyList()
                                )
                            )
                        ),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    "godkjent av veilederflagg er true" {
                        val resultat = with(requestScope) {
                            requestValidator.validerStartAvPeriodeOenske(identitsnummer, true)
                        }.shouldBeInstanceOf<Either.Left<Problem>>()
                        resultat.value.opplysning shouldContain IkkeAnsatt
                        resultat.value.opplysning shouldContain DomeneOpplysning.ErForhaandsgodkjent
                    }

                    "godkjent av veileder er false" {
                        val resultat = with(requestScope) {
                            requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                        }.shouldBeInstanceOf<Either.Right<OK>>()
                        resultat.value.opplysning shouldContain IkkeAnsatt
                        resultat.value.opplysning shouldNotContain DomeneOpplysning.ErForhaandsgodkjent
                    }
                }

                "Bruker ikke bosatt" {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123",
                        path = "test"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(
                            Folkeregisterpersonstatus(
                                forenkletStatus = "ikkeBosatt",
                                metadata = Metadata(
                                    endringer = emptyList()
                                )
                            )
                        ),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }.shouldBeInstanceOf<Either.Left<Problem>>()
                    resultat.value.opplysning shouldContain DomeneOpplysning.IkkeBosatt
                    resultat.value.regel.id shouldBe IkkeBosattINorgeIHenholdTilFolkeregisterloven
                }
                "Bruker har dNummer" {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123",
                        path = "test"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(
                            Folkeregisterpersonstatus(
                                forenkletStatus = "ikkeBosatt",
                                metadata = Metadata(
                                    endringer = emptyList()
                                )
                            ),
                            Folkeregisterpersonstatus(
                                forenkletStatus = "dNummer",
                                metadata = Metadata(
                                    endringer = emptyList()
                                )
                            )
                        ),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }.shouldBeInstanceOf<Either.Left<Problem>>()
                    resultat.value.opplysning shouldContain DomeneOpplysning.IkkeBosatt
                    resultat.value.opplysning shouldContain DomeneOpplysning.Dnummer
                    resultat.value.regel.id shouldBe IkkeBosattINorgeIHenholdTilFolkeregisterloven
                }
                "Person ikke funnet" {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123",
                        path = "test"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns null
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }.shouldBeInstanceOf<Either.Left<Problem>>()
                    resultat.value.opplysning shouldContain DomeneOpplysning.PersonIkkeFunnet
                    resultat.value.regel.id shouldBe IkkeFunnet
                }
                "Ukjent alder" {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123",
                        path = "test"
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
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }.shouldBeInstanceOf<Either.Left<Problem>>()
                    resultat.value.opplysning shouldContain DomeneOpplysning.UkjentFoedselsdato
                }
                "Registrert som død" {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123",
                        path = "test"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(
                            Folkeregisterpersonstatus(
                                forenkletStatus = "doedIFolkeregisteret",
                                metadata = Metadata(
                                    endringer = emptyList()
                                )
                            )
                        ),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }.shouldBeInstanceOf<Either.Left<Problem>>()
                    resultat.value.opplysning shouldContain DomeneOpplysning.ErDoed
                }
                "Registrert som savnet" {
                    val requestScope = RequestScope(
                        claims = ResolvedClaims()
                            .add(TokenXPID, "12345678909"),
                        callId = "123",
                        traceparent = "123",
                        navConsumerId = "123",
                        path = "test"
                    )
                    coEvery {
                        with(requestScope) {
                            personInfoService.hentPersonInfo(identitsnummer.verdi)
                        }
                    } returns Person(
                        foedsel = listOf(Foedsel("2000-01-01", 2000)),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = listOf(
                            Folkeregisterpersonstatus(
                                forenkletStatus = "forsvunnet",
                                metadata = Metadata(
                                    endringer = listOf(
                                        Endring(
                                            type = Endringstype.OPPRETT,
                                            registrert = "2021-01-01",
                                            kilde = "test",
                                        )
                                    )
                                )
                            )
                        ),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    val resultat = with(requestScope) {
                        requestValidator.validerStartAvPeriodeOenske(identitsnummer)
                    }.shouldBeInstanceOf<Either.Left<Problem>>()
                    resultat.value.opplysning shouldContain DomeneOpplysning.ErSavnet
                }

            }
        }
    }
})

