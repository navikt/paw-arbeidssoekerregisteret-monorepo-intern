package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseHendelseRecordPostgresRepository.readAllNestedRecordsForId
import no.nav.paw.arbeidssoekerregisteret.backup.database.hwm.initHwm
import no.nav.paw.arbeidssoekerregisteret.backup.kafka.processRecords
import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ApplicationHappyPathTest : FreeSpec({
    with(TestApplicationContext.build()) {
        "Verifiser enkel applikasjonsflyt" {
            initDatabase()
            initHwm(testApplicationContext)

            val topic = testApplicationContext.applicationConfig.hendelsesloggTopic
            val testRecords = testConsumerRecords()

            val mergeRecord = createMergeRecord(
                originalHendelse = testRecords.originalHendelse(),
                nyesteHendelse = testRecords.nyesteHendelse(),
            )
            testRecords.addRecord(mergeRecord)

            val testConsumerRecords = ConsumerRecords(testRecords)

            processRecords(records = testConsumerRecords, testApplicationContext)

            testConsumerRecords.forEach { record ->
                val partition = record.partition()
                val offset = record.offset()
                val forventetHendelse = record.value()

                val lagretHendelse = transaction {
                    readRecord(testApplicationContext.applicationConfig.version, partition, offset)
                }
                lagretHendelse.shouldNotBeNull()
                lagretHendelse.partition shouldBe partition
                lagretHendelse.offset shouldBe offset
                lagretHendelse.data shouldBe forventetHendelse
            }

            transaction {
                val nyesteId = testConsumerRecords.records(topic).last().value().id
                val originalId = testConsumerRecords.records(topic).first().value().id
                val hendelser = readAllNestedRecordsForId(
                    hendelseDeserializer = HendelseSerde().deserializer(),
                    arbeidssoekerId = nyesteId,
                    consumerVersion = testApplicationContext.applicationConfig.version,
                    merged = true
                )
                hendelser.map { it.arbeidssoekerId }.distinct() shouldContainExactlyInAnyOrder listOf(nyesteId, originalId)
            }
        }
    }

})

private fun testConsumerRecords(): MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>> {
    val topic = testApplicationContext.applicationConfig.hendelsesloggTopic
    val partitionCount = testApplicationContext.applicationConfig.partitionCount
    val records: MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>> =
        (0..partitionCount - 1).associate { partition ->
            val recordList = mutableListOf<ConsumerRecord<Long, Hendelse>>(
                ConsumerRecord(
                    topic,
                    partition,
                    0L,
                    partition + 100L,
                    startet((10000000000 + partition).toString(), id = 1L + partition)
                ),
                ConsumerRecord(
                    topic,
                    partition,
                    1L,
                    partition + 100L,
                    opplysninger((10000000000 + partition).toString(), id = 1L + partition)
                ),
                ConsumerRecord(
                    topic,
                    partition,
                    2L,
                    partition + 100L,
                    avsluttet((10000000000 + partition).toString(), id = 1L + partition)
                ),
            )
            TopicPartition(topic, partition) to recordList
        }.toMutableMap()

    return records
}

private fun Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>>.originalHendelse() = entries.first().value.first()
private fun Map<TopicPartition, List<ConsumerRecord<Long, Hendelse>>>.nyesteHendelse() = entries.last().value.last()

fun MutableMap<TopicPartition, MutableList<ConsumerRecord<Long, Hendelse>>>.addRecord(record: ConsumerRecord<Long, Hendelse>) {
    this[TopicPartition(record.topic(), 5)]?.add(record)
}

private fun createMergeRecord(
    originalHendelse: ConsumerRecord<Long, Hendelse>,
    nyesteHendelse: ConsumerRecord<Long, Hendelse>,
) = ConsumerRecord<Long, Hendelse>(
    testApplicationContext.applicationConfig.hendelsesloggTopic,
    nyesteHendelse.partition(),
    nyesteHendelse.offset() + 1,
    nyesteHendelse.key(),
    ArbeidssoekerIdFlettetInn(
        identitetsnummer = nyesteHendelse.value().identitetsnummer,
        id = nyesteHendelse.value().id,
        hendelseId = UUID.randomUUID(),
        metadata = nyesteHendelse.value().metadata.copy(aarsak = "Merged"),
        kilde = Kilde(
            arbeidssoekerId = originalHendelse.value().id,
            identitetsnummer = setOf(
                originalHendelse.value().identitetsnummer
            )
        )
    )
)
