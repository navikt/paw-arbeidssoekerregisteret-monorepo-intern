package no.nav.paw.arbeidssokerregisteret.arbeidssoker

import no.nav.paw.arbeidssokerregisteret.arbeidssoker.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.arbeidssoker.domain.PeriodeDto
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

class ArbeidssokerRepository(private val db: Database) {
    private fun resultRowTilPeriodeDto(resultRow: ResultRow): PeriodeDto =
        PeriodeDto(
            id = resultRow[ArbeidssokerPeriodeTable.id],
            fraOgMedDato = resultRow[ArbeidssokerPeriodeTable.fraOgMedDato],
            tilOgMedDato = resultRow[ArbeidssokerPeriodeTable.tilOgMedDato],
        )

    fun opprett(foedselsnummer: Foedselsnummer, fraOgMedDato: LocalDateTime) = transaction(db) {
        ArbeidssokerPeriodeTable.insert {
            it[this.foedselsnummer] = foedselsnummer.verdi
            it[this.fraOgMedDato] = fraOgMedDato
            it[this.tilOgMedDato] = null
            it[this.opprettetAv] = foedselsnummer.verdi
        }
    }

    fun hent(foedselsnummer: Foedselsnummer) = transaction(db) {
        ArbeidssokerPeriodeTable.select { ArbeidssokerPeriodeTable.foedselsnummer.eq(foedselsnummer.verdi) }
            .map(::resultRowTilPeriodeDto)
    }

    fun avslutt(id: UUID, tilOgMedDato: LocalDateTime) = transaction(db) {
        ArbeidssokerPeriodeTable.update({ ArbeidssokerPeriodeTable.id.eq(id) }) {
            it[this.tilOgMedDato] = tilOgMedDato
        }
    }

    fun hentSistePeriode(foedselsnummer: Foedselsnummer): PeriodeDto? =
        transaction(db) {
            ArbeidssokerPeriodeTable.select { ArbeidssokerPeriodeTable.foedselsnummer.eq(foedselsnummer.verdi) }
                .orderBy(ArbeidssokerPeriodeTable.fraOgMedDato, SortOrder.DESC)
                .limit(1)
                .map(::resultRowTilPeriodeDto)
                .firstOrNull()
        }
}
