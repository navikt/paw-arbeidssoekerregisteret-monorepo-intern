package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentitetHendelseDeserializer
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterMergetHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import no.nav.paw.kafkakeygenerator.repository.HendelseRepository
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class HendelseService(
    applicationConfig: ApplicationConfig,
    private val hendelseRepository: HendelseRepository,
    private val pawIdentitetHendelseProducer: Producer<Long, IdentitetHendelse>
) {
    private val logger = buildLogger
    private val serializer = IdentitetHendelseSerializer()
    private val deserializer = IdentitetHendelseDeserializer()
    private val version = applicationConfig.pawIdentitetProducer.version
    private val identitetTopic = applicationConfig.pawIdentitetProducer.topic
    private val batchSize = applicationConfig.identitetHendelseJob.batchSize

    fun lagreIdentiteterEndretHendelse(
        aktorId: String,
        arbeidssoekerId: Long,
        identiteter: List<Identitet>,
        tidligereIdentiteter: List<Identitet>
    ) {
        lagreIdentiteterHendelse(
            aktorId = aktorId,
            arbeidssoekerId = arbeidssoekerId,
            hendelse = IdentiteterEndretHendelse(
                identiteter = identiteter,
                tidligereIdentiteter = tidligereIdentiteter
            )
        )
    }

    fun lagreIdentiteterMergetHendelse(
        aktorId: String,
        arbeidssoekerId: Long,
        identiteter: List<Identitet>,
        tidligereIdentiteter: List<Identitet>
    ) {
        lagreIdentiteterHendelse(
            aktorId = aktorId,
            arbeidssoekerId = arbeidssoekerId,
            hendelse = IdentiteterMergetHendelse(
                identiteter = identiteter,
                tidligereIdentiteter = tidligereIdentiteter
            )
        )
    }

    fun lagreIdentiteterHendelse(
        aktorId: String,
        arbeidssoekerId: Long,
        hendelse: IdentitetHendelse
    ) {
        val rowsAffected = hendelseRepository.insert(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            version = version,
            data = serializer.serializeToString(hendelse),
            status = HendelseStatus.VENTER
        )
        logger.info(
            "Lagret utgående identitet-hendelse med status {} (rows affected {})",
            HendelseStatus.VENTER.name,
            rowsAffected
        )
    }

    fun sendVentendeIdentitetHendelser() {
        var idList: Set<Long>

        do {
            idList = hendelseRepository.updateStatusByStatusReturning(
                fraStatus = HendelseStatus.VENTER,
                tilStatus = HendelseStatus.PROSESSERER,
                limit = batchSize
            ).toSet()

            logger.info("Håndterer {} ventende identitet-hendelser", idList.size)

            idList.sorted()
                .forEach(::sendVentendeIdentitetHendelse)
        } while (idList.isNotEmpty())
    }

    private fun sendVentendeIdentitetHendelse(id: Long) {
        val identitetHendelseRow = hendelseRepository.findById(id)
        if (identitetHendelseRow != null) {
            val hendelse = deserializer.deserializeFromString(identitetHendelseRow.data)
            val record = ProducerRecord<Long, IdentitetHendelse>(
                identitetTopic,
                identitetHendelseRow.arbeidssoekerId,
                hendelse
            )
            val metadata = pawIdentitetHendelseProducer.send(record).get()
            val rowsAffected = hendelseRepository.updateById(
                id = id,
                partition = metadata.partition(),
                offset = metadata.offset(),
                status = HendelseStatus.SENDT
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