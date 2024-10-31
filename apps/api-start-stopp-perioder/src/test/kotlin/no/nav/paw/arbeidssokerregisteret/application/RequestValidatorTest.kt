package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.NonEmptyList
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
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
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
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
                val requestValidator = RequestValidator(autorisasjonService, personInfoService, InngangsReglerV2, registry)
                "Når forhandsgodkjent av veileder er false" {
                    val tilgangskontrollresultat = requestValidator.validerTilgang(requestScope = requestScope, identitetsnummer = identitsnummer)
                        .shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
                    tilgangskontrollresultat.value.opplysning shouldContain AnsattTilgang
                    tilgangskontrollresultat.value.opplysning shouldNotContain DomeneOpplysning.ErForhaandsgodkjent
                }
                "Når forhandsgodkjent av ansatt er true" {
                    val tilgangskontrollresultat = requestValidator.validerTilgang(requestScope, identitsnummer, true)
                        .shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
                    tilgangskontrollresultat.value.opplysning shouldContain AnsattTilgang
                    tilgangskontrollresultat.value.opplysning shouldContain DomeneOpplysning.ErForhaandsgodkjent
                }
            }
            "Når veileder ikke har tilgang til bruker" {
                val autorisasjonService: AutorisasjonService = mockk()
                coEvery {
                    autorisasjonService.verifiserVeilederTilgangTilBruker(any(), any())
                } returns false
                val requestValidator = RequestValidator(autorisasjonService, personInfoService, InngangsReglerV2, registry)

                val tilgangskontrollresultat = requestValidator.validerTilgang(requestScope, identitsnummer)
                    .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                tilgangskontrollresultat.value.head.opplysninger shouldContain AnsattIkkeTilgang
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
            val requestValidator = RequestValidator(autorisasjonService, personInfoService, InngangsReglerV2, registry)
            "standardbruker" {
                val tilgangskontrollresultat = requestValidator.validerTilgang(requestScope, identitsnummer)
                    .shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
                tilgangskontrollresultat.value.opplysning shouldContain IkkeAnsatt
                tilgangskontrollresultat.value.opplysning shouldNotContain DomeneOpplysning.ErForhaandsgodkjent
            }
            "forhåndsgodkjentflagg" {
                val tilgangskontrollresultat = requestValidator.validerTilgang(requestScope, identitsnummer, true)
                    .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                tilgangskontrollresultat.value.head.opplysninger shouldContain IkkeAnsatt
                tilgangskontrollresultat.value.head.opplysninger shouldContain DomeneOpplysning.ErForhaandsgodkjent
                tilgangskontrollresultat.value.head.regel.id.shouldBe(IkkeAnsattOgForhaandsgodkjentAvAnsatt)
            }
        }

        "Sjekker valider start av periode" - {
            val identitsnummer = Identitetsnummer("12345678909")
            val personInfoService: PersonInfoService = mockk()
            "Når bruker er innlogget" - {

                val autorisasjonService: AutorisasjonService = mockk()
                val requestValidator = RequestValidator(autorisasjonService, personInfoService, InngangsReglerV2, registry)
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
                        personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns Person(
                        foedselsdato = listOf(Foedselsdato("2000-01-01", 2000)),
                        foedested = listOf(Foedested("NOR", "Norge", "NO")),
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
                        val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer, true)
                            .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                        resultat.value.head.opplysninger shouldContain IkkeAnsatt
                        resultat.value.head.opplysninger shouldContain DomeneOpplysning.ErForhaandsgodkjent
                    }

                    "godkjent av veileder er false" {
                        val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                            .shouldBeInstanceOf<Either.Right<GrunnlagForGodkjenning>>()
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
                        personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns Person(
                        foedselsdato = listOf(Foedselsdato("2000-01-01", 2000)),
                        foedested = listOf(Foedested("NOR", "Oslo", "Oslo")),
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
                    val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                        .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.IkkeBosatt
                    resultat.value.head.regel.id shouldBe IkkeBosattINorgeIHenholdTilFolkeregisterloven
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
                        personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns Person(
                        foedselsdato = listOf(Foedselsdato("2000-01-01", 2000)),
                        foedested = listOf(Foedested("NOR", "Oslo", "Oslo")),
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
                    val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                        .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.IkkeBosatt
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.Dnummer
                    resultat.value.head.regel.id shouldBe IkkeBosattINorgeIHenholdTilFolkeregisterloven
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
                            personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns null
                    val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                        .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.PersonIkkeFunnet
                    resultat.value.head.regel.id shouldBe IkkeFunnet
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
                        personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns Person(
                        foedselsdato = emptyList(),
                        foedested = listOf(Foedested("ARG", "Argentina", "AR")),
                        bostedsadresse = emptyList(),
                        folkeregisterpersonstatus = emptyList(),
                        opphold = emptyList(),
                        innflyttingTilNorge = emptyList(),
                        utflyttingFraNorge = emptyList(),
                        statsborgerskap = listOf(Statsborgerskap("ARG", Metadata(emptyList())))
                    )
                    val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                        .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.UkjentFoedselsdato
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
                        personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns Person(
                        foedselsdato = listOf(Foedselsdato("2000-01-01", 2000)),
                        foedested = listOf(Foedested("ARG", "Argentina", "AR")),
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
                    val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                        .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.ErDoed
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
                        personInfoService.hentPersonInfo(requestScope, identitsnummer.verdi)
                    } returns Person(
                        foedselsdato = listOf(Foedselsdato("2000-01-01", 2000)),
                        foedested = listOf(Foedested("ARG", "Argentina", "AR")),
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
                    val resultat = requestValidator.validerStartAvPeriodeOenske(requestScope, identitsnummer)
                        .shouldBeInstanceOf<Either.Left<NonEmptyList<Problem>>>()
                    resultat.value.head.opplysninger shouldContain DomeneOpplysning.ErSavnet
                }

            }
        }
    }
})

