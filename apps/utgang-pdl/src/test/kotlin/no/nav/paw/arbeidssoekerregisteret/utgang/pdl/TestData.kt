package no.nav.paw.arbeidssoekerregisteret.utgang.pdl

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Bostedsadresse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Foedsel
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Metadata
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Opphold
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Person
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Statsborgerskap
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.UtflyttingFraNorge

fun generatePdlMockResponse(ident: String, person: Person?, status: String): List<HentPersonBolkResult> =
    listOf(
        HentPersonBolkResult(
            ident,
            person,
            status
        )
    )

fun getStatsborgerskap(land: String = "NOR") = Statsborgerskap(land, Metadata(emptyList()))

fun getOppholdstillatelse(
    fra: String? = null,
    til: String? = null,
    type: Oppholdstillatelse = Oppholdstillatelse.PERMANENT
) = Opphold(fra, til, type, Metadata(emptyList()))

fun getFolkeregisterpersonstatus(status: String = "bosatt") = Folkeregisterpersonstatus(status, Metadata(emptyList()))
fun getListOfFolkeregisterpersonstatus(vararg status: String) = status.map { getFolkeregisterpersonstatus(it) }

fun getBostedsadresse(angittFlyttedato: String? = null) = Bostedsadresse(angittFlyttedato)

fun getPerson(
    foedsel: Foedsel? = null,
    statsborgerskap: Statsborgerskap? = null,
    opphold: Opphold? = null,
    folkeregisterpersonstatus: List<Folkeregisterpersonstatus>? = null,
    bostedsadresse: Bostedsadresse? = null,
    innflyttingTilNorge: List<no.nav.paw.pdl.graphql.generated.hentpersonbolk.InnflyttingTilNorge> = emptyList(),
    utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList()
): Person = Person(
    foedsel = listOf(foedsel ?: Foedsel("2000-01-01")),
    statsborgerskap = listOf(statsborgerskap ?: getStatsborgerskap()),
    opphold = listOf(opphold ?: getOppholdstillatelse()),
    folkeregisterpersonstatus = folkeregisterpersonstatus ?: listOf(getFolkeregisterpersonstatus()),
    bostedsadresse = listOf(bostedsadresse ?: getBostedsadresse()),
    innflyttingTilNorge = innflyttingTilNorge,
    utflyttingFraNorge = utflyttingFraNorge
)