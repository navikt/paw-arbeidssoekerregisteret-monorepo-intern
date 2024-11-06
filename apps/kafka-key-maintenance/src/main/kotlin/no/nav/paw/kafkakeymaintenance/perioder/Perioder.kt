package no.nav.paw.kafkakeymaintenance.perioder

import no.nav.paw.kafkakeymaintenance.ApplicationContext
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import org.jetbrains.exposed.sql.transactions.transaction

interface Perioder {
    operator fun get(identitetsnummer: String): PeriodeRad?

    operator fun get(identitetsnummer: Collection<String>): List<PeriodeRad> =
        identitetsnummer.mapNotNull { get(it) }
}

fun dbPerioder(appContext: ApplicationContext): Perioder = ExpsedPerioder(appContext)

fun statiskePerioder(rader: Map<String, PeriodeRad>): Perioder = object : Perioder {
    override fun get(identitetsnummer: String): PeriodeRad? = rader[identitetsnummer]
}

private class ExpsedPerioder(appContext: ApplicationContext) : Perioder {
    private val txFactory = txContext(appContext)

    override fun get(identitetsnummer: String): PeriodeRad? =
        transaction { txFactory().periodeRad(identitetsnummer) }

    override fun get(identitetsnummer: Collection<String>): List<PeriodeRad> =
        transaction {
            val tx = txFactory()
            identitetsnummer.mapNotNull { tx.periodeRad(it) }
        }
}