package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.adreseOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.alderOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.euEoesStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.forenkletFregOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.gbrStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.norskStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.oppholdstillatelseOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.utflyttingOpplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Person

val negativeOpplysninger = setOf(
    Opplysning.IKKE_BOSATT,
    Opplysning.SAVNET,
    Opplysning.DOED,
    Opplysning.OPPHOERT_IDENTITET,
)

fun genererPersonFakta(person: Person): Set<no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning> {
    require(person.foedselsdato.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
    require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
    require(person.opphold.size <= 1) { "Personen har flere opphold enn forventet" }

    return alderOpplysning(person.foedselsdato.firstOrNull()) +
            adreseOpplysning(person.bostedsadresse.firstOrNull()) +
            euEoesStatsborgerOpplysning(person.statsborgerskap) +
            gbrStatsborgerOpplysning(person.statsborgerskap) +
            norskStatsborgerOpplysning(person.statsborgerskap) +
            forenkletFregOpplysning(person.folkeregisterpersonstatus) +
            oppholdstillatelseOpplysning(person.opphold.firstOrNull()) +
            utflyttingOpplysning(person.innflyttingTilNorge, person.utflyttingFraNorge)
}