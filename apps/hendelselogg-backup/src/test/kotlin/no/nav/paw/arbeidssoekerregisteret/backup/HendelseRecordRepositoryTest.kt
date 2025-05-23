package no.nav.paw.arbeidssoekerregisteret.backup

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseRecordPostgresRepository.writeRecord
import no.nav.paw.arbeidssoekerregisteret.backup.utils.getOneRecordForId
import no.nav.paw.arbeidssoekerregisteret.backup.utils.readRecord
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class HendelseRecordRepositoryTest : FreeSpec({
    with(TestApplicationContext.buildWithDatabase()){
        "Verify data functions" - {
            val hendelseSerde = HendelseSerde()
            val record = ConsumerRecord<Long, Hendelse>(
                "t1",
                1,
                23,
                567,
                Startet(
                    hendelseId = UUID.randomUUID(),
                    id = 244L,
                    identitetsnummer = "12345678901",
                    metadata = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata(
                        tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        utfoertAv = Bruker(
                            type = BrukerType.SLUTTBRUKER,
                            id = "12345678901",
                            sikkerhetsnivaa = "idporten-loa-high"
                        ),
                        kilde = "unit-test",
                        aarsak = "tester",
                        tidspunktFraKilde = null
                    ),
                    opplysninger = setOf(
                        Opplysning.ER_OVER_18_AAR,
                        Opplysning.OPPHOERT_IDENTITET,
                        Opplysning.OPPHOLDSTILATELSE_UTGAATT
                    )
                )
            ).apply {
                headers().add(
                    "traceparent",
                    "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01".toByteArray()
                )
            }
            "we can write to the log without errors" {
                transaction {
                    writeRecord(1, hendelseSerde.serializer(), record)
                }
            }
            val recordVersion2 = ConsumerRecord(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                (record.value() as Startet).copy(
                    hendelseId = UUID.randomUUID(),
                )
            )
            "we can write a different version to the log without errors" {
                transaction {
                    writeRecord(2, hendelseSerde.serializer(), recordVersion2)
                }
            }
            "we can read a record based on id" {
                val storedData = transaction {
                    getOneRecordForId(id = record.value().identitetsnummer)
                }

                storedData.shouldNotBeNull()
                storedData.partition shouldBe record.partition()
                storedData.offset shouldBe record.offset()
                storedData.recordKey shouldBe record.key()
                storedData.arbeidssoekerId shouldBe record.value().id
                storedData.data shouldBe record.value()
                storedData.traceparent shouldBe record.headers().lastHeader("traceparent")
                    ?.let { h -> String(h.value()) }
            }
            "we can read multiple versions from the log" {

                val storedDataV1 = transaction {
                    readRecord(1, record.partition(), record.offset())
                }
                storedDataV1.shouldNotBeNull()
                storedDataV1.partition shouldBe record.partition()
                storedDataV1.offset shouldBe record.offset()
                storedDataV1.recordKey shouldBe record.key()
                storedDataV1.arbeidssoekerId shouldBe record.value().id
                storedDataV1.data shouldBe record.value()
                storedDataV1.traceparent shouldBe record.headers().lastHeader("traceparent")
                    ?.let { header -> String(header.value()) }

                val storedDataV2 = transaction {
                    readRecord(2, record.partition(), record.offset())
                }

                storedDataV2.shouldNotBeNull()
                storedDataV2.partition shouldBe record.partition()
                storedDataV2.offset shouldBe record.offset()
                storedDataV2.recordKey shouldBe record.key()
                storedDataV2.arbeidssoekerId shouldBe record.value().id
                storedDataV2.data shouldBe recordVersion2.value()
                storedDataV2.traceparent shouldBe null
            }
        }
    }
})
