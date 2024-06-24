package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.flatMap
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.navAnsattTilgangFakta
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.tokenXPidFakta
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.*
import no.nav.paw.arbeidssokerregisteret.application.regler.tilgangsReglerIPrioritertRekkefolge
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.pdl.graphql.generated.hentperson.Person

class RequestValidator(
    private val autorisasjonService: AutorisasjonService,
    private val personInfoService: PersonInfoService,
) {

    context(RequestScope)
    @WithSpan
    fun validerTilgang(
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean = false
    ): Either<Problem, OK> {
        val autentiseringsFakta = tokenXPidFakta(identitetsnummer) +
                autorisasjonService.navAnsattTilgangFakta(identitetsnummer) +
                if (erForhaandsGodkjentAvVeileder) {
                    setOf(DomeneOpplysning.ErForhaandsgodkjent)
                } else {
                    emptySet()
                }
        return tilgangsReglerIPrioritertRekkefolge.evaluer(autentiseringsFakta)
    }

    context(RequestScope)
    @WithSpan
    suspend fun validerStartAvPeriodeOenske(
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean = false
    ): Either<Problem, OK> =
        validerTilgang(identitetsnummer, erForhaandsGodkjentAvVeileder)
            .flatMap { tilgangsResultat ->
                val person = personInfoService.hentPersonInfo(identitetsnummer.verdi)
                val opplysning = person?.let { genererPersonFakta(it) } ?: setOf(DomeneOpplysning.PersonIkkeFunnet)
                reglerForInngangIPrioritertRekkefolge.evaluer(opplysning + tilgangsResultat.opplysning)
            }


    fun genererPersonFakta(person: Person): Set<Opplysning> {
        require(person.foedsel.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
        require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
        require(person.opphold.size <= 1) { "Personen har flere opphold enn forventet" }

        return alderOpplysning(person.foedsel.firstOrNull()) +
                adreseOpplysning(person.bostedsadresse.firstOrNull()) +
                euEoesStatsborgerOpplysning(person.statsborgerskap) +
                gbrStatsborgerOpplysning(person.statsborgerskap) +
                forenkletFregOpplysning(person.folkeregisterpersonstatus) +
                oppholdstillatelseOpplysning(person.opphold.firstOrNull()) +
                utflyttingOpplysning(person.innflyttingTilNorge, person.utflyttingFraNorge)
    }
}




