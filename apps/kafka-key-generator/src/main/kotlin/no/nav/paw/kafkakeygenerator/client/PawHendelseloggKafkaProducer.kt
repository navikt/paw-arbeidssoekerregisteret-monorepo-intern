package no.nav.paw.kafkakeygenerator.client

import no.nav.paw.arbeidssokerregisteret.intern.v1.ArbeidssoekerIdFlettetInn
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Kilde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.kafka.producer.sendBlocking
import no.nav.paw.kafkakeygenerator.config.ApplicationConfig
import no.nav.paw.kafkakeygenerator.utils.asRecordKey
import no.nav.paw.logging.logger.buildNamedLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

class PawHendelseloggKafkaProducer(
    private val runtimeEnvironment: RuntimeEnvironment,
    private val applicationConfig: ApplicationConfig,
    private val producer: Producer<Long, Hendelse>
) {
    private val logger = buildNamedLogger("kafka.producer.paw.hendelselogg")

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
        sendHendelseloggHendelse(
            arbeidssoekerId = fraArbeidssoekerId,
            hendelse = hendelse
        )
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
        sendHendelseloggHendelse(
            arbeidssoekerId = tilArbeidssoekerId,
            hendelse = hendelse
        )
    }

    private fun sendHendelseloggHendelse(
        arbeidssoekerId: Long,
        hendelse: Hendelse
    ) {
        val recordKey = arbeidssoekerId.asRecordKey()
        val record = ProducerRecord(
            applicationConfig.pawHendelseloggProducer.topic,
            recordKey,
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

    private fun buildMetadata(sourceTimestamp: Instant): Metadata = Metadata(
        tidspunkt = Instant.now(),
        utfoertAv = Bruker(
            type = BrukerType.SYSTEM,
            id = runtimeEnvironment.appNameOrDefaultForLocal(),
            sikkerhetsnivaa = null
        ),
        kilde = runtimeEnvironment.appImageOrDefaultForLocal(),
        aarsak = "Endring i identiteter",
        tidspunktFraKilde = TidspunktFraKilde(
            tidspunkt = sourceTimestamp,
            avviksType = AvviksType.FORSINKELSE
        )
    )
}