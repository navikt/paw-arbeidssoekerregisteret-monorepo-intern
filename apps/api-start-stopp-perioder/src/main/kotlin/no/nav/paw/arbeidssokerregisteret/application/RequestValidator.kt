package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatMap
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.navAnsattTilgangFakta
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.tokenXPidFakta
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.*
import no.nav.paw.arbeidssokerregisteret.application.regler.TilgangsRegler
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.pdl.graphql.generated.hentperson.Person

class RequestValidator(
    private val autorisasjonService: AutorisasjonService,
    private val personInfoService: PersonInfoService,
    private val regler: Regler
) {

    context(RequestScope)
    @WithSpan
    fun validerTilgang(
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean = false
    ): Either<NonEmptyList<Problem>, GrunnlagForGodkjenning> {
        val autentiseringsFakta = tokenXPidFakta(identitetsnummer) +
                autorisasjonService.navAnsattTilgangFakta(identitetsnummer) +
                if (erForhaandsGodkjentAvVeileder) {
                    setOf(DomeneOpplysning.ErForhaandsgodkjent)
                } else {
                    emptySet()
                }
        return TilgangsRegler.evaluer(autentiseringsFakta)
    }

    context(RequestScope)
    @WithSpan
    suspend fun validerStartAvPeriodeOenske(
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean = false
    ): Either<NonEmptyList<Problem>, GrunnlagForGodkjenning> =
        validerTilgang(identitetsnummer, erForhaandsGodkjentAvVeileder)
            .flatMap { grunnlagForGodkjentAuth ->
                val person = personInfoService.hentPersonInfo(identitetsnummer.verdi)
                val opplysning = person?.let { genererPersonFakta(it) } ?: setOf(DomeneOpplysning.PersonIkkeFunnet)
                regler.evaluer(
                    opplysning + grunnlagForGodkjentAuth.opplysning
                )
            }

    fun genererPersonFakta(person: Person): Set<Opplysning> {
        require(person.foedsel.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
        require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
        require(person.opphold.size <= 1) { "Personen har flere opphold enn forventet" }

        return alderOpplysning(person.foedsel.firstOrNull()) +
                adreseOpplysning(person.bostedsadresse.firstOrNull()) +
                euEoesStatsborgerOpplysning(person.statsborgerskap) +
                gbrStatsborgerOpplysning(person.statsborgerskap) +
                norskStatsborgerOpplysning(person.statsborgerskap) +
                forenkletFregOpplysning(person.folkeregisterpersonstatus) +
                oppholdstillatelseOpplysning(person.opphold.firstOrNull()) +
                utflyttingOpplysning(person.innflyttingTilNorge, person.utflyttingFraNorge)
    }
}




