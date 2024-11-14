package no.nav.paw.kafkakeygenerator.handler

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysAuditRepository
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.utils.buildLogger
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.Audit
import no.nav.paw.kafkakeygenerator.vo.IdentitetStatus
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class KafkaConsumerRecordHandler(
    private val database: Database,
    private val kafkaKeysRepository: KafkaKeysRepository,
    private val kafkaKeysAuditRepository: KafkaKeysAuditRepository,
) {
    private val logger = buildLogger

    fun handleRecords(sequence: Sequence<ConsumerRecords<Long, Hendelse>>) {
        sequence.forEach { records ->
            records
                .map { it.value() }
                .filterIsInstance<IdentitetsnummerSammenslaatt>()
                .forEach { hendelse ->
                    logger.info("Mottok hendelse om sammenslåing av identitetsnummer")
                    val identitetsnummer = hendelse.alleIdentitetsnummer
                        .map { Identitetsnummer(it) } + Identitetsnummer(hendelse.identitetsnummer)
                    val fraArbeidssoekerId = ArbeidssoekerId(hendelse.id)
                    val tilArbeidssoekerId = ArbeidssoekerId(hendelse.flyttetTilArbeidssoekerId)
                    updateIdentiteter(HashSet(identitetsnummer), fraArbeidssoekerId, tilArbeidssoekerId)
                }
        }
    }

    private fun updateIdentiteter(
        identitetsnummerSet: HashSet<Identitetsnummer>,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        transaction(database) {
            // TODO Dette vil stoppe Kafka Consumer og føre til unhealthy helsestatus for appen. Vil vi det?
            kafkaKeysRepository.find(fraArbeidssoekerId).let {
                if (it == null) throw IllegalStateException("ArbeidssøkerId ikke funnet")
            }
            kafkaKeysRepository.find(tilArbeidssoekerId).let {
                if (it == null) throw IllegalStateException("ArbeidssøkerId ikke funnet")
            }

            identitetsnummerSet.forEach { identitetsnummer ->
                val kafkaKey = kafkaKeysRepository.find(identitetsnummer)
                if (kafkaKey != null) {
                    updateIdentitet(identitetsnummer, fraArbeidssoekerId, tilArbeidssoekerId, kafkaKey.second)
                } else {
                    insertIdentitet(identitetsnummer, tilArbeidssoekerId)
                }
            }
        }
    }

    private fun updateIdentitet(
        identitetsnummer: Identitetsnummer,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId,
        eksisterendeArbeidssoekerId: ArbeidssoekerId
    ) {
        if (eksisterendeArbeidssoekerId == tilArbeidssoekerId) {
            val audit = Audit(identitetsnummer, IdentitetStatus.VERIFISERT, "Ingen endringer")
            kafkaKeysAuditRepository.insert(audit)
        } else {
            val count = kafkaKeysRepository.update(identitetsnummer, tilArbeidssoekerId)
            if (count != 0) {
                val audit = Audit(
                    identitetsnummer,
                    IdentitetStatus.OPPDATERT,
                    "Bytte av arbeidsøkerId fra ${fraArbeidssoekerId.value} til ${tilArbeidssoekerId.value}"
                )
                kafkaKeysAuditRepository.insert(audit)
            } else {
                logger.warn("Oppdatering førte ikke til noen endringer i databasen")
            }
        }
    }

    private fun insertIdentitet(
        identitetsnummer: Identitetsnummer,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        val count = kafkaKeysRepository.insert(identitetsnummer, tilArbeidssoekerId)
        if (count != 0) {
            val audit = Audit(
                identitetsnummer = identitetsnummer,
                identitetStatus = IdentitetStatus.OPPRETTET,
                detaljer = "Opprettet ident for arbeidsøkerId ${tilArbeidssoekerId.value}"
            )
            kafkaKeysAuditRepository.insert(audit)
        } else {
            logger.warn("Opprettelse førte ikke til noen endringer i databasen")
        }
    }
}