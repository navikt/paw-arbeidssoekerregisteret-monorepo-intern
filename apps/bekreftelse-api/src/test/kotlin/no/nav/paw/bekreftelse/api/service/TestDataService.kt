package no.nav.paw.bekreftelse.api.service

import no.nav.paw.bekreftelse.api.models.BekreftelseRow
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import org.jetbrains.exposed.sql.transactions.transaction

class TestDataService(private val bekreftelseRepository: BekreftelseRepository) {

    fun opprettBekreftelse(bekreftelseRow: BekreftelseRow) {
        transaction {
            bekreftelseRepository.insert(bekreftelseRow)
        }
    }

    fun opprettBekreftelser(bekreftelseRows: Iterable<BekreftelseRow>) {
        transaction {
            bekreftelseRows.forEach(bekreftelseRepository::insert)
        }
    }
}