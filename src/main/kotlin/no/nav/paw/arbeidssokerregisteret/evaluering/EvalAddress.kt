package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse

fun evalAdresse(adresse: Bostedsadresse?): Evaluation {
    return when {
        adresse == null -> Evaluation.INGEN_ADRESSE_FUNNET
        adresse.vegadresse?.kommunenummer != null -> Evaluation.HAR_NORSK_ADRESSE
        adresse.matrikkeladresse?.kommunenummer != null -> Evaluation.HAR_NORSK_ADRESSE
        adresse.ukjentBosted?.bostedskommune != null -> Evaluation.HAR_NORSK_ADRESSE
        adresse.utenlandskAdresse != null -> Evaluation.HAR_UTENLANDSK_ADRESSE
        else -> Evaluation.INGEN_ADRESSE_FUNNET
    }
}
