package no.nav.paw.arbeidssoeker.synk.repository

import no.nav.paw.arbeidssoeker.synk.model.ArbeidssoekerDatabaseRow
import no.nav.paw.arbeidssoeker.synk.model.ArbeidssoekereSynkTable
import no.nav.paw.arbeidssoeker.synk.model.asArbeidssoekereRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class ArbeidssoekerSynkRepository {

    fun find(): List<ArbeidssoekerDatabaseRow> {
        return transaction {
            ArbeidssoekereSynkTable.selectAll()
                .orderBy(ArbeidssoekereSynkTable.inserted to SortOrder.ASC)
                .map { it.asArbeidssoekereRow() }
        }
    }

    fun find(identitetsnummer: String): List<ArbeidssoekerDatabaseRow> {
        return transaction {
            ArbeidssoekereSynkTable.selectAll()
                .where { ArbeidssoekereSynkTable.identitetsnummer eq identitetsnummer }
                .orderBy(ArbeidssoekereSynkTable.inserted to SortOrder.ASC)
                .map { it.asArbeidssoekereRow() }
        }
    }

    fun find(
        version: String,
        identitetsnummer: String
    ): ArbeidssoekerDatabaseRow? {
        return transaction {
            ArbeidssoekereSynkTable.selectAll()
                .where {
                    ArbeidssoekereSynkTable.version eq version
                    ArbeidssoekereSynkTable.identitetsnummer eq identitetsnummer
                }
                .orderBy(ArbeidssoekereSynkTable.inserted to SortOrder.ASC)
                .map { it.asArbeidssoekereRow() }
                .firstOrNull()
        }
    }

    fun insert(
        version: String,
        identitetsnummer: String,
        status: Int
    ) {
        transaction {
            ArbeidssoekereSynkTable.insert {
                it[ArbeidssoekereSynkTable.version] = version
                it[ArbeidssoekereSynkTable.identitetsnummer] = identitetsnummer
                it[ArbeidssoekereSynkTable.status] = status
                it[inserted] = Instant.now()
            }
        }
    }

    fun update(
        version: String,
        identitetsnummer: String,
        status: Int
    ) {
        transaction {
            ArbeidssoekereSynkTable.update({
                ArbeidssoekereSynkTable.version eq version
                ArbeidssoekereSynkTable.identitetsnummer eq identitetsnummer
            }) {
                it[ArbeidssoekereSynkTable.status] = status
                it[updated] = Instant.now()
            }
        }
    }
}