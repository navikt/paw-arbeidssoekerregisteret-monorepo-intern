package no.nav.paw.arbeidssoekerregisteret.utgang.pdl

import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Bostedsadresse
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Foedsel
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Folkeregistermetadata
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Metadata
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Opphold
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Person
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Statsborgerskap
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.UtflyttingFraNorge

fun generatePdlPerson(
    foedsel: Foedsel? = null,
    statsborgerskap: Statsborgerskap? = null,
    opphold: Opphold? = null,
    folkeregisterpersonstatus: Folkeregisterpersonstatus? = null,
    bostedsadresse: Bostedsadresse? = null,
): Person =
    Person(
        foedsel = listOf(
            foedsel ?: Foedsel(
                foedselsdato = "1990-01-01",
                foedselsaar = 1990
            )
        ),
        statsborgerskap = listOf(
            statsborgerskap ?: Statsborgerskap(
                land = "NOR",
                metadata = Metadata(
                    endringer = emptyList()
                )
            )
        ),
        opphold = listOf(
            opphold ?: Opphold(
                oppholdFra = "2020-01-01",
                oppholdTil = "2021-01-01",
                type = Oppholdstillatelse.PERMANENT,
                metadata = Metadata(
                    endringer = emptyList()
                )
            )
        ),
        folkeregisterpersonstatus = listOf(
            folkeregisterpersonstatus ?: Folkeregisterpersonstatus(
                forenkletStatus = "bosattEtterFolkeregisterloven",
                metadata = Metadata(
                    endringer = emptyList()
                )
            )
        ),
        bostedsadresse = listOf(
            bostedsadresse ?: Bostedsadresse(
                angittFlyttedato = null,
            )
        ),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList(),
    )


fun generatePdlHentPersonMockResponse(ident: String, person: Person?, status: String): List<HentPersonBolkResult> =
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

fun getBostedsadresse(angittFlyttedato: String? = null) = Bostedsadresse(angittFlyttedato)

fun getUtflyttingFraNorge(utflyttingsdato: String, metadata: Folkeregistermetadata? = null) = UtflyttingFraNorge(utflyttingsdato, metadata)

fun getInnflyttingTilNorge(metadata: Folkeregistermetadata? = null) = no.nav.paw.pdl.graphql.generated.hentpersonbolk.InnflyttingTilNorge(metadata)

fun getPerson(
    foedsel: Foedsel? = null,
    statsborgerskap: Statsborgerskap? = null,
    opphold: Opphold? = null,
    folkeregisterpersonstatus: Folkeregisterpersonstatus? = null,
    bostedsadresse: Bostedsadresse? = null,
    innflyttingTilNorge: List<no.nav.paw.pdl.graphql.generated.hentpersonbolk.InnflyttingTilNorge> = emptyList(),
    utflyttingFraNorge: List<UtflyttingFraNorge> = emptyList()
): Person = Person(
    foedsel = listOf(foedsel ?: Foedsel("2000-01-01")),
    statsborgerskap = listOf(statsborgerskap ?: getStatsborgerskap()),
    opphold = listOf(opphold ?: getOppholdstillatelse()),
    folkeregisterpersonstatus = listOf(folkeregisterpersonstatus ?: getFolkeregisterpersonstatus()),
    bostedsadresse = listOf(bostedsadresse ?: getBostedsadresse()),
    innflyttingTilNorge = innflyttingTilNorge,
    utflyttingFraNorge = utflyttingFraNorge
)