package no.nav.paw.kafkakeygenerator.repository

import no.nav.paw.kafkakeygenerator.database.KafkaKeysIdentitetTable
import no.nav.paw.kafkakeygenerator.database.KafkaKeysTable
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.attempt
import no.nav.paw.kafkakeygenerator.vo.flatMap
import no.nav.paw.kafkakeygenerator.vo.left
import no.nav.paw.kafkakeygenerator.vo.mapToFailure
import no.nav.paw.kafkakeygenerator.vo.right
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaKeysRepository {

    fun find(arbeidssoekerId: ArbeidssoekerId): ArbeidssoekerId? =
        transaction {
            KafkaKeysTable.selectAll()
                .where { KafkaKeysTable.id eq arbeidssoekerId.value }
                .singleOrNull()?.let { ArbeidssoekerId(it[KafkaKeysTable.id]) }
        }

    fun hentSisteArbeidssoekerId(): Either<Failure, ArbeidssoekerId> =
        attempt {
            transaction {
                KafkaKeysIdentitetTable
                    .selectAll()
                    .orderBy(KafkaKeysIdentitetTable.kafkaKey, SortOrder.DESC)
                    .firstOrNull()?.get(KafkaKeysIdentitetTable.kafkaKey)
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }
            .map { id -> id?.let(::ArbeidssoekerId) }
            .flatMap { id -> id?.let(::right) ?: left(Failure("database", FailureCode.DB_NOT_FOUND)) }

    fun hent(currentPos: Long, maxSize: Int): Either<Failure, Map<Identitetsnummer, ArbeidssoekerId>> {
        return attempt {
            transaction {
                KafkaKeysIdentitetTable
                    .selectAll()
                    .where { KafkaKeysIdentitetTable.kafkaKey greaterEq currentPos and (KafkaKeysIdentitetTable.kafkaKey less (currentPos + maxSize)) }
                    .orderBy(column = KafkaKeysIdentitetTable.kafkaKey, order = SortOrder.ASC)
                    .limit(maxSize)
                    .associate {
                        Identitetsnummer(it[KafkaKeysIdentitetTable.identitetsnummer]) to ArbeidssoekerId(it[KafkaKeysIdentitetTable.kafkaKey])
                    }
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }
    }

    fun hent(identiteter: List<String>): Either<Failure, Map<String, ArbeidssoekerId>> =
        attempt {
            transaction {
                KafkaKeysIdentitetTable
                    .selectAll()
                    .where { KafkaKeysIdentitetTable.identitetsnummer inList identiteter }
                    .associate {
                        it[KafkaKeysIdentitetTable.identitetsnummer] to it[KafkaKeysIdentitetTable.kafkaKey]
                    }
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }.map { resultMap -> resultMap.mapValues { ArbeidssoekerId(it.value) } }

    fun hent(identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> =
        attempt {
            transaction {
                KafkaKeysIdentitetTable
                    .selectAll()
                    .where { KafkaKeysIdentitetTable.identitetsnummer eq identitet.value }
                    .firstOrNull()?.get(KafkaKeysIdentitetTable.kafkaKey)
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }
            .map { id -> id?.let(::ArbeidssoekerId) }
            .flatMap { id -> id?.let(::right) ?: left(Failure("database", FailureCode.DB_NOT_FOUND)) }

    fun hent(arbeidssoekerId: ArbeidssoekerId): Either<Failure, List<Identitetsnummer>> =
        attempt {
            transaction {
                KafkaKeysIdentitetTable
                    .selectAll()
                    .where { KafkaKeysIdentitetTable.kafkaKey eq arbeidssoekerId.value }
                    .map { Identitetsnummer(it[KafkaKeysIdentitetTable.identitetsnummer]) }
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }


    fun lagre(identitet: Identitetsnummer, arbeidssoekerId: ArbeidssoekerId): Either<Failure, Unit> =
        attempt {
            transaction {
                KafkaKeysIdentitetTable.insertIgnore {
                    it[identitetsnummer] = identitet.value
                    it[kafkaKey] = arbeidssoekerId.value
                }.insertedCount
            }
        }
            .mapToFailure { exception ->
                Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
            }
            .flatMap { if (it == 1) right(Unit) else left(Failure("database", FailureCode.CONFLICT)) }


    fun opprett(identitet: Identitetsnummer): Either<Failure, ArbeidssoekerId> =
        attempt {
            transaction {
                val key = KafkaKeysTable.insert { }[KafkaKeysTable.id]
                val opprettet = KafkaKeysIdentitetTable.insertIgnore {
                    it[identitetsnummer] = identitet.value
                    it[kafkaKey] = key
                }.insertedCount == 1
                if (opprettet) key else null
            }
        }.mapToFailure { exception ->
            Failure("database", FailureCode.INTERNAL_TECHINCAL_ERROR, exception)
        }
            .map { id -> id?.let(::ArbeidssoekerId) }
            .flatMap { id -> id?.let(::right) ?: left(Failure("database", FailureCode.CONFLICT)) }
}