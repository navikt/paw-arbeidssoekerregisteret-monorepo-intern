package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse

fun evalAdresse(adresse: Bostedsadresse?): Attributter {
    return when {
        adresse == null -> Attributter.INGEN_ADRESSE_FUNNET
        adresse.vegadresse?.kommunenummer != null -> Attributter.HAR_NORSK_ADRESSE
        adresse.matrikkeladresse?.kommunenummer != null -> Attributter.HAR_NORSK_ADRESSE
        adresse.ukjentBosted?.bostedskommune != null -> Attributter.HAR_NORSK_ADRESSE
        adresse.utenlandskAdresse != null -> Attributter.HAR_UTENLANDSK_ADRESSE
        else -> Attributter.INGEN_ADRESSE_FUNNET
    }
}
