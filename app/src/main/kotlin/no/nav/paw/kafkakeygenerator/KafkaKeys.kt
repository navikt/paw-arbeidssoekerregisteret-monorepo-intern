package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.database.IdentitetTabell
import no.nav.paw.kafkakeygenerator.database.KafkaKeysTabell
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaKeys(private val database: Database) {

    fun hent(identiteter: List<String>): Either<Failure, Map<String, Long>> =
        attempt {
            transaction(database) {
                IdentitetTabell.select {
                    IdentitetTabell.identitetsnummer inList identiteter
                }.associate {
                    it[IdentitetTabell.identitetsnummer] to it[IdentitetTabell.kafkaKey]
                }
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }

    fun hent(identitet: Identitetsnummer): Either<Failure, Long> =
        attempt {
            transaction(database) {
                IdentitetTabell.select {
                    IdentitetTabell.identitetsnummer eq identitet.value
                }.firstOrNull()?.get(IdentitetTabell.kafkaKey)
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }.flatMap { id -> id?.let(::right) ?: left(Failure("database", FailureCode.DB_NOT_FOUND)) }

    fun lagre(identitet: Identitetsnummer, nøkkel: Long): Either<Failure, Unit> =
        attempt {
            transaction(database) {
                IdentitetTabell.insertIgnore {
                    it[identitetsnummer] = identitet.value
                    it[kafkaKey] = nøkkel
                }.insertedCount
            }
        }
            .mapToFailure { exception ->
                Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
            }
            .flatMap { if (it == 1) right(Unit) else left(Failure("database", FailureCode.CONFLICT)) }


    fun opprett(identitet: Identitetsnummer): Either<Failure, Long> =
        attempt {
            transaction(database) {
                val nøkkel = KafkaKeysTabell.insert { }[KafkaKeysTabell.id]
                val opprettet = IdentitetTabell.insertIgnore {
                    it[identitetsnummer] = identitet.value
                    it[kafkaKey] = nøkkel
                }.insertedCount == 1
                if (opprettet) nøkkel else null
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }.flatMap { id -> id?.let(::right) ?: left(Failure("database", FailureCode.CONFLICT)) }
}