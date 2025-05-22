package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.utils.TestApplicationContext
import no.nav.paw.arbeidssoekerregisteret.backup.utils.avsluttet
import no.nav.paw.arbeidssoekerregisteret.backup.utils.readRecord
import no.nav.paw.arbeidssoekerregisteret.backup.utils.startet
import no.nav.paw.arbeidssoekerregisteret.backup.utils.toApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction

class ApplicationConsumerResetTest : FreeSpec({
    "Prosesserer ConsumerRecords riktig iht. HWM ved oppstart" - {
        with(TestApplicationContext.buildWithDatabase().toApplicationContext()) {
            initHwm(this)
            val testConsumerRecords = testConsumerRecords()
            hendelseloggBackup.processRecords(
                records = testConsumerRecords,
                consumerVersion = applicationConfig.consumerVersion
            )
            testConsumerRecords.forEach { record ->
                val forventetHendelse = record.value()
                val lagretHendelse = transaction {
                    readRecord(
                        consumerVersion = applicationConfig.consumerVersion,
                        partition = record.partition(),
                        offset = record.offset()
                    )
                }
                lagretHendelse?.data shouldBe forventetHendelse
                lagretHendelse.shouldNotBeNull()
                lagretHendelse.partition shouldBe record.partition()
                lagretHendelse.offset shouldBe record.offset()
            }
        }
    }
})

private fun testConsumerRecords(): ConsumerRecords<Long, Hendelse> {
    val topic = "paw.arbeidssoker-hendelseslogg-v1"
    val records: Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>> = mapOf(
        TopicPartition(topic, 0) to listOf(
            ConsumerRecord(topic, 0, 0L, 100L, startet(identitetsnummer = "12345678901", id = 1)),
            ConsumerRecord(topic, 0, 1L, 101L, avsluttet(identitetsnummer = "12345678903", id = 3)),
            ConsumerRecord(topic, 0, 2L, 100L, avsluttet(identitetsnummer = "12345678901", id = 1)),
            ConsumerRecord(topic, 0, 3L, 100L, avsluttet(identitetsnummer = "12345678901", id = 1)),
        ),
        TopicPartition(topic, 2) to listOf(
            ConsumerRecord(topic, 2, 0L, 102L, startet(identitetsnummer = "12345678902", id = 2))
        )
    )

    return ConsumerRecords(records)
}