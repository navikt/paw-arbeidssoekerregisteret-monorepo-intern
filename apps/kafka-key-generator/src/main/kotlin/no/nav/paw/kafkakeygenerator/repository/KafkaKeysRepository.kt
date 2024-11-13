package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class KafkaKeysRepository(
    private val database: Database
) {

    fun update(
        identitetsnummer: Identitetsnummer,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ): Int = transaction(database) {
        IdentitetTabell.update(where = {
            (IdentitetTabell.identitetsnummer eq identitetsnummer.value) and
                    (IdentitetTabell.kafkaKey eq fraArbeidssoekerId.value)
        }) {
            it[kafkaKey] = tilArbeidssoekerId.value
        }
    }
}