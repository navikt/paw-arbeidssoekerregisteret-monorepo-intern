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