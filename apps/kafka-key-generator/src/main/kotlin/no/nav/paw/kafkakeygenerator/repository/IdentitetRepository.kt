package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class IdentitetRepository {
    fun find(identitetsnummer: Identitetsnummer): Pair<Identitetsnummer, ArbeidssoekerId>? = transaction {
        IdentitetTabell.selectAll()
            .where { IdentitetTabell.identitetsnummer eq identitetsnummer.value }
            .singleOrNull()
            ?.let {
                Identitetsnummer(it[IdentitetTabell.identitetsnummer]) to ArbeidssoekerId(it[IdentitetTabell.kafkaKey])
            }
    }

    fun insert(
        ident: Identitetsnummer,
        arbeidssoekerId: ArbeidssoekerId
    ): Int = transaction {
        IdentitetTabell.insert {
            it[identitetsnummer] = ident.value
            it[kafkaKey] = arbeidssoekerId.value
        }.insertedCount
    }

    fun update(
        identitetsnummer: Identitetsnummer,
        tilArbeidssoekerId: ArbeidssoekerId
    ): Int = transaction {
        IdentitetTabell.update(where = {
            (IdentitetTabell.identitetsnummer eq identitetsnummer.value)
        }) {
            it[kafkaKey] = tilArbeidssoekerId.value
        }
    }
}