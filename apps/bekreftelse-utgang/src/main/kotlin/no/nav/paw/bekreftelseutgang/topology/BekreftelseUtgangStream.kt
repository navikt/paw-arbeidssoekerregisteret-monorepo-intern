package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.baOmAaAvsluttePeriodeHendelsesType
import no.nav.paw.bekreftelse.internehendelser.registerGracePeriodeUtloeptHendelseType
import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.config.kafka.streams.genericProcess
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Instant
import java.util.*

fun StreamsBuilder.buildBekreftelseUtgangStream(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        stream<Long, BekreftelseHendelse>(bekreftelseHendelseloggTopic)
            .genericProcess<Long, BekreftelseHendelse, Long, Hendelse>(
                name = "bekreftelseUtgangStream",
                internStateStoreName,
                punctuation = null,
            ) { record ->
                val stateStore = getStateStore<KeyValueStore<UUID, String>>(internStateStoreName)
                // TODO: Må jeg ta høyde for at periodeStream ikke er ajoure og har lagt inn identitetsnummer i state?
                val identitetsnummer = stateStore[record.value().periodeId] ?: return@genericProcess

                when(record.value().hendelseType) {
                    registerGracePeriodeUtloeptHendelseType -> avsluttetHendelse(
                        identitetsnummer = identitetsnummer,
                        periodeId = record.value().periodeId,
                        arbeidssoekerId = record.value().arbeidssoekerId,
                        utfoertAv = Bruker(
                            type = BrukerType.SYSTEM,
                            id = applicationConfig.getAppImage()
                        ),
                        aarsak = "Graceperiode utløpt"
                    )
                    baOmAaAvsluttePeriodeHendelsesType -> avsluttetHendelse(
                        identitetsnummer = identitetsnummer,
                        periodeId = record.value().periodeId,
                        arbeidssoekerId = record.value().arbeidssoekerId,
                        utfoertAv = Bruker(
                            type = BrukerType.SLUTTBRUKER,
                            id = identitetsnummer
                        ),
                        aarsak = "Svarte NEI på spørsmål 'Vil du fortsatt være registrert som arbeidssøker?'"
                    )
                    else -> {
                        return@genericProcess
                    }
                }
            }.to(hendelseloggTopic, Produced.with(Serdes.Long(), HendelseSerde()))
    }
}

fun ApplicationConfig.getAppImage() = runtimeEnvironment.appImageOrDefaultForLocal("paw-arbeidssoekerregisteret-bekreftelse-utgang:LOCAL")

fun avsluttetHendelse(identitetsnummer: String, periodeId: UUID, arbeidssoekerId: Long, utfoertAv: Bruker, aarsak: String) = Avsluttet(
    hendelseId = UUID.randomUUID(),
    id = arbeidssoekerId,
    identitetsnummer = identitetsnummer,
    metadata = metadata(utfoertAv, aarsak),
    periodeId = periodeId,
)

fun metadata(utfoertAv: Bruker, aarsak: String) = Metadata(
    tidspunkt = Instant.now(),
    utfoertAv = utfoertAv,
    kilde = "paw.arbeidssoekerregisteret.bekreftelse-utgang",
    aarsak = aarsak,
)
