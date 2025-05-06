package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.*
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.processRecords
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

class ApplicationConsumerResetTest : FreeSpec({
    "Verifiser applikasjonsflyt ved hikke i consumer" - {

        initDbContainer()
        initHwm(testApplicationContext)
        processRecords(records = testData(), context = testApplicationContext)
        testData()
            .distinctBy { it.partition() to it.offset() }
            .forEach { record ->
                val partition = record.partition()
                val offset = record.offset()
                val forventetHendelse = record.value()
                val lagretHendelse = transaction {
                    readRecord(
                        testApplicationContext.applicationConfig.version,
                        HendelseSerde().deserializer(),
                        partition,
                        offset
                    )
                }
                lagretHendelse?.data shouldBe forventetHendelse
                lagretHendelse.shouldNotBeNull()
                lagretHendelse.partition shouldBe partition
                lagretHendelse.offset shouldBe offset
            }
    }
})

private fun testData(): ConsumerRecords<Long, Hendelse> {
    val records = listOf(
        ConsumerRecord("paw.arbeidssoker-hendelseslogg-v1", 0, 0L, 100L, startet() as Hendelse),
        ConsumerRecord("paw.arbeidssoker-hendelseslogg-v1", 0, 1L, 101L, avsluttet() as Hendelse),
        ConsumerRecord("paw.arbeidssoker-hendelseslogg-v1", 2, 0L, 102L, startet() as Hendelse),
        ConsumerRecord("paw.arbeidssoker-hendelseslogg-v1", 0, 2L, 100L, avsluttet() as Hendelse),
        ConsumerRecord("paw.arbeidssoker-hendelseslogg-v1", 0, 3L, 100L, avsluttet() as Hendelse)
    )

    val grouped: Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>> =
        records.groupBy { TopicPartition(it.topic(), it.partition()) }

    return ConsumerRecords(grouped)
}