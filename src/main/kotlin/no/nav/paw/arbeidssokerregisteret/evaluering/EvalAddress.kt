package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse

fun evalAdresse(adresse: Bostedsadresse?): Attributt {
    return when {
        adresse == null -> Attributt.INGEN_ADRESSE_FUNNET
        adresse.vegadresse?.kommunenummer != null -> Attributt.HAR_NORSK_ADRESSE
        adresse.matrikkeladresse?.kommunenummer != null -> Attributt.HAR_NORSK_ADRESSE
        adresse.ukjentBosted?.bostedskommune != null -> Attributt.HAR_NORSK_ADRESSE
        adresse.utenlandskAdresse != null -> Attributt.HAR_UTENLANDSK_ADRESSE
        else -> Attributt.INGEN_ADRESSE_FUNNET
    }
}
