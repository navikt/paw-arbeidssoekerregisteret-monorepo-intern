package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse

fun adreseOpplysning(adresse: Bostedsadresse?): Opplysning {
    return when {
        adresse == null -> IngenAdresseFunnet
        adresse.vegadresse?.kommunenummer != null -> HarNorskAdresse
        adresse.matrikkeladresse?.kommunenummer != null -> HarNorskAdresse
        adresse.ukjentBosted?.bostedskommune != null -> HarNorskAdresse
        adresse.utenlandskAdresse != null -> HarUtenlandskAdresse
        else -> IngenAdresseFunnet
    }
}
