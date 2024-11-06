package no.nav.paw.kafkakeymaintenance

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.kafkakeymaintenance.kafka.topic
import no.nav.paw.kafkakeymaintenance.kafka.txContext
import no.nav.paw.kafkakeymaintenance.kafka.updateHwm
import no.nav.paw.kafkakeymaintenance.perioder.insertOrUpdate
import no.nav.paw.kafkakeymaintenance.perioder.periodeRad
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PeriodeConsumer(
    applicationContext: ApplicationContext,
    private val periodeConsumer: Sequence<List<ConsumerRecord<Long, Periode>>>,
) {

    private val ctxFactory = txContext(applicationContext)

    fun run() {
        periodeConsumer.forEach { batch ->
            transaction {
                val tx = ctxFactory()
                batch.forEach { periodeRecord ->
                    val hwmValid = tx.updateHwm(
                        topic = topic(periodeRecord.topic()),
                        partition = periodeRecord.partition(),
                        offset = periodeRecord.offset(),
                        time = Instant.ofEpochMilli(periodeRecord.timestamp()),
                        lastUpdated = Instant.now()
                    )
                    if (hwmValid) {
                        val rad = periodeRad(periodeRecord.value())
                        tx.insertOrUpdate(rad)
                    }
                }
            }
        }
    }
}