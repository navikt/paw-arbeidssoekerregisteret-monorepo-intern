package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

fun TransactionContext.hentIdenter(personId: Long): List<IdentMedPersonId> {
    return IdentTable.selectAll()
        .where {
            (IdentTable.personId eq personId)
        }.map {
            identRad(
                personId = it[IdentTable.personId],
                ident = it[IdentTable.ident],
                identType = it[IdentTable.identType],
                gjeldende = it[IdentTable.gjeldende]
            )
        }
}

fun TransactionContext.opprettIdent(personId: Long, ident: String, identType: IdentType, gjeldende: Boolean) {
    IdentTable.insert {
        it[IdentTable.personId] = personId
        it[IdentTable.ident] = ident
        it[IdentTable.identType] = identType.verdi
        it[IdentTable.gjeldende] = gjeldende
    }
}

fun TransactionContext.slettIdent(personId: Long, ident: String, identType: IdentType): Boolean =
    IdentTable.deleteWhere {
        (IdentTable.personId eq personId) and (IdentTable.ident eq ident) and (IdentTable.identType eq identType.verdi)
    } > 0

fun TransactionContext.hentIdent(identer: List<Pair<String, Int>>): List<IdentMedPersonId> {
    return IdentTable.selectAll()
        .where {
            (IdentTable.ident inList identer.map { it.first }) and (IdentTable.identType inList identer.map { it.second })
        }.map {
            identRad(
                personId = it[IdentTable.personId],
                ident = it[IdentTable.ident],
                identType = it[IdentTable.identType],
                gjeldende = it[IdentTable.gjeldende]
            )
        }
}