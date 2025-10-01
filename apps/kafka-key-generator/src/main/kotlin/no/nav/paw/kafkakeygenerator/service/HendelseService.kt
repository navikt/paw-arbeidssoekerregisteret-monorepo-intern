package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.identitet.internehendelser.IdentitetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterMergetHendelse
import no.nav.paw.identitet.internehendelser.IdentiteterSlettetHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.config.ServerConfig
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

class HendelseService(
    serverConfig: ServerConfig,
    applicationConfig: ApplicationConfig,
    private val pawIdentitetHendelseProducer: Producer<Long, IdentitetHendelse>,
    private val pawHendelseloggHendelseProducer: Producer<Long, Hendelse>
) {
    private val logger = buildLogger
    private val env = serverConfig.runtimeEnvironment
    private val aktorTopic = applicationConfig.pdlAktorConsumer.topic
    private val identitetTopic = applicationConfig.pawIdentitetProducer.topic
    private val hendelseloggTopic = applicationConfig.pawHendelseloggProducer.topic

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

    fun sendIdentiteterHendelse(
        arbeidssoekerId: Long,
        hendelse: IdentitetHendelse
    ) {
        val record = ProducerRecord<Long, IdentitetHendelse>(
            identitetTopic,
            arbeidssoekerId,
            hendelse
        )
        val metadata = pawIdentitetHendelseProducer.sendBlocking(record)
        logger.info(
            "Sendte identitet-hendelse på topic {} (offset {}, partition {})",
            metadata.topic(),
            metadata.offset(),
            metadata.partition()
        )
    }

    fun sendIdentitetsnummerSammenslaattHendelse(
        fraArbeidssoekerId: Long,
        tilArbeidssoekerId: Long,
        identitet: String,
        identiteter: Set<String>,
        sourceTimestamp: Instant
    ) {
        val hendelse = IdentitetsnummerSammenslaatt( // På de keys som ikke lengre er i bruk
            id = fraArbeidssoekerId,
            identitetsnummer = identitet,
            flyttedeIdentitetsnumre = identiteter,
            flyttetTilArbeidssoekerId = tilArbeidssoekerId,
            hendelseId = UUID.randomUUID(),
            metadata = buildMetadata(sourceTimestamp)
        )
        sendHendelseloggHendelse(fraArbeidssoekerId, hendelse)
    }

    fun sendArbeidssoekerIdFlettetInnHendelse(
        fraArbeidssoekerId: Long,
        tilArbeidssoekerId: Long,
        identitet: String,
        identiteter: Set<String>,
        sourceTimestamp: Instant
    ) {
        val hendelse = ArbeidssoekerIdFlettetInn(
            id = tilArbeidssoekerId,
            identitetsnummer = identitet,
            kilde = Kilde(
                arbeidssoekerId = fraArbeidssoekerId,
                identitetsnummer = identiteter,
            ),
            hendelseId = UUID.randomUUID(),
            metadata = buildMetadata(sourceTimestamp)
        )
        sendHendelseloggHendelse(tilArbeidssoekerId, hendelse)
    }

    private fun sendHendelseloggHendelse(
        arbeidssoekerId: Long,
        hendelse: Hendelse
    ) {
        val recordKey = publicTopicKeyFunction(ArbeidssoekerId(arbeidssoekerId))
        val record = ProducerRecord(hendelseloggTopic, recordKey.value, hendelse)
        val metadata = pawHendelseloggHendelseProducer.sendBlocking(record)
        logger.info(
            "Sendte identitet-hendelse på topic {} (offset {}, partition {})",
            metadata.topic(),
            metadata.offset(),
            metadata.partition()
        )
    }

    private fun buildMetadata(sourceTimestamp: Instant): Metadata = Metadata(
        tidspunkt = Instant.now(),
        utfoertAv = Bruker(
            type = BrukerType.SYSTEM,
            id = env.appImageOrDefaultForLocal(),
            sikkerhetsnivaa = null
        ),
        kilde = aktorTopic,
        aarsak = "Endring i identiteter",
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = sourceTimestamp,
            avviksType = AvviksType.FORSINKELSE
        )
    )
}