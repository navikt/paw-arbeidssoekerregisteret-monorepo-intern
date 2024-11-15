package no.nav.paw.kafkakeymaintenance.perioder

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

fun TransactionContext.insertOrUpdate(periode: PeriodeRad) {
    periodeRad(periode.identitetsnummer)
        .let { lagretDate ->
            if (lagretDate == null) {
                PerioderTable.insert {
                    it[version] = consumerVersion
                    it[periodeId] = periode.periodeId
                    it[identitetsnummer] = periode.identitetsnummer
                    it[fra] = periode.fra
                    it[til] = periode.til
                }
            } else {
                PerioderTable.update(
                    where = { (PerioderTable.version eq consumerVersion) and (PerioderTable.periodeId eq periode.periodeId) }
                ) {
                    it[identitetsnummer] = periode.identitetsnummer
                    it[fra] = periode.fra
                    it[til] = periode.til
                }
            }
        }
}

fun TransactionContext.periodeRad(identitetsnummer: String): PeriodeRad? =
    PerioderTable
        .selectAll()
        .where {
            (PerioderTable.version eq consumerVersion) and
                    (PerioderTable.identitetsnummer eq identitetsnummer)
        }.firstOrNull()?.let {
            PeriodeRad(
                periodeId = it[PerioderTable.periodeId],
                identitetsnummer = it[PerioderTable.identitetsnummer],
                fra = it[PerioderTable.fra],
                til = it[PerioderTable.til]
            )
        }
