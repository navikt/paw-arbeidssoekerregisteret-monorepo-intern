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

class KafkaConsumerRecordHandler(
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
                    val identitetsnummer = Identitetsnummer(hendelse.identitetsnummer)
                    val fraArbeidssoekerId = ArbeidssoekerId(hendelse.id)
                    val tilArbeidssoekerId = ArbeidssoekerId(hendelse.flyttetTilArbeidssoekerId)
                    updateIdent(identitetsnummer, fraArbeidssoekerId, tilArbeidssoekerId)
                }
        }
    }

    private fun updateIdent(
        identitetsnummer: Identitetsnummer,
        fraArbeidssoekerId: ArbeidssoekerId,
        tilArbeidssoekerId: ArbeidssoekerId
    ) {
        val rows = kafkaKeysRepository.update(identitetsnummer, fraArbeidssoekerId, tilArbeidssoekerId)
        if (rows != 0) {
            kafkaKeysAuditRepository.insert(
                Audit(
                    identitetsnummer = identitetsnummer,
                    identitetStatus = IdentitetStatus.BYTTET_ARBEIDSSOEKER_ID,
                    detaljer = "Bytte av arbeidsøkerId fra ${fraArbeidssoekerId.value} til ${tilArbeidssoekerId.value}"
                )
            )
        } else {
            logger.warn("Endring av arbeidssøkerId førte ikke til noen oppdatering i databasen")
        }
    }
}