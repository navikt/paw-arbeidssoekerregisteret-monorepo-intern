package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse

fun adresseFakta(adresse: Bostedsadresse?): Opplysning {
    return when {
        adresse == null -> Opplysning.INGEN_ADRESSE_FUNNET
        adresse.vegadresse?.kommunenummer != null -> Opplysning.HAR_NORSK_ADRESSE
        adresse.matrikkeladresse?.kommunenummer != null -> Opplysning.HAR_NORSK_ADRESSE
        adresse.ukjentBosted?.bostedskommune != null -> Opplysning.HAR_NORSK_ADRESSE
        adresse.utenlandskAdresse != null -> Opplysning.HAR_UTENLANDSK_ADRESSE
        else -> Opplysning.INGEN_ADRESSE_FUNNET
    }
}
