package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.tilIdentRader
import no.nav.person.pdl.aktor.v2.Aktor
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant

fun TransactionContext.slettPerson(recordKey: String): Boolean =
    PersonTable.deleteWhere { (PersonTable.versjon eq consumerVersion) and (PersonTable.recordKey eq recordKey) } > 0

fun TransactionContext.hentIkkeProsessertePersoner(
    maksAntall: Int,
    sistEndretFoer: Instant
): List<Person> {
    return PersonTable.selectAll()
        .where {
            (PersonTable.versjon eq consumerVersion) and
                    (PersonTable.sistEndret less sistEndretFoer) and
                    (PersonTable.mergeProsessert eq false)
        }.limit(maksAntall)
        .map {
            Person(
                personId = it[PersonTable.id],
                recordKey = it[PersonTable.recordKey],
                sistEndret = it[PersonTable.sistEndret],
                traceparant = it[PersonTable.traceparent],
                tidspunktFraKilde = it[PersonTable.tidspunktFraKilde]
            )
        }
}

fun TransactionContext.hentPerson(recordKey: String): Person? {
    return PersonTable.selectAll()
        .where {
            (PersonTable.versjon eq consumerVersion) and (PersonTable.recordKey eq recordKey)
        }.map {
            Person(
                personId = it[PersonTable.id],
                recordKey = it[PersonTable.recordKey],
                sistEndret = it[PersonTable.sistEndret],
                traceparant = it[PersonTable.traceparent],
                tidspunktFraKilde = it[PersonTable.tidspunktFraKilde]
            )
        }.firstOrNull()
}

fun TransactionContext.opprettPerson(
    recordKey: String,
    tidspunktFraKilde: Instant,
    tidspunkt: Instant,
    traceId: String
): Person {
    return PersonTable.insertReturning(listOf(PersonTable.id)) {
        it[PersonTable.recordKey] = recordKey
        it[PersonTable.versjon] = consumerVersion
        it[PersonTable.sistEndret] = tidspunkt
        it[PersonTable.mergeProsessert] = false
        it[PersonTable.traceparent] = traceId
        it[PersonTable.tidspunktFraKilde] = tidspunktFraKilde
    }.let { results ->
        Person(
            personId = results.first()[PersonTable.id],
            recordKey = recordKey,
            sistEndret = tidspunkt,
            traceparant = traceId,
            tidspunktFraKilde = tidspunktFraKilde
        )
    }
}

fun TransactionContext.oppdaterPerson(personId: Long, tidspunkt: Instant, traceId: String, tidspunktFraKilde: Instant): Boolean =
    PersonTable.update(
        where = { PersonTable.id eq personId },
        body = {
            it[PersonTable.sistEndret] = tidspunkt
            it[PersonTable.tidspunktFraKilde] = tidspunktFraKilde
            it[PersonTable.mergeProsessert] = false
            it[PersonTable.traceparent] = traceId
        }
    ) > 0

fun TransactionContext.mergeProsessert(personId: Long): Boolean =
    PersonTable.update(
        where = { PersonTable.id eq personId },
        body = {
            it[PersonTable.mergeProsessert] = true
        }
    ) > 0


