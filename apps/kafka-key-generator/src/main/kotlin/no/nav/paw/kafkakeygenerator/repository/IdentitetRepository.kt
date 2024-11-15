package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class IdentitetRepository(
    private val database: Database
) {
    fun find(identitetsnummer: Identitetsnummer): Pair<Identitetsnummer, ArbeidssoekerId>? = transaction(database) {
        IdentitetTabell.selectAll()
            .where { IdentitetTabell.identitetsnummer eq identitetsnummer.value }
            .singleOrNull()
            ?.let {
                Identitetsnummer(it[IdentitetTabell.identitetsnummer]) to ArbeidssoekerId(it[IdentitetTabell.kafkaKey])
            }
    }

    fun find(arbeidssoekerId: ArbeidssoekerId): Pair<Identitetsnummer, ArbeidssoekerId>? = transaction(database) {
        IdentitetTabell.selectAll()
            .where { IdentitetTabell.kafkaKey eq arbeidssoekerId.value }
            .singleOrNull()
            ?.let {
                Identitetsnummer(it[IdentitetTabell.identitetsnummer]) to ArbeidssoekerId(it[IdentitetTabell.kafkaKey])
            }
    }

    fun insert(
        ident: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Int = transaction(database) {
        IdentitetTabell.insert {
            it[identitetsnummer] = ident.value
            it[kafkaKey] = arbeidssoekerId.value
        }.insertedCount
    }

    fun update(
        identitetsnummer: Identitetsnummer,
        tilArbeidssoekerId: ArbeidssoekerId
    ): Int = transaction(database) {
        IdentitetTabell.update(where = {
            (IdentitetTabell.identitetsnummer eq identitetsnummer.value)
        }) {
            it[kafkaKey] = tilArbeidssoekerId.value
        }
    }
}