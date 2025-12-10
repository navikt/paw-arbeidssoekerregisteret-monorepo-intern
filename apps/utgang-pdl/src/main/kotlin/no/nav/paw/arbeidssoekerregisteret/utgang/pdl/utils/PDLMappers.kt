package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils

import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Bostedsadresse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Endring
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Foedested
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Foedselsdato
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Folkeregistermetadata
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.InnflyttingTilNorge
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Matrikkeladresse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Metadata
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Opphold
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Person
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Statsborgerskap
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.UtenlandskAdresse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.UtflyttingFraNorge
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Vegadresse

fun Person.toPerson(): no.nav.paw.pdl.graphql.generated.hentperson.Person =
    no.nav.paw.pdl.graphql.generated.hentperson.Person(
        foedselsdato = this.foedselsdato.toFoedselsdato(),
        statsborgerskap = this.statsborgerskap.toStatsborgerskap(),
        opphold = this.opphold.toOpphold(),
        folkeregisterpersonstatus = this.folkeregisterpersonstatus.toFolkeregisterpersonstatus(),
        bostedsadresse = this.bostedsadresse.toBostedsadresseBolk(),
        innflyttingTilNorge = this.innflyttingTilNorge.toInnflyttingTilNorge(),
        utflyttingFraNorge = this.utflyttingFraNorge.toUtflyttingFraNorge(),
    )

fun List<UtflyttingFraNorge>.toUtflyttingFraNorge(): List<no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge(
        utflyttingsdato = it.utflyttingsdato,
        folkeregistermetadata = it.folkeregistermetadata?.toFolkeregistermetadata(),
    )
}

fun Folkeregistermetadata.toFolkeregistermetadata(): no.nav.paw.pdl.graphql.generated.hentperson.Folkeregistermetadata =
    no.nav.paw.pdl.graphql.generated.hentperson.Folkeregistermetadata(
        gyldighetstidspunkt = this.gyldighetstidspunkt,
        ajourholdstidspunkt = this.ajourholdstidspunkt,
    )

fun List<InnflyttingTilNorge>.toInnflyttingTilNorge(): List<no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge(
        folkeregistermetadata = it.folkeregistermetadata?.toFolkeregistermetadata()
    )
}

fun List<Folkeregisterpersonstatus>.toFolkeregisterpersonstatus(): List<no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus(
        forenkletStatus = it.forenkletStatus,
        metadata = it.metadata.toMetadata(),
    )
}

fun List<Statsborgerskap>.toStatsborgerskap(): List<no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap(
        land = it.land,
        metadata = it.metadata.toMetadata(),
    )
}

fun List<Opphold>.toOpphold(): List<no.nav.paw.pdl.graphql.generated.hentperson.Opphold> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.Opphold(
        oppholdFra = it.oppholdFra,
        oppholdTil = it.oppholdTil,
        type = it.type,
        metadata = it.metadata.toMetadata(),
    )
}

fun Metadata.toMetadata(): no.nav.paw.pdl.graphql.generated.hentperson.Metadata = no.nav.paw.pdl.graphql.generated.hentperson.Metadata(
    endringer = this.endringer.map { it.toEndring() },
)

fun Endring.toEndring(): no.nav.paw.pdl.graphql.generated.hentperson.Endring =
    no.nav.paw.pdl.graphql.generated.hentperson.Endring(
        type = this.type,
        registrert = this.registrert,
        kilde = this.kilde,
    )

fun List<Foedselsdato>.toFoedselsdato(): List<no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato(
        foedselsdato = it.foedselsdato,
        foedselsaar = it.foedselsaar,
    )
}

fun List<Bostedsadresse>.toBostedsadresseBolk(): List<no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse> = this.map {
    no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse(
        angittFlyttedato = it.angittFlyttedato,
        gyldigFraOgMed = it.gyldigFraOgMed,
        gyldigTilOgMed = it.gyldigTilOgMed,
        vegadresse = it.vegadresse?.toVegadresse(),
        matrikkeladresse = it.matrikkeladresse?.toMatikkeladresse(),
        utenlandskAdresse = it.utenlandskAdresse?.toUtenlandskAdresse(),
    )
}

fun Vegadresse.toVegadresse(): no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse =
    no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse(
        kommunenummer = this.kommunenummer,
    )

fun Matrikkeladresse.toMatikkeladresse(): no.nav.paw.pdl.graphql.generated.hentperson.Matrikkeladresse =
    no.nav.paw.pdl.graphql.generated.hentperson.Matrikkeladresse(
        kommunenummer = this.kommunenummer,
    )

fun UtenlandskAdresse.toUtenlandskAdresse(): no.nav.paw.pdl.graphql.generated.hentperson.UtenlandskAdresse =
    no.nav.paw.pdl.graphql.generated.hentperson.UtenlandskAdresse(
        landkode = this.landkode,
    )