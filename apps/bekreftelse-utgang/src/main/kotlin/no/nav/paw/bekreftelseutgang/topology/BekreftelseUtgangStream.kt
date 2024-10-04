package no.nav.paw.bekreftelseutgang.topology

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelseSerde
import no.nav.paw.bekreftelse.internehendelser.baOmAaAvsluttePeriodeHendelsesType
import no.nav.paw.bekreftelse.internehendelser.registerGracePeriodeUtloeptHendelseType
import no.nav.paw.bekreftelseutgang.config.ApplicationConfig
import no.nav.paw.bekreftelseutgang.tilstand.InternTilstand
import no.nav.paw.bekreftelseutgang.tilstand.StateStore
import no.nav.paw.bekreftelseutgang.tilstand.generateAvsluttetEventIfStateIsComplete
import no.nav.paw.config.env.appImageOrDefaultForLocal
import no.nav.paw.config.kafka.streams.mapNonNull
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import java.time.Instant
import java.util.*

fun StreamsBuilder.buildBekreftelseUtgangStream(applicationConfig: ApplicationConfig) {
    with(applicationConfig.kafkaTopology) {
        stream(bekreftelseHendelseloggTopic, Consumed.with(Serdes.Long(), BekreftelseHendelseSerde()))
            .mapNonNull<Long, BekreftelseHendelse, Hendelse>(
                name = "bekreftelseUtgangStream",
                stateStoreName,
            ) { bekreftelseHendelse ->
                val stateStore: StateStore = getStateStore(stateStoreName)
                val currentState = stateStore[bekreftelseHendelse.periodeId] ?: InternTilstand(null, null)
                val newState = currentState.copy(bekreftelseHendelse = bekreftelseHendelse)

                stateStore.put(bekreftelseHendelse.periodeId, newState)

                newState.generateAvsluttetEventIfStateIsComplete(applicationConfig)

            }.to(hendelseloggTopic, Produced.with(Serdes.Long(), HendelseSerde()))
    }
}

fun processBekreftelseHendelse(
    bekreftelseHendelse: BekreftelseHendelse,
    identitetsnummer: String,
    applicationConfig: ApplicationConfig,
): Avsluttet? {
    return when(bekreftelseHendelse.hendelseType) {
        registerGracePeriodeUtloeptHendelseType -> avsluttetHendelse(
            identitetsnummer = identitetsnummer,
            periodeId = bekreftelseHendelse.periodeId,
            arbeidssoekerId = bekreftelseHendelse.arbeidssoekerId,
            utfoertAv = Bruker(
                type = BrukerType.SYSTEM,
                id = applicationConfig.getAppImage()
            ),
            aarsak = "Graceperiode utløpt"
        )
        baOmAaAvsluttePeriodeHendelsesType -> avsluttetHendelse(
            identitetsnummer = identitetsnummer,
            periodeId = bekreftelseHendelse.periodeId,
            arbeidssoekerId = bekreftelseHendelse.arbeidssoekerId,
            utfoertAv = Bruker(
                type = BrukerType.SLUTTBRUKER,
                id = identitetsnummer
            ),
            aarsak = "Svarte NEI på spørsmål 'Vil du fortsatt være registrert som arbeidssøker?'"
        )
        else -> null
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
