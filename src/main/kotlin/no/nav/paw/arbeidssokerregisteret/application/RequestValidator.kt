package no.nav.paw.arbeidssokerregisteret.application

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
    suspend fun validerStartAvPeriodeOenske(identitetsnummer: Identitetsnummer): EndeligResultat {
        val autentiseringsFakta = evalBrukerTilgang(identitetsnummer) +
            autorisasjonService.evalNavAnsattTilgang(identitetsnummer)
        val tilgangsResultat = tilgangsReglerIPrioritertRekkefolge.evaluer(autentiseringsFakta)
        if (tilgangsResultat is EndeligResultat) {
            return tilgangsResultat
        } else {
            val person = personInfoService.hentPersonInfo(identitetsnummer.verdi)
            val fakta = person?.let { genererPersonFakta(it) } ?: setOf(Fakta.PERSON_IKKE_FUNNET)
            return reglerForInngangIPrioritertRekkefolge.evaluer(fakta + autentiseringsFakta)
        }
    }
}

fun genererPersonFakta(person: Person): Set<Fakta> {
    require(person.foedsel.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
    require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
    require(person.opphold.size  <= 1) { "Personen har flere opphold enn forventet" }
    require(person.innflyttingTilNorge.size <= 1) { "Personen har flere innflyttinger enn forventet" }
    require(person.utflyttingFraNorge.size <= 1) { "Personen har flere utflyttinger enn forventet" }
    return evalAlder(person.foedsel.firstOrNull()) +
        evalAdresse(person.bostedsadresse.firstOrNull()) +
        evalForenkletFRegStatus(person.folkeregisterpersonstatus) +
        evalOppholdstillatelse(person.opphold.firstOrNull()) +
        evalFlytting(person.innflyttingTilNorge.firstOrNull(), person.utflyttingFraNorge.firstOrNull())
}




