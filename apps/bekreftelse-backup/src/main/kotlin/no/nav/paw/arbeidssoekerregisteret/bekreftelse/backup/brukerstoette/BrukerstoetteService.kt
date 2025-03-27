package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.brukerstoette

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.readAllRecordsForId
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database.txContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.ApplicationContext
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.vo.StoredData
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.jetbrains.exposed.sql.transactions.transaction

class BrukerstoetteService(
    applicationContext: ApplicationContext,
    private val kafkaKeysClient: KafkaKeysClient,
) {
    private val txCtx = txContext(applicationContext)
    suspend fun hentBekreftelseHendelser(identitetsnummer: String): List<StoredData>? {
        val (id, _) = kafkaKeysClient.getIdAndKey(identitetsnummer)
        val hendelser = transaction {
            txCtx().readAllRecordsForId(id)
        }
        if(hendelser.isEmpty()) {
            return null
        }
        return hendelser
    }
}