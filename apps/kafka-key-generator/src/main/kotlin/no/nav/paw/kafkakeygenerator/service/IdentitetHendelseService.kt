package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentitetHendelseDeserializer
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseStatus
import no.nav.paw.kafkakeygenerator.repository.IdentitetHendelseRepository
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class IdentitetHendelseService(
    applicationConfig: ApplicationConfig,
    private val identitetHendelseRepository: IdentitetHendelseRepository,
    private val pawIdentitetProducer: Producer<Long, IdentitetHendelse>
) {
    private val logger = buildLogger
    private val serializer = IdentitetHendelseSerializer()
    private val deserializer = IdentitetHendelseDeserializer()
    private val identitetTopic = applicationConfig.pawIdentitetProducer.topic

    fun lagreIdentiteterEndretHendelse(
        aktorId: String,
        arbeidssoekerId: Long,
        identiteter: List<Identitet>,
        tidligereIdentiteter: List<Identitet>
    ) {
        val hendelse = IdentiteterEndretHendelse(
            identiteter = identiteter,
            tidligereIdentiteter = tidligereIdentiteter
        )
        val rowsAffected = identitetHendelseRepository.insert(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            data = serializer.serializeToString(hendelse),
            status = IdentitetHendelseStatus.VENTER
        )
        logger.info(
            "Lagret utgående identitet-hendelse med status {} (rows affected {})",
            IdentitetHendelseStatus.VENTER.name,
            rowsAffected
        )
    }

    fun sendVentendeIdentitetHendelser() {
        val idList = identitetHendelseRepository.updateStatusByStatusReturning(
            fraStatus = IdentitetHendelseStatus.VENTER,
            tilStatus = IdentitetHendelseStatus.PROSESSERER,
        )
        logger.info("Håndterer {} ventende identitet-hendelser", idList.size)
        idList.sorted()
            .forEach(::sendVentendeIdentitetHendelse)
    }

    private fun sendVentendeIdentitetHendelse(id: Long) {
        val identitetHendelseRow = identitetHendelseRepository.findById(id)
        if (identitetHendelseRow != null) {
            val hendelse = deserializer.deserializeFromString(identitetHendelseRow.data)
            val record = ProducerRecord<Long, IdentitetHendelse>(
                identitetTopic,
                identitetHendelseRow.arbeidssoekerId,
                hendelse
            )
            val metadata = pawIdentitetProducer.send(record).get()
            val rowsAffected = identitetHendelseRepository.updateStatusById(
                id = id,
                status = IdentitetHendelseStatus.SENDT
            )
            logger.info(
                "Sendte identitet-hendelse på topic {} (offset {}, partition {}) (rows affected {})",
                metadata.topic(),
                metadata.offset(),
                metadata.partition(),
                rowsAffected
            )
        }
    }
}