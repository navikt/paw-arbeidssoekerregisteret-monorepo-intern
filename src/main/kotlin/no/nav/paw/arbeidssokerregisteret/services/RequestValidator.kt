package no.nav.paw.arbeidssokerregisteret.services

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.evaluering.*
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.genererTilgangsResultat
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.sjekkOmRettTilRegistrering
import no.nav.paw.pdl.graphql.generated.hentperson.Person

class RequestValidator(
    private val autorisasjonService: AutorisasjonService,
    private val personInfoService: PersonInfoService,
) {
    context(RequestScope)
    suspend fun validerStartAvPeriodeOenske(identitetsnummer: Identitetsnummer): Resultat {
        val tilgagsSjekkResultat = genererTilgangsResultat(autorisasjonService, identitetsnummer)
        return if (tilgagsSjekkResultat is IkkeTilgang) {
            tilgagsSjekkResultat
        } else {
            val person = personInfoService.hentPersonInfo(identitetsnummer.verdi)
            val evalueringer = (person?.let { evaluerStartAvPeriodeOenske(identitetsnummer, person) }
                ?: setOf(Attributt.PERSON_IKKE_FUNNET)) + tilgagsSjekkResultat.attributt
            sjekkOmRettTilRegistrering(evalueringer)
        }
    }
}

context(RequestScope)
fun evaluerStartAvPeriodeOenske(identitetsnummer: Identitetsnummer, person: Person): Set<Attributt> {
    require(person.foedsel.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
    require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
    require(person.opphold.size  <= 1) { "Personen har flere opphold enn forventet" }
    require(person.innflyttingTilNorge.size <= 1) { "Personen har flere innflyttinger enn forventet" }
    require(person.utflyttingFraNorge.size <= 1) { "Personen har flere utflyttinger enn forventet" }
    return evalAlder(person.foedsel.firstOrNull()) +
        evalAdresse(person.bostedsadresse.firstOrNull()) +
        evalForenkletFRegStatus(person.folkeregisterpersonstatus) +
        evalBrukerTilgang(identitetsnummer) +
        evalOppholdstillatelse(person.opphold.firstOrNull()) +
        evalFlytting(person.innflyttingTilNorge.firstOrNull(), person.utflyttingFraNorge.firstOrNull())
}




