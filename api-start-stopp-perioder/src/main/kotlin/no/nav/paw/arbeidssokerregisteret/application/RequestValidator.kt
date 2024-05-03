package no.nav.paw.arbeidssokerregisteret.application

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.fakta.*
import no.nav.paw.arbeidssokerregisteret.application.regler.reglerForInngangIPrioritertRekkefolge
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
    fun validerTilgang(identitetsnummer: Identitetsnummer, erForhaandsGodkjentAvVeileder: Boolean = false): TilgangskontrollResultat {
        val autentiseringsFakta = tokenXPidFakta(identitetsnummer) +
            autorisasjonService.navAnsattTilgangFakta(identitetsnummer) +
            if (erForhaandsGodkjentAvVeileder) {
                setOf(Opplysning.FORHAANDSGODKJENT_AV_ANSATT)
            } else {
                emptySet()
            }
        return tilgangsReglerIPrioritertRekkefolge.evaluer(autentiseringsFakta)
    }

    context(RequestScope)
    @WithSpan
    suspend fun validerStartAvPeriodeOenske(identitetsnummer: Identitetsnummer, erForhaandsGodkjentAvVeileder: Boolean = false): EndeligResultat {
        val tilgangsResultat = validerTilgang(identitetsnummer, erForhaandsGodkjentAvVeileder)
        if (tilgangsResultat is EndeligResultat) {
            return tilgangsResultat
        } else {
            val person = personInfoService.hentPersonInfo(identitetsnummer.verdi)
            val opplysning = person?.let { genererPersonFakta(it) } ?: setOf(Opplysning.PERSON_IKKE_FUNNET)
            return reglerForInngangIPrioritertRekkefolge.evaluer(opplysning + tilgangsResultat.opplysning)
        }
    }
}

fun genererPersonFakta(person: Person): Set<Opplysning> {
    require(person.foedsel.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
    require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
    require(person.opphold.size <= 1) { "Personen har flere opphold enn forventet" }
    require(person.innflyttingTilNorge.size <= 1) { "Personen har flere innflyttinger enn forventet" }
    require(person.utflyttingFraNorge.size <= 1) { "Personen har flere utflyttinger enn forventet" }

    return alderFakta(person.foedsel.firstOrNull()) +
        adresseFakta(person.bostedsadresse.firstOrNull()) +
        forenkletFregFakta(person.folkeregisterpersonstatus) +
        oppholdstillatelseFakta(person.opphold.firstOrNull()) +
        utflyttingFakta(person.innflyttingTilNorge.firstOrNull(), person.utflyttingFraNorge.firstOrNull())

}




