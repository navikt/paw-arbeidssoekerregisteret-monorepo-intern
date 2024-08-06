package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse

fun adreseOpplysning(adresse: Bostedsadresse?): Set<Opplysning> {
    return when {
        adresse == null -> setOf(IngenAdresseFunnet)
        adresse.vegadresse?.kommunenummer != null -> setOf(HarNorskAdresse, HarRegistrertAdresseIEuEoes)
        adresse.matrikkeladresse?.kommunenummer != null -> setOf(HarNorskAdresse, HarRegistrertAdresseIEuEoes)
        adresse.ukjentBosted?.bostedskommune != null -> setOf(HarNorskAdresse, HarRegistrertAdresseIEuEoes)
        adresse.utenlandskAdresse != null -> if (adresse.utenlandskAdresse!!.landkode in eea) {
            setOf(HarUtenlandskAdresse, HarRegistrertAdresseIEuEoes)
        } else {
            setOf(HarUtenlandskAdresse)
        }
        else -> setOf(IngenAdresseFunnet)
    }
}
