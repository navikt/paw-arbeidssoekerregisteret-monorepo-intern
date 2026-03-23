package no.nav.paw.kafkakeygenerator.client

import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterMergetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSlettetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSplittetHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class PawIdentitetKafkaProducer(
    private val applicationConfig: ApplicationConfig,
    private val producer: Producer<Long, IdentitetHendelse>
) {
    private val logger = buildNamedLogger("kafka.producer.paw.identiteter")

    fun sendIdentiteterEndretHendelse(
        arbeidssoekerId: Long,
        identiteter: List<Identitet>,
        tidligereIdentiteter: List<Identitet>
    ) {
        sendIdentiteterHendelse(
            arbeidssoekerId = arbeidssoekerId,
            hendelse = IdentiteterEndretHendelse(
                identiteter = identiteter.sortedBy { it.type.ordinal },
                tidligereIdentiteter = tidligereIdentiteter.sortedBy { it.type.ordinal }
            )
        )
    }

    fun sendIdentiteterMergetHendelse(
        arbeidssoekerId: Long,
        identiteter: List<Identitet>,
        tidligereIdentiteter: List<Identitet>
    ) {
        sendIdentiteterHendelse(
            arbeidssoekerId = arbeidssoekerId,
            hendelse = IdentiteterMergetHendelse(
                identiteter = identiteter.sortedBy { it.type.ordinal },
                tidligereIdentiteter = tidligereIdentiteter.sortedBy { it.type.ordinal }
            )
        )
    }

    fun sendIdentiteterSplittetHendelse(
        arbeidssoekerId: Long,
        identiteter: List<Identitet>,
        tidligereIdentiteter: List<Identitet>
    ) {
        sendIdentiteterHendelse(
            arbeidssoekerId = arbeidssoekerId,
            hendelse = IdentiteterSplittetHendelse(
                identiteter = identiteter.sortedBy { it.type.ordinal },
                tidligereIdentiteter = tidligereIdentiteter.sortedBy { it.type.ordinal }
            )
        )
    }

    fun sendIdentiteterSlettetHendelse(
        arbeidssoekerId: Long,
        tidligereIdentiteter: List<Identitet>
    ) {
        sendIdentiteterHendelse(
            arbeidssoekerId = arbeidssoekerId,
            hendelse = IdentiteterSlettetHendelse(
                tidligereIdentiteter = tidligereIdentiteter.sortedBy { it.type.ordinal }
            )
        )
    }

    private fun sendIdentiteterHendelse(
        arbeidssoekerId: Long,
        hendelse: IdentitetHendelse
    ) {
        val record = ProducerRecord(
            applicationConfig.pawIdentitetProducer.topic,
            arbeidssoekerId,
            hendelse
        )
        val metadata = producer.sendBlocking(record)
        logger.info(
            "Sendte identitet-hendelse på topic {} (offset {}, partition {})",
            metadata.topic(),
            metadata.offset(),
            metadata.partition()
        )
    }
}